package org.icpclive.balloons

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.icpclive.balloons.db.DatabaseConfig
import org.icpclive.balloons.db.databaseModule
import org.icpclive.balloons.event.EventStream
import org.icpclive.balloons.tools.H2Shell
import org.icpclive.balloons.tools.ResetContest
import org.icpclive.balloons.tools.Volunteer
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.cli.CdsCommandLineOptions

object Application : CliktCommand("balloons") {
    override val printHelpOnEmptyArgs = true
    override val invokeWithoutSubcommand = true

    private val cdsSettings by CdsCommandLineOptions()
    private val balloonSettings by BalloonOptions()

    init {
        subcommands(H2Shell, Volunteer, ResetContest)
    }

    private val databaseConfig by findOrSetObject("databaseConfig") {
        DatabaseConfig(cdsSettings.configDirectory.resolve("h2"))
    }

    override fun run() {
        val database = databaseModule(databaseConfig)
        currentContext.findOrSetObject("volunteerRepository") { database.volunteerRepository }
        currentContext.findOrSetObject("balloonRepository") { database.balloonRepository }

        if (currentContext.invokedSubcommand != null) {
            // Don't run app for subcommands
            return
        }

        val eventStream = EventStream(database.balloonRepository, cdsSettings.toFlow().addComputedData())

        embeddedServer(
            Netty,
            configure = {
                connector { port = balloonSettings.port }
                callGroupSize = parallelism * 2
                runningLimit = parallelism * 16
            },
        ) {
            balloonsModule(database, eventStream, balloonSettings.lang, balloonSettings.disableRegistration)
        }.start(wait = true)
    }
}

fun main(args: Array<String>) {
    Application.main(args)
}
