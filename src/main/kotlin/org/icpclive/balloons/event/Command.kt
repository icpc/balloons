package org.icpclive.balloons.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Command

sealed class BalloonCommand : Command {
    abstract val runId: String
}

@Serializable
@SerialName("takeBalloon")
data class TakeBalloon(
    override val runId: String,
) : BalloonCommand()

@Serializable
@SerialName("dropBalloon")
data class DropBalloon(
    override val runId: String,
) : BalloonCommand()

@Serializable
@SerialName("deliverBalloon")
data class DeliverBalloon(
    override val runId: String,
) : BalloonCommand()
