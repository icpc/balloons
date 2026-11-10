package org.icpclive.balloons

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.CacheControl
import io.ktor.http.content.CachingOptions
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.react
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.condition
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.identity
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.uri
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.websocket.WebSocketDeflateExtension
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.icpclive.balloons.admin.adminController
import org.icpclive.balloons.auth.CredentialValidator
import org.icpclive.balloons.auth.WebSocketAuthenticator
import org.icpclive.balloons.auth.authController
import org.icpclive.balloons.auth.installJwt
import org.icpclive.balloons.db.DatabaseModule
import org.icpclive.balloons.event.EventStream
import org.icpclive.balloons.event.contestController
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

fun Application.balloonsModule(
    database: DatabaseModule,
    eventStream: EventStream,
    lang: Language,
    disableRegistration: Boolean,
) {
    val (_, secretKeyRepository, volunteerRepository) = database

    // The best dependency injection framework for you
    val jwtVerifier = JWT.require(Algorithm.HMAC256(secretKeyRepository.secretKey)).build()
    val credentialValidator = CredentialValidator(volunteerRepository)
    val webSocketAuthenticator = WebSocketAuthenticator(jwtVerifier, credentialValidator)

    install(CallLogging)
    install(Compression) {
        gzip {
            condition {
                request.uri.startsWith("/assets/")
            }
        }
        deflate {
            condition {
                request.uri.startsWith("/assets/")
            }
        }
        identity()
    }

    install(ContentNegotiation) {
        json(
            Json {
                encodeDefaults = true
            },
        )
    }

    install(WebSockets) {
        pingPeriod = 15.seconds
        extensions {
            install(WebSocketDeflateExtension)
        }
    }

    installJwt(jwtVerifier, credentialValidator)

    launch { eventStream.consumeCds() }

    routing {
        adminController(volunteerRepository)
        authController(secretKeyRepository, volunteerRepository, disableRegistration)
        contestController(eventStream, webSocketAuthenticator, lang, disableRegistration)
        route("/") {
            install(CachingHeaders) {
                options { call, _ ->
                    if (call.request.uri.startsWith("/assets/")) {
                        CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 365.days.inWholeSeconds.toInt()))
                    } else {
                        CachingOptions(CacheControl.NoCache(null))
                    }
                }
            }

            singlePageApplication {
                react("frontend")
                useResources = true
            }
        }
    }
}
