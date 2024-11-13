package org.icpclive.balloons.db

import org.jooq.SQLDialect
import org.jooq.impl.DSL

data class DatabaseModule(
    val balloonRepository: BalloonRepository,
    val secretKeyRepository: SecretKeyRepository,
    val volunteerRepository: VolunteerRepository,
)

fun databaseModule(databaseConfig: DatabaseConfig): DatabaseModule {
    val dbConnection = databaseConfig.createConnection()
    val jooq = DSL.using(dbConnection, SQLDialect.H2)
    val balloonRepository = BalloonRepository(jooq)
    val secretKeyRepository = SecretKeyRepository(jooq)
    val volunteerRepository = VolunteerRepository(jooq)
    return DatabaseModule(balloonRepository, secretKeyRepository, volunteerRepository)
}
