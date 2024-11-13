package org.icpclive.balloons

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.serialization.Serializable

@Serializable
data class BalloonConfig(
    val allowPublicRegistration: Boolean = true,
    val port: Int = 8001,
)

class BalloonOptions : OptionGroup("Balloons options") {
    val port: Int by option("-p", "--port", help = "Port to run web app on")
        .int()
        .default(8001)

    val disableRegistration by option().flag().help("Disable web registration")
}
