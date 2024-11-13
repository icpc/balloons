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

class EventStream(private val balloonRepository: BalloonRepository) {
    private val runs = MutableStateFlow(listOf<Run>())

    private val sink = MutableStateFlow<Pair<State, Event>>(State(Contest("Loading", listOf(), listOf()), listOf()) to Reload)
    val stream = sink.asStateFlow()

    /**
     * @return `true` if command succeeded, `false` otherwise (in case of concurrent modification, etc.)
     */
    fun processCommand(
        command: Command,
        volunteerId: Long,
    ): Boolean =
        when (command) {
            is BalloonCommand -> processBalloonCommand(command, volunteerId)
        }

    private fun processBalloonCommand(
        command: BalloonCommand,
        volunteerId: Long,
    ): Boolean {
        val balloon =
            getState().balloons.find { it.runId == command.runId }
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
    fun processRun(runInfo: RunInfo) {
        val runId = runInfo.id.value

        val existingRun = runs.value.find { it.runId == runId }
        val run = Run(runInfo)

        if (runInfo.result.isSolved()) {
            if (existingRun == run) {
                return
            }

            runs.update { runs ->
                runs.filter { it.runId != runId }.plus(Run(runInfo)).sorted()
            }
        } else {
            if (existingRun == null) {
                return
            }

            // Extremely rare case: the run was OK and then rejudged. In that case, we should remove this run from our list.
            logger.warning { "Removing existing OK for ${Run(runInfo)}" }

            runs.update { runs ->
                runs.filter { it.runId != runId }
            }
        }

        synchronizeProblemState(run.problemId, run.teamId)
    }

    /**
     * Recalculates current state by [runs] and commits it to [sink].
     */
    private fun synchronizeProblemState(
        problemId: String,
        teamId: String,
    ) {
        val existingBalloon = getState().balloons.find { it.problemId == problemId && it.teamId == teamId }
        val existingFTS = getState().balloons.find { it.problemId == problemId && it.isFTS }

        val actualRun = runs.value.find { it.problemId == problemId && it.teamId == teamId }
        val actualFTS = runs.value.find { it.problemId == problemId }

        if (existingBalloon != null && existingBalloon.runId != actualRun?.runId) {
            // Existing balloon does not correspond to OK run for this team, so remove it.
            updateSink(BalloonDeleted(existingBalloon.runId))
        }

        if (actualRun != null) {
            val targetBalloon = actualRun.toBalloon(isFTS = actualRun.runId == actualFTS?.runId).withDelivery()

            if (existingBalloon != targetBalloon) {
                updateSink(BalloonUpdated(targetBalloon))
            }
        }

        if (existingFTS?.runId != actualFTS?.runId) {
            if (existingFTS != null) {
                // Drop FTS for old run
                val balloon = getState().balloons.find { it.runId == existingFTS.runId }
                if (balloon != null && balloon.isFTS) {
                    updateSink(BalloonUpdated(balloon.copy(isFTS = false)))
                }
            }

            if (actualFTS != null) {
                // Add FTS for new run
                val balloon =
                    getState().balloons.find { it.runId == actualFTS.runId }
                        ?: throw IllegalStateException("No balloon for FTS run $actualFTS found")

                if (!balloon.isFTS) {
                    updateSink(BalloonUpdated(balloon.copy(isFTS = true)))
                }
            }
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

    private fun Run.toBalloon(isFTS: Boolean) =
        Balloon(runId, isFTS, teamId, problemId, time)
            .withDelivery()

    private fun Balloon.withDelivery(): Balloon {
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
