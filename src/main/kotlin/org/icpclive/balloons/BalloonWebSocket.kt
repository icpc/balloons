package org.icpclive.balloons

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.balloons.auth.VolunteerPrincipal
import org.icpclive.balloons.event.Command
import org.icpclive.balloons.event.EventStream
import org.icpclive.balloons.event.Reload
import org.icpclive.cds.util.getLogger
import org.koin.ktor.ext.inject

fun Route.balloonWebsocket() {
    val eventStream: EventStream by inject()

    authenticate {
        webSocket("/api/balloons") {
            val principal = call.principal<VolunteerPrincipal>()

            if (principal?.volunteer?.canAccess != true) {
                // ktor gives us `webSocketRaw` or `webSocket`. First requires manual processing of all frames (including pings),
                // second doesn't allow us to return HTTP response, so here we go.
                send("""{"error": "access denied"}""")
                return@webSocket
            }

            val outgoingStream =
                launch {
                    var expectState = true

                    eventStream.stream.collect { (state, event) ->
                        if (expectState || event == Reload) {
                            expectState = false
                            send(Json.Default.encodeToString(state))
                        } else {
                            send(Json.Default.encodeToString(event))
                        }
                    }
                }

            runCatching {
                incoming.consumeEach { frame ->
                    if (frame !is Frame.Text) {
                        return@consumeEach
                    }

                    val command = Json.Default.decodeFromString<Command>(frame.readText())
                    if (!eventStream.processCommand(command, volunteerId = principal.volunteer.id!!)) {
                        send("""{"error": "command failed"}""")
                    }
                }
            }.onFailure { exception ->
                logger.warning { "WebSocket exception: ${exception.localizedMessage}" }
            }.also {
                outgoingStream.cancel()
            }
        }
    }
}

private val logger by getLogger()
