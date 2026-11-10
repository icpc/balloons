package org.icpclive.balloons.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.icpclive.balloons.auth.VolunteerPrincipal
import org.icpclive.balloons.db.VolunteerRepository
import org.icpclive.balloons.db.tables.records.VolunteerRecord
import org.icpclive.cds.util.getLogger

private const val MIN_PASSWORD_LENGTH = 6

@Serializable
data class VolunteerPatch(
    val role: Role? = null,
    val password: String? = null,
)

@Serializable
data class NewVolunteer(
    val login: String,
    val password: String,
    val role: Role = Role.VOLUNTEER,
)

/**
 * @return current volunteer if they are a manager, otherwise responds with 403 and returns `null`
 */
private suspend fun RoutingContext.requireManager(): VolunteerRecord? {
    val volunteer = call.principal<VolunteerPrincipal>()?.volunteer
    if (volunteer?.canManage != true) {
        call.respond(HttpStatusCode.Forbidden)
        return null
    }
    return volunteer
}

fun Route.adminController(volunteerRepository: VolunteerRepository) {
    authenticate {
        get("/api/volunteers") {
            requireManager() ?: return@get

            call.respond(volunteerRepository.all().map(::VolunteerView))
        }

        post("/api/volunteers") {
            val manager = requireManager() ?: return@post

            val request = call.receive<NewVolunteer>()
            if (request.login.isBlank() || request.password.length < MIN_PASSWORD_LENGTH) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Login must not be blank, password must have at least $MIN_PASSWORD_LENGTH characters",
                )
                return@post
            }

            val created =
                volunteerRepository.register(
                    request.login,
                    request.password,
                    canAccess = request.role.canAccess,
                    canManage = request.role.canManage,
                )
            if (created == null) {
                call.respond(HttpStatusCode.Conflict, "User with the same login exists")
                return@post
            }

            logger.info { "Manager ${manager.login} created volunteer ${request.login} with role ${request.role}" }
            call.respond(HttpStatusCode.Created)
        }

        patch("/api/volunteers/{id}") {
            val manager = requireManager() ?: return@patch

            val volunteerId =
                call.parameters["id"]?.toLongOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest)

            val volunteer =
                volunteerRepository.getById(volunteerId)
                    ?: return@patch call.respond(HttpStatusCode.NotFound)

            val patch = call.receive<VolunteerPatch>()

            if (patch.role == null && patch.password == null) {
                call.respond(HttpStatusCode.BadRequest, "At least role or password should be set")
                return@patch
            }
            if (patch.role != null && volunteerId == manager.id) {
                call.respond(HttpStatusCode.BadRequest, "You can not change your own role")
                return@patch
            }
            if (patch.password != null && patch.password.length < MIN_PASSWORD_LENGTH) {
                call.respond(HttpStatusCode.BadRequest, "Password must have at least 6 characters")
                return@patch
            }

            patch.role?.let {
                volunteerRepository.updateFlags(volunteerId, it.canAccess, it.canManage)
                logger.info { "Manager ${manager.login} changed role of ${volunteer.login} from ${VolunteerView(volunteer).role} to $it" }
            }
            patch.password?.let {
                volunteerRepository.setPassword(volunteerId, it)
                logger.info { "Manager ${manager.login} changed password of ${volunteer.login}" }
            }

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private val logger by getLogger()
