package org.icpclive.balloons.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import kotlinx.coroutines.runBlocking
import org.icpclive.balloons.db.BalloonRepository
import org.icpclive.cds.util.getLogger
import kotlin.getValue

object ResetContest : CliktCommand("reset-contest") {
    private val balloonRepository: BalloonRepository by requireObject("balloonRepository")

    override fun help(context: Context) = "Delete all submissions and delivery status"

    override fun run() {
        runBlocking {
            balloonRepository.truncate()
            logger.info { "All balloons removed" }
        }
    }
}

private val logger by getLogger()
