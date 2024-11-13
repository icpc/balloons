package org.icpclive.balloons.event

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class Balloon(
    val runId: String,
    val isFTS: Boolean,
    val teamId: String,
    val problemId: String,
    val time: Duration,
    val takenBy: String? = null,
    val delivered: Boolean = false,
)
