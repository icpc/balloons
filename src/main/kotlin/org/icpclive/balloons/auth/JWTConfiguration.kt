package org.icpclive.balloons.auth

import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import org.icpclive.balloons.db.VolunteerRepository

class CredentialValidator(private val volunteerRepository: VolunteerRepository) {
    fun validate(credential: JWTCredential): VolunteerPrincipal? =
        credential.payload.let {
            val volunteerId = it.subject?.toLongOrNull() ?: return@let null
            val volunteer = volunteerRepository.getById(volunteerId) ?: return@let null

            VolunteerPrincipal(volunteer, it)
        }
}

fun Application.installJwt(
    jwtVerifier: JWTVerifier,
    credentialValidator: CredentialValidator,
) {
    install(Authentication) {
        jwt {
            realm = "balloons"
            verifier(jwtVerifier)
            validate { credentialValidator.validate(it) }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}
