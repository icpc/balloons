package org.icpclive.balloons.admin

import kotlinx.serialization.Serializable
import org.icpclive.balloons.db.tables.records.VolunteerRecord

enum class Role(
    val canAccess: Boolean,
    val canManage: Boolean,
) {
    PENDING(false, false),
    VOLUNTEER(true, false),
    MANAGER(true, true),
}

@Serializable
data class VolunteerView(
    val id: Long,
    val login: String,
    val role: Role,
) {
    constructor(record: VolunteerRecord) : this(
        record.id!!,
        record.login,
        when {
            record.canManage == true -> Role.MANAGER
            record.canAccess == true -> Role.VOLUNTEER
            else -> Role.PENDING
        },
    )
}
