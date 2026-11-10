package org.icpclive.balloons.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.icpclive.balloons.FakeCds
import org.icpclive.balloons.TestDatabase
import org.icpclive.balloons.inbox
import org.icpclive.balloons.next
import org.icpclive.cds.api.Verdict
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class EventStreamTest {
    @TempDir
    lateinit var directory: Path

    /** One running event stream; create a second one over the same [db] to emulate a restart. */
    private class Fixture(
        val db: TestDatabase,
        val scope: CoroutineScope,
    ) {
        val cds = FakeCds()
        val stream = EventStream(db.module.balloonRepository, cds.flow).also { scope.launch { it.consumeCds() } }

        fun inbox() = stream.inbox(scope)

        /** State a client connecting right now would get. */
        suspend fun snapshot(): State {
            val messages = Channel<ClientMessage>(Channel.UNLIMITED)
            val subscription = stream.subscribe(scope) { messages.send(it) }
            try {
                return messages.next().state!!
            } finally {
                subscription.cancel()
            }
        }

        /**
         * Waits until the balloons are exactly [expected] (run id to first-to-solve flag).
         * The order of events during a rejudge is up to the contest data library, so only the outcome is checked.
         */
        suspend fun awaitBalloons(vararg expected: Pair<String, Boolean>) {
            suspend fun balloons() = snapshot().balloons.mapValues { it.value.isFTS }
            withTimeoutOrNull(10.seconds) {
                while (balloons() != expected.toMap()) {
                    delay(20)
                }
            }
            assertEquals(expected.toMap(), balloons())
        }

        /** Starts the contest and returns an inbox that has already consumed the initial state. */
        suspend fun startedInbox() =
            inbox().also {
                cds.contest()
                it.next()
            }
    }

    private fun streamTest(block: suspend Fixture.() -> Unit) =
        runBlocking {
            val scope = CoroutineScope(Dispatchers.Default)
            try {
                Fixture(TestDatabase(directory), scope).block()
            } finally {
                scope.cancel()
            }
        }

    private val ClientMessage.balloon get() = (event as BalloonUpdated).balloon

    @Test
    fun `subscriber first receives the state with the contest`() =
        streamTest {
            val inbox = inbox()
            cds.contest("Finals")

            val message = inbox.next()
            assertNull(message.event)
            val contest = assertNotNull(message.state).contest
            assertEquals("Finals", contest.name)
            assertEquals(emptyMap(), message.state.balloons)

            // Hidden team is dropped; teams are named by their place and sorted by it, falling back to the id.
            assertEquals(
                listOf(Team("t1", "B2", "Team t1", "North"), Team("t2", "t2", "Team t2", null)),
                contest.teams,
            )
            assertEquals(listOf("A", "B"), contest.problems.map { it.alias })
            assertEquals(listOf("Problem A", "Problem B"), contest.problems.map { it.name })
            assertNotNull(contest.problems[0].color)
            assertNull(contest.problems[1].color)
        }

    @Test
    fun `contest change is sent as an event`() =
        streamTest {
            val inbox = startedInbox()
            cds.contest("Renamed")

            assertEquals("Renamed", (inbox.next().event as ContestUpdated).contest.name)
        }

    @Test
    fun `accepted run becomes a balloon`() =
        streamTest {
            val inbox = startedInbox()
            cds.run("r1", team = "t1", problem = "A")

            assertEquals(
                BalloonUpdated(Balloon("r1", isFTS = true, teamId = "t1", problemId = "A", time = 1.minutes)),
                inbox.next().event,
            )
        }

    @Test
    fun `only the first solution of a problem is marked first to solve`() =
        streamTest {
            val inbox = startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            cds.run("r2", team = "t2", problem = "A")
            cds.run("r3", team = "t2", problem = "B")

            assertEquals(
                listOf("r1" to true, "r2" to false, "r3" to true),
                List(3) { inbox.next().balloon }.map { it.runId to it.isFTS },
            )
        }

    @Test
    fun `rejected, hidden and repeated accepted runs give no balloons`() =
        streamTest {
            val inbox = startedInbox()
            cds.run("rejected", team = "t1", problem = "A", verdict = Verdict.WrongAnswer)
            cds.run("hidden", team = "t1", problem = "B", isHidden = true)
            cds.run("hidden-team", team = "t3", problem = "B")
            cds.run("first", team = "t2", problem = "A")
            cds.run("again", team = "t2", problem = "A")
            cds.run("last", team = "t2", problem = "B")

            // Runs are processed in order, so anything published for the skipped ones would show up before "last".
            assertEquals("first", inbox.next().balloon.runId)
            assertEquals("last", inbox.next().balloon.runId)
        }

    @Test
    fun `balloon is recalled when its run is rejudged to rejected`() =
        streamTest {
            val inbox = startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            assertEquals("r1", inbox.next().balloon.runId)

            cds.run("r1", team = "t1", problem = "A", verdict = Verdict.WrongAnswer)
            assertEquals(BalloonDeleted("r1"), inbox.next().event)

            cds.run("r2", team = "t2", problem = "B")
            inbox.next()
            assertEquals(setOf("r2"), inbox().next().state?.balloons?.keys)
        }

    @Test
    fun `balloon moves to an older run rejudged to accepted and keeps its delivery`() =
        streamTest {
            val alice = db.addVolunteer("alice")
            val inbox = startedInbox()
            cds.run("older", team = "t1", problem = "A", verdict = Verdict.WrongAnswer)
            cds.run("newer", team = "t1", problem = "A")
            assertEquals("newer", inbox.next().balloon.runId)
            assertTrue(stream.processCommand(TakeBalloon("newer"), alice))

            cds.run("older", team = "t1", problem = "A")

            awaitBalloons("older" to true)
            snapshot().balloons.getValue("older").let {
                assertEquals(1.minutes, it.time)
                assertEquals("alice", it.takenBy)
            }
            assertTrue(stream.processCommand(DeliverBalloon("older"), alice))
            assertFalse(stream.processCommand(DeliverBalloon("newer"), alice))
        }

    @Test
    fun `first to solve passes to the next team when the first solution is rejudged to rejected`() =
        streamTest {
            startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            cds.run("r2", team = "t2", problem = "A")
            awaitBalloons("r1" to true, "r2" to false)

            cds.run("r1", team = "t1", problem = "A", verdict = Verdict.WrongAnswer)

            awaitBalloons("r2" to true)
        }

    @Test
    fun `first to solve passes to the next team when the first team becomes hidden`() =
        streamTest {
            startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            cds.run("r2", team = "t2", problem = "A")
            awaitBalloons("r1" to true, "r2" to false)

            cds.contest(hiddenTeams = setOf("t1", "t3"))

            awaitBalloons("r2" to true)
            assertEquals(listOf("t2"), snapshot().contest.teams.map { it.id })
        }

    @Test
    fun `first to solve passes to an older run rejudged to accepted`() =
        streamTest {
            startedInbox()
            cds.run("older", team = "t1", problem = "A", verdict = Verdict.WrongAnswer)
            cds.run("newer", team = "t2", problem = "A")
            awaitBalloons("newer" to true)

            cds.run("older", team = "t1", problem = "A")

            awaitBalloons("older" to true, "newer" to false)
        }

    @Test
    fun `late subscriber receives current state and then only new events`() =
        streamTest {
            val first = startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            first.next()

            val late = inbox()
            val state = assertNotNull(late.next().state)
            assertEquals("Test contest", state.contest.name)
            assertEquals(setOf("r1"), state.balloons.keys)

            cds.run("r2", team = "t2", problem = "B")
            assertEquals("r2", late.next().balloon.runId)
            assertEquals("r2", first.next().balloon.runId)
        }

    @Test
    fun `balloon is taken, dropped, taken by another volunteer and delivered`() =
        streamTest {
            val alice = db.addVolunteer("alice")
            val bob = db.addVolunteer("bob")
            val inbox = startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            inbox.next()

            assertTrue(stream.processCommand(TakeBalloon("r1"), alice))
            inbox.next().balloon.let {
                assertEquals("alice", it.takenBy)
                assertFalse(it.delivered)
            }

            assertTrue(stream.processCommand(DropBalloon("r1"), alice))
            assertNull(inbox.next().balloon.takenBy)

            assertTrue(stream.processCommand(TakeBalloon("r1"), bob))
            assertEquals("bob", inbox.next().balloon.takenBy)

            assertTrue(stream.processCommand(DeliverBalloon("r1"), bob))
            inbox.next().balloon.let {
                assertEquals("bob", it.takenBy)
                assertTrue(it.delivered)
            }
        }

    @Test
    fun `volunteers can not interfere with a balloon taken by someone else`() =
        streamTest {
            val alice = db.addVolunteer("alice")
            val bob = db.addVolunteer("bob")
            val inbox = startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            inbox.next()
            assertTrue(stream.processCommand(TakeBalloon("r1"), alice))
            inbox.next()

            assertFalse(stream.processCommand(TakeBalloon("r1"), bob))
            assertFalse(stream.processCommand(DropBalloon("r1"), bob))
            assertFalse(stream.processCommand(DeliverBalloon("r1"), bob))

            // Nothing was published for the failed commands: the next event is alice's delivery.
            assertTrue(stream.processCommand(DeliverBalloon("r1"), alice))
            inbox.next().balloon.let {
                assertEquals("alice", it.takenBy)
                assertTrue(it.delivered)
            }
        }

    @Test
    fun `balloon can not be delivered before it is taken or dropped after delivery`() =
        streamTest {
            val alice = db.addVolunteer("alice")
            val inbox = startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            inbox.next()

            assertFalse(stream.processCommand(DeliverBalloon("r1"), alice))
            assertFalse(stream.processCommand(DropBalloon("r1"), alice))

            assertTrue(stream.processCommand(TakeBalloon("r1"), alice))
            assertTrue(stream.processCommand(DeliverBalloon("r1"), alice))
            assertFalse(stream.processCommand(DropBalloon("r1"), alice))

            assertEquals("alice", inbox.next().balloon.takenBy)
            assertTrue(inbox.next().balloon.delivered)
        }

    @Test
    fun `repeating a command succeeds without a new event`() =
        streamTest {
            val alice = db.addVolunteer("alice")
            val inbox = startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            inbox.next()

            assertTrue(stream.processCommand(TakeBalloon("r1"), alice))
            assertTrue(stream.processCommand(TakeBalloon("r1"), alice))
            assertTrue(stream.processCommand(DeliverBalloon("r1"), alice))
            assertTrue(stream.processCommand(DeliverBalloon("r1"), alice))

            assertFalse(inbox.next().balloon.delivered)
            assertTrue(inbox.next().balloon.delivered)
            cds.run("r2", team = "t2", problem = "B")
            assertEquals("r2", inbox.next().balloon.runId)
        }

    @Test
    fun `command for an unknown run fails`() =
        streamTest {
            val alice = db.addVolunteer("alice")
            startedInbox()

            assertFalse(stream.processCommand(TakeBalloon("nope"), alice))
        }

    @Test
    fun `recalled balloon can not be taken`() =
        streamTest {
            val alice = db.addVolunteer("alice")
            val inbox = startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            inbox.next()
            cds.run("r1", team = "t1", problem = "A", verdict = Verdict.WrongAnswer)
            inbox.next()

            assertFalse(stream.processCommand(TakeBalloon("r1"), alice))
        }

    @Test
    fun `deliveries survive a restart`() =
        streamTest {
            val alice = db.addVolunteer("alice")
            val inbox = startedInbox()
            cds.run("r1", team = "t1", problem = "A")
            cds.run("r2", team = "t2", problem = "B")
            inbox.next()
            inbox.next()
            assertTrue(stream.processCommand(TakeBalloon("r1"), alice))
            assertTrue(stream.processCommand(DeliverBalloon("r1"), alice))
            assertTrue(stream.processCommand(TakeBalloon("r2"), alice))

            // New stream over the same database, the contest system replays its runs from the beginning.
            val restarted = Fixture(db, scope)
            val newInbox = restarted.startedInbox()
            restarted.cds.run("r1", team = "t1", problem = "A")
            restarted.cds.run("r2", team = "t2", problem = "B")

            newInbox.next().balloon.let {
                assertEquals("r1", it.runId)
                assertEquals("alice", it.takenBy)
                assertTrue(it.delivered)
            }
            newInbox.next().balloon.let {
                assertEquals("r2", it.runId)
                assertEquals("alice", it.takenBy)
                assertFalse(it.delivered)
            }
        }
}
