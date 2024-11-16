package org.icpclive.balloons

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int

class BalloonOptions : OptionGroup("Balloons options") {
    val port: Int by option("-p", "--port", help = "Port to run web app on")
        .int()
        .default(8001)

    val lang: Language by option().enum<Language> { it.name.lowercase() }.default(Language.EN).help("Web interface language")

    val disableRegistration by option().flag().help("Disable web registration")
}

@Suppress("UNUSED")
enum class Language { RU, EN }
