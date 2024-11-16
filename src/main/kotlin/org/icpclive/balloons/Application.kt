package org.icpclive.balloons

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.react
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json
import org.icpclive.balloons.admin.adminController
import org.icpclive.balloons.auth.CredentialValidator
import org.icpclive.balloons.auth.WebSocketAuthenticator
import org.icpclive.balloons.auth.authController
import org.icpclive.balloons.auth.installJwt
import org.icpclive.balloons.db.DatabaseConfig
import org.icpclive.balloons.db.databaseModule
import org.icpclive.balloons.event.CDSFetcher
import org.icpclive.balloons.event.EventStream
import org.icpclive.balloons.event.contestController
import org.icpclive.balloons.event.launchCDSFetcher
import org.icpclive.balloons.tools.H2Shell
import org.icpclive.balloons.tools.Volunteer
import org.icpclive.cds.cli.CdsCommandLineOptions

object Application : CliktCommand("balloons") {
    override val printHelpOnEmptyArgs = true
    override val invokeWithoutSubcommand = true

    private val cdsSettings by CdsCommandLineOptions()
    private val balloonSettings by BalloonOptions()

    init {
        subcommands(H2Shell, Volunteer)
    }

    private val databaseConfig by findOrSetObject("databaseConfig") {
        DatabaseConfig(cdsSettings.configDirectory.resolve("h2"))
    }

    override fun run() {
        val (balloonRepository, secretKeyRepository, volunteerRepository) = databaseModule(databaseConfig)
        currentContext.findOrSetObject("volunteerRepository") { volunteerRepository }

        if (currentContext.invokedSubcommand != null) {
            // Don't run app for subcommands
            return
        }

        // The best dependency injection framework for you
        val jwtVerifier = JWT.require(Algorithm.HMAC256(secretKeyRepository.secretKey)).build()
        val credentialValidator = CredentialValidator(volunteerRepository)
        val webSocketAuthenticator = WebSocketAuthenticator(jwtVerifier, credentialValidator)
        val eventStream = EventStream(balloonRepository)
        val cdsFetcher = CDSFetcher(eventStream, cdsSettings)

        embeddedServer(Netty, port = balloonSettings.port) {
            install(ContentNegotiation) {
                json(
                    Json {
                        encodeDefaults = true
                    },
                )
            }

            install(WebSockets)

            installJwt(jwtVerifier, credentialValidator)

            launchCDSFetcher(cdsFetcher)

            routing {
                singlePageApplication {
                    react("frontend")
                    useResources = true
                }
                adminController(volunteerRepository)
                authController(secretKeyRepository, volunteerRepository, balloonSettings.disableRegistration)
                contestController(eventStream, webSocketAuthenticator, balloonSettings)
            }
        }.start(wait = true)
    }
}

fun main(args: Array<String>) {
    Application.main(args)
}
