package org.icpclive.balloons.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import kotlinx.serialization.Serializable
import org.icpclive.balloons.auth.VolunteerPrincipal
import org.icpclive.balloons.db.VolunteerRepository
import org.icpclive.cds.util.getLogger

@Serializable
data class VolunteerPatch(
    val canManage: Boolean? = null,
    val canAccess: Boolean? = null,
)

fun Route.adminController(volunteerRepository: VolunteerRepository) {
    authenticate {
        get("/api/volunteers") {
            val principal: VolunteerPrincipal? = call.principal()
            if (principal?.volunteer?.canManage != true) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }

            val volunteers = volunteerRepository.all().map(::VolunteerView)

            call.respond(volunteers)
        }

        patch("/api/volunteers/{id}") {
            val principal: VolunteerPrincipal? = call.principal()
            if (principal?.volunteer?.canManage != true) {
                call.respond(HttpStatusCode.Forbidden)
                return@patch
            }

            val volunteerId =
                call.parameters["id"]?.toLongOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest)

            val volunteer =
                volunteerRepository.getById(volunteerId)
                    ?: return@patch call.respond(HttpStatusCode.NotFound)

            val patch = call.receive<VolunteerPatch>()
            logger.info { "Manager ${principal.volunteer.login} changes ${volunteer.login} rights to $patch" }

            if (patch.canAccess == null && patch.canManage == null) {
                call.respond(HttpStatusCode.BadRequest, "At least canAccess or canManage should be set")
                return@patch
            }

            volunteerRepository.updateFlags(volunteerId, patch.canAccess, patch.canManage)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private val logger by getLogger()
