package org.icpclive.balloons.event

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.icpclive.balloons.Language
import org.icpclive.balloons.auth.VolunteerPrincipal
import org.icpclive.balloons.auth.WebSocketAuthenticator
import org.icpclive.cds.util.getLogger
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

@Serializable
data class UserInfo(
    val canRegister: Boolean? = null,
    val login: String? = null,
    val canAccess: Boolean? = null,
    val canManage: Boolean? = null,
    val language: Language,
)

fun Route.contestController(
    eventStream: EventStream,
    webSocketAuthenticator: WebSocketAuthenticator,
    lang: Language,
    disableRegistration: Boolean,
) {
    authenticate(optional = true) {
        get("/api/info") {
            val principal = call.principal<VolunteerPrincipal>()

            if (principal == null) {
                call.respond(UserInfo(canRegister = !disableRegistration, language = lang))
            } else {
                val volunteer = principal.volunteer
                call.respond(
                    UserInfo(
                        login = volunteer.login,
                        canAccess = volunteer.canAccess,
                        canManage = volunteer.canManage,
                        language = lang,
                    ),
                )
            }
        }
    }

    webSocket("/api/balloons") {
        val principal =
            try {
                withTimeoutOrNull(AUTH_TIMEOUT) { webSocketAuthenticator.authenticate(this@webSocket) }
            } catch (_: ClosedReceiveChannelException) {
                return@webSocket
            } catch (_: IOException) {
                return@webSocket
            }

        if (principal == null) {
            send("""{"error": "access denied"}""")
            cancel()
            return@webSocket
        }

        val outgoingStream =
            eventStream.subscribe(this) { message ->
                // TODO: serialize whole message
                val frameContent =
                    when {
                        message.state != null -> jsonSerializer.encodeToString(message.state)
                        message.event != null -> jsonSerializer.encodeToString(message.event)
                        else -> throw IllegalArgumentException("invalid event: $message")
                    }

                send(frameContent)
            }

        try {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) {
                    return@consumeEach
                }

                val command = jsonSerializer.decodeFromString<Command>(frame.readText())
                if (!eventStream.processCommand(command, volunteerId = principal.volunteer.id!!)) {
                    send("""{"error": "command failed"}""")
                }
            }
        } catch (_: ClosedReceiveChannelException) {
        } catch (exc: IOException) {
            logger.info { "WebSocket of ${principal.volunteer.login} closed: ${exc.message}" }
            cancel()
        } catch (exc: SerializationException) {
            logger.warning { "Bad command from ${principal.volunteer.login}: ${exc.message}" }
        } finally {
            outgoingStream.cancel()
        }
    }
}

private val AUTH_TIMEOUT = 10.seconds
private val logger by getLogger()
private val jsonSerializer = Json { encodeDefaults = true }
