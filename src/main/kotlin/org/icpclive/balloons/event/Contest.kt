package org.icpclive.balloons.event

import kotlinx.serialization.Serializable
import org.icpclive.cds.api.ContestInfo

@Serializable
data class Contest(
    val name: String,
    val teams: List<Team>,
    val problems: List<Problem>,
) {
    constructor(contestInfo: ContestInfo) : this(
        name = contestInfo.name,
        teams = contestInfo.teams.values.map(::Team).sortedBy { it.displayName },
        problems = contestInfo.problems.map { (_, value) -> Problem(value) }.sortedBy { it.alias },
    )
}
