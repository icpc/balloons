package org.icpclive.balloons

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.icpclive.balloons.admin.Role
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class WebSocketTest {
    @TempDir
    lateinit var directory: Path

    private suspend fun DefaultClientWebSocketSession.nextMessage(): JsonObject = (incoming.next() as Frame.Text).readText().toJson()

    private suspend fun DefaultClientWebSocketSession.takeBalloon(runId: String) = send("""{"type": "takeBalloon", "runId": "$runId"}""")

    /** Authenticates and consumes the initial state. */
    private suspend fun DefaultClientWebSocketSession.authenticate(token: String): JsonObject {
        send(token)
        return nextMessage()
    }

    private val JsonObject.balloon get() = getValue("balloon").jsonObject

    @Test
    fun `invalid token is denied`() =
        apiTest(directory) {
            client.webSocket("/api/balloons") {
                send("garbage")

                assertEquals("access denied", nextMessage()["error"].text)
            }
        }

    @Test
    fun `pending user is denied`() =
        apiTest(directory) {
            val token = db.tokenFor("newbie", Role.PENDING)
            client.webSocket("/api/balloons") {
                send(token)

                assertEquals("access denied", nextMessage()["error"].text)
            }
        }

    @Test
    fun `volunteer receives the state and then balloon events`() =
        apiTest(directory) {
            val token = db.tokenFor("alice")
            cds.contest("Finals")
            client.webSocket("/api/balloons") {
                val state = authenticate(token)
                assertEquals("Finals", state.getValue("contest").jsonObject["name"].text)
                assertEquals(JsonObject(emptyMap()), state["balloons"])

                cds.run("r1", team = "t1", problem = "A")
                val event = nextMessage()
                assertEquals("balloonUpdated", event["type"].text)
                assertEquals(
                    listOf("r1", "t1", "A", "true", "60000", null, "false"),
                    listOf("runId", "teamId", "problemId", "isFTS", "time", "takenBy", "delivered").map { event.balloon[it].text },
                )
            }
        }

    @Test
    fun `reconnected volunteer sees existing balloons in the state`() =
        apiTest(directory) {
            val token = db.tokenFor("alice")
            cds.contest()
            cds.run("r1", team = "t1", problem = "A")
            client.webSocket("/api/balloons") {
                send(token)
                // The state may arrive before the run is processed, in that case the run follows as an event.
                val first = nextMessage()
                val balloons = first["balloons"]?.jsonObject
                if (balloons.isNullOrEmpty()) {
                    assertEquals("r1", nextMessage().balloon["runId"].text)
                } else {
                    assertEquals(setOf("r1"), balloons.keys)
                }
            }
            client.webSocket("/api/balloons") {
                assertEquals(setOf("r1"), authenticate(token).getValue("balloons").jsonObject.keys)
            }
        }

    @Test
    fun `commands are broadcast to everyone and conflicts are reported to the sender`() =
        apiTest(directory) {
            val alice = db.tokenFor("alice")
            val bob = db.tokenFor("bob")
            cds.contest()
            client.webSocket("/api/balloons") {
                val aliceSocket = this
                aliceSocket.authenticate(alice)
                client.webSocket("/api/balloons") {
                    val bobSocket = this
                    bobSocket.authenticate(bob)
                    cds.run("r1", team = "t1", problem = "A")
                    aliceSocket.nextMessage()
                    bobSocket.nextMessage()

                    aliceSocket.takeBalloon("r1")
                    assertEquals("alice", aliceSocket.nextMessage().balloon["takenBy"].text)
                    assertEquals("alice", bobSocket.nextMessage().balloon["takenBy"].text)

                    bobSocket.takeBalloon("r1")
                    assertEquals("command failed", bobSocket.nextMessage()["error"].text)

                    aliceSocket.send("""{"type": "deliverBalloon", "runId": "r1"}""")
                    assertEquals("true", bobSocket.nextMessage().balloon["delivered"].text)
                    // Alice got only the delivery: bob's failure was not sent to her.
                    assertEquals("true", aliceSocket.nextMessage().balloon["delivered"].text)
                }
            }
        }

    @Test
    fun `dropped balloon becomes free again`() =
        apiTest(directory) {
            val token = db.tokenFor("alice")
            cds.contest()
            client.webSocket("/api/balloons") {
                authenticate(token)
                cds.run("r1", team = "t1", problem = "A")
                nextMessage()

                takeBalloon("r1")
                assertEquals("alice", nextMessage().balloon["takenBy"].text)
                send("""{"type": "dropBalloon", "runId": "r1"}""")
                assertNull(nextMessage().balloon["takenBy"].text)
            }
        }

    @Test
    fun `command for an unknown run fails`() =
        apiTest(directory) {
            val token = db.tokenFor("alice")
            cds.contest()
            client.webSocket("/api/balloons") {
                authenticate(token)

                takeBalloon("nope")
                assertEquals("command failed", nextMessage()["error"].text)
            }
        }

    @Test
    fun `malformed command closes the connection`() =
        apiTest(directory) {
            val token = db.tokenFor("alice")
            cds.contest()
            client.webSocket("/api/balloons") {
                authenticate(token)

                send("""{"type": "stealBalloon", "runId": "r1"}""")
                val next = withContext(Dispatchers.Default) { withTimeout(10.seconds) { incoming.receiveCatching() } }
                assertNull(next.getOrNull())
            }
        }
}
