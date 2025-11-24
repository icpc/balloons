package org.icpclive.balloons.event

import kotlinx.serialization.Serializable

@Serializable
data class State(
    val contest: Contest,
    val balloons: Map<String, Balloon>,
)
