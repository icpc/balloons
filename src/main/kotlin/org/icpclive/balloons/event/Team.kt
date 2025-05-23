package org.icpclive.balloons.event

import kotlinx.serialization.Serializable
import org.icpclive.cds.api.TeamInfo

@Serializable
data class Team(
    val id: String,
    val displayName: String,
    val fullName: String,
    val hall: String?,
    val isHidden: Boolean = false,
) {
    constructor(teamInfo: TeamInfo) : this(
        id = teamInfo.id.value,
        displayName = teamInfo.customFields["place"] ?: teamInfo.id.value,
        fullName = teamInfo.fullName,
        hall = teamInfo.customFields["hall"],
        isHidden = teamInfo.isHidden,
    )
}
