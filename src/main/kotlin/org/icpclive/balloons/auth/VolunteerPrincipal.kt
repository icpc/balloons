package org.icpclive.balloons.auth

import com.auth0.jwt.interfaces.Payload
import io.ktor.server.auth.jwt.JWTPayloadHolder
import org.icpclive.balloons.db.tables.records.VolunteerRecord

class VolunteerPrincipal(
    val volunteer: VolunteerRecord,
    payload: Payload,
) : JWTPayloadHolder(payload)
