package org.icpclive.balloons.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.icpclive.balloons.db.VolunteerRepository
import org.icpclive.cds.util.getLogger
import kotlin.system.exitProcess

object Volunteer : CliktCommand("volunteer") {
    override fun help(context: Context) = "Manage volunteers"

    init {
        subcommands(CreateVolunteer, UpdateVolunteer)
    }

    override fun run() {}
}

object CreateVolunteer : CliktCommand("create") {
    override val printHelpOnEmptyArgs = true

    private val admin by option().flag().help("Make this volunteer an admin")
    private val login by argument()
    private val password by argument()

    private val volunteerRepository: VolunteerRepository by requireObject("volunteerRepository")

    override fun run() {
        runBlocking {
            if (volunteerRepository.register(login, password, canAccess = true, canManage = admin) != null) {
                logger.info { "Volunteer $login created" }
            } else {
                logger.error { "Volunteer not created: probably it already exists." }
                exitProcess(1)
            }
        }
    }
}

object UpdateVolunteer : CliktCommand("update") {
    override val printHelpOnEmptyArgs = true

    private val login by argument()
    private val makeAdmin by option().flag().help("Make this volunteer an admin")
    private val newPassword by option().help("Set new password")

    private val volunteerRepository: VolunteerRepository by requireObject("volunteerRepository")

    override fun run() {
        if (newPassword == null && !makeAdmin) {
            logger.error { "Nothing to update" }
            exitProcess(1)
        }

        val volunteer = volunteerRepository.getByLogin(login)

        if (volunteer == null) {
            logger.error { "Volunteer not found" }
            exitProcess(1)
        }

        runBlocking {
            newPassword?.let {
                volunteerRepository.setPassword(volunteer.id!!, it)
                logger.info { "Password for volunteer $login updated" }
            }
            if (makeAdmin) {
                volunteerRepository.updateFlags(volunteer.id!!, canManage = true)
                logger.info { "Volunteer $login is admin now" }
            }
        }
    }
}

private val logger by getLogger()
