package org.icpclive.balloons.event

import kotlinx.serialization.Serializable
import org.icpclive.cds.util.serializers.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
data class Balloon(
    val runId: String,
    val isFTS: Boolean,
    val teamId: String,
    val problemId: String,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val time: Duration,
    val takenBy: String? = null,
    val delivered: Boolean = false,
)
