package org.icpclive.balloons.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.balloons.db.BalloonRepository
import org.icpclive.balloons.db.tables.references.BALLOON
import org.icpclive.balloons.db.tables.references.VOLUNTEER
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.RunInfo
import org.icpclive.cds.api.RunResult
import org.icpclive.cds.cli.CdsCommandLineOptions
import org.icpclive.cds.util.getLogger
import kotlin.getValue

class EventStream(
    private val balloonRepository: BalloonRepository,
    settings: CdsCommandLineOptions,
) {
    // We maintain current [state] of the contest, and [eventFlow] with updates to that state.
    // When new subscriber arrives, they receive state at the time of connection, and all subsequent updates.
    // To avoid locking by slow clients, we extend buffer capacity of the flow. It may eat RAM.
    //
    // You should never emit manually to [eventFlow] or update [state]. Always use [update] method that
    // prevents concurrent modifications.
    private var state: State = State(Contest("", listOf(), listOf()), mapOf())
    private val eventFlow = MutableSharedFlow<Pair<State, Event>>(replay = 1, extraBufferCapacity = Int.MAX_VALUE)

    private val mutex = Mutex()

    private val cds = settings.toFlow().addComputedData()

    suspend fun consumeCds() {
        cds.buffer(capacity = UNLIMITED).collect { event ->
            when (event) {
                is RunUpdate -> processRun(event.newInfo)
                is InfoUpdate -> processContestInfo(event.newInfo)
                else -> {}
            }
        }
    }

    fun subscribe(
        scope: CoroutineScope,
        process: suspend (ClientMessage) -> Unit,
    ): Job =
        scope.launch {
            var hasState = false
            eventFlow.collect { (state, event) ->
                if (!hasState) {
                    process(ClientMessage(state = state))
                    hasState = true
                } else {
                    process(ClientMessage(event = event))
                }
            }
        }

    /**
     * @return `true` if command succeeded, `false` otherwise (in case of concurrent modification, etc.)
     */
    suspend fun processCommand(
        command: Command,
        volunteerId: Long,
    ): Boolean {
        if (command !is BalloonCommand) {
            throw NotImplementedError("${command::class.qualifiedName} command is not supported yet")
        }

        val balloon = state.balloons[command.runId] ?: return false

        val commandResult =
            when (command) {
                is TakeBalloon -> balloonRepository.reserveBalloon(balloon, volunteerId)
                is DropBalloon -> balloonRepository.dropBalloon(balloon, volunteerId)
                is DeliverBalloon -> balloonRepository.deliverBalloon(balloon, volunteerId)
            }

        if (!commandResult) {
            return false
        }

        val newBalloon = balloon.withDelivery()

        if (balloon != newBalloon) {
            update(BalloonUpdated(newBalloon))
        }

        return true
    }

    private suspend fun processContestInfo(contestInfo: ContestInfo) {
        val newContest = Contest(contestInfo)

        if (newContest != state.contest) {
            update(ContestUpdated(newContest))
        }
    }

    private suspend fun processRun(runInfo: RunInfo) {
        val runId = runInfo.id.value
        val isBalloon = !runInfo.isHidden && runInfo.result.isSolved() && !(runInfo.result as RunResult.ICPC).isAfterFirstOk

        val existingBalloon = state.balloons[runId]

        if (isBalloon) {
            val balloon =
                Balloon(
                    runId = runId,
                    isFTS = (runInfo.result as RunResult.ICPC).isFirstToSolveRun,
                    teamId = runInfo.teamId.value,
                    problemId = runInfo.problemId.value,
                    time = runInfo.time,
                ).withDelivery()

            if (existingBalloon != balloon) {
                // New or updated run, push it.
                if (existingBalloon != null) {
                    logger.info { "Balloon for submission $runId is updated: was ${existingBalloon.isFTS}, now ${balloon.isFTS}" }
                }

                update(BalloonUpdated(balloon))
            }
        } else if (existingBalloon != null) {
            // Extremely rare: possibly rejudge from OK to WA. We're removing a balloon.
            logger.info { "Balloon for submission $runId is recalled" }
            update(BalloonDeleted(runId))
        }
    }

    private suspend fun update(event: Event) =
        mutex.withLock {
            state = event.applyTo(state)
            eventFlow.emit(state to event)
        }

    private fun RunResult.isSolved(): Boolean =
        when (this) {
            is RunResult.ICPC -> verdict.isAccepted
            is RunResult.IOI -> false
            is RunResult.InProgress -> false
        }

    private suspend fun Balloon.withDelivery(): Balloon {
        val delivery = balloonRepository.getDelivery(this)
        return this.copy(
            takenBy = delivery?.get(VOLUNTEER.LOGIN),
            delivered = delivery?.get(BALLOON.DELIVERED) ?: false,
        )
    }

    companion object {
        private val logger by getLogger()
    }
}

data class ClientMessage(
    val state: State? = null,
    val event: Event? = null,
) {
    init {
        if (state == null && event == null) {
            throw IllegalArgumentException("Either state or event must be non-null")
        }
        if (state != null && event != null) {
            throw IllegalArgumentException("Only one of state or event must be non-null")
        }
    }
}

fun launchCDS(eventStream: EventStream) {
    CoroutineScope(Job()).launch {
        eventStream.consumeCds()
    }
}
