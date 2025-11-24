package org.icpclive.balloons.event

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.icpclive.balloons.db.BalloonRepository
import org.icpclive.balloons.db.tables.references.BALLOON
import org.icpclive.balloons.db.tables.references.VOLUNTEER
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.RunInfo
import org.icpclive.cds.api.RunResult
import org.icpclive.cds.util.getLogger

class EventStream(
    private val balloonRepository: BalloonRepository,
) {
    private val sink = MutableStateFlow<Pair<State, Event>>(State(Contest("Loading", listOf(), listOf()), mapOf()) to Reload)
    val stream = sink.asStateFlow()

    /**
     * @return `true` if command succeeded, `false` otherwise (in case of concurrent modification, etc.)
     */
    suspend fun processCommand(
        command: Command,
        volunteerId: Long,
    ): Boolean =
        when (command) {
            is BalloonCommand -> processBalloonCommand(command, volunteerId)
        }

    private suspend fun processBalloonCommand(
        command: BalloonCommand,
        volunteerId: Long,
    ): Boolean {
        val balloon =
            getState().balloons[command.runId]
                ?: return false

        when (command) {
            is TakeBalloon -> {
                if (!balloonRepository.reserveBalloon(balloon, volunteerId)) {
                    return false
                }
            }

            is DropBalloon -> {
                if (!balloonRepository.dropBalloon(balloon, volunteerId)) {
                    return false
                }
            }

            is DeliverBalloon -> {
                if (!balloonRepository.deliverBalloon(balloon, volunteerId)) {
                    return false
                }
            }
        }

        val newBalloon = balloon.withDelivery()

        if (balloon != newBalloon) {
            updateSink(BalloonUpdated(newBalloon))
        }

        return true
    }

    // This can be written in non-concurrent fashion.
    fun processContestInfo(contestInfo: ContestInfo) {
        val newContest = Contest(contestInfo)

        if (newContest != getState().contest) {
            updateSink(ContestUpdated(newContest))
        }
    }

    // This can be written in non-concurrent fashion.
    suspend fun processRun(runInfo: RunInfo) {
        val runId = runInfo.id.value
        val isBalloon = !runInfo.isHidden && runInfo.result.isSolved() && !(runInfo.result as RunResult.ICPC).isAfterFirstOk

        val existingBalloon = getState().balloons[runId]

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

                updateSink(BalloonUpdated(balloon))
            }
        } else if (existingBalloon != null) {
            // Extremely rare: possibly rejudge from OK to WA. We're removing a balloon.
            logger.info { "Balloon for submission $runId is recalled" }
            updateSink(BalloonDeleted(runId))
        }
    }

    private fun getState() = sink.value.first

    private fun updateSink(event: Event) = sink.update { (state, _) -> state with event }

    /**
     * Use this as the last operator in [MutableStateFlow.update].
     *
     * It should be THE ONLY way to update the state. Each event should be applied in its own `update`.
     * The exception is [Reload] event that sends new state to connected clients. In that case, make
     * all updates inside [MutableStateFlow.update] and finish with `state with Reload`.
     */
    private infix fun State.with(event: Event) = event.applyTo(this) to event

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
