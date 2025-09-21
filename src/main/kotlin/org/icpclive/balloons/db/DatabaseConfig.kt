package org.icpclive.balloons.db

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.absolutePathString

data class DatabaseConfig(
    val path: Path,
) {
    fun toJdbcUrl() = "jdbc:h2:${path.absolutePathString()};AUTO_SERVER=TRUE"

    fun createConnection(): Connection = DriverManager.getConnection("${toJdbcUrl()};INIT=RUNSCRIPT FROM 'classpath:schema.sql'")
}
