package org.icpclive.balloons.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import kotlinx.coroutines.runBlocking
import org.icpclive.balloons.db.VolunteerRepository
import org.icpclive.cds.util.getLogger
import org.jooq.exception.IntegrityConstraintViolationException
import kotlin.system.exitProcess

object Volunteer : CliktCommand("volunteer") {
    override fun help(context: Context) = "Manage volunteers"

    init {
        subcommands(CreateVolunteer, UpdateVolunteer, DeleteVolunteer)
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
    private val manage by option().boolean().help("Update management privileges")
    private val newPassword by option().help("Set new password")

    private val volunteerRepository: VolunteerRepository by requireObject("volunteerRepository")

    override fun run() {
        if (newPassword == null && manage == null) {
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
            manage?.let {
                volunteerRepository.updateFlags(volunteer.id!!, canManage = it)
                logger.info { "Volunteer $login is admin now" }
            }
        }
    }
}

object DeleteVolunteer : CliktCommand("delete") {
    override val printHelpOnEmptyArgs = true

    private val login by argument()

    private val volunteerRepository: VolunteerRepository by requireObject("volunteerRepository")

    override fun run() {
        val volunteer = volunteerRepository.getByLogin(login)

        if (volunteer == null) {
            logger.error { "Volunteer not found" }
            exitProcess(1)
        }

        runBlocking {
            try {
                volunteerRepository.delete(volunteer.id!!)
                logger.info { "Volunteer $login deleted" }
            } catch (_: IntegrityConstraintViolationException) {
                logger.error { "Can not delete volunteer $login: they already took some balloons." }
                exitProcess(1)
            }
        }
    }
}

private val logger by getLogger()
