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
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.balloons.BalloonOptions
import org.icpclive.balloons.Language
import org.icpclive.balloons.auth.VolunteerPrincipal
import org.icpclive.balloons.auth.WebSocketAuthenticator
import org.icpclive.cds.util.getLogger

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
    settings: BalloonOptions,
) {
    authenticate(optional = true) {
        get("/api/info") {
            val principal = call.principal<VolunteerPrincipal>()

            if (principal == null) {
                call.respond(UserInfo(canRegister = !settings.disableRegistration, language = settings.lang))
            } else {
                val volunteer = principal.volunteer
                call.respond(
                    UserInfo(
                        login = volunteer.login,
                        canAccess = volunteer.canAccess,
                        canManage = volunteer.canManage,
                        language = settings.lang,
                    ),
                )
            }
        }
    }

    webSocket("/api/balloons") {
        val principal =
            try {
                webSocketAuthenticator.authenticate(this)
            } catch (exc: ClosedReceiveChannelException) {
                return@webSocket
            }

        if (principal?.volunteer?.canAccess != true) {
            send("""{"error": "access denied"}""")
            return@webSocket
        }

        val outgoingStream =
            launch {
                var expectState = true

                eventStream.stream.collect { (state, event) ->
                    if (expectState || event == Reload) {
                        expectState = false
                        send(jsonSerializer.encodeToString(state))
                    } else {
                        send(jsonSerializer.encodeToString(event))
                    }
                }
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
        } catch (ignored: ClosedReceiveChannelException) {
        } catch (exc: Exception) {
            logger.warning { "WebSocket exception: ${exc.localizedMessage}" }
            throw exc
        } finally {
            outgoingStream.cancel()
        }
    }
}

private val logger by getLogger()
private val jsonSerializer = Json { encodeDefaults = true }
