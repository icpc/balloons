package org.icpclive.balloons.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.jooq.ExecutorProvider
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration

data class DatabaseModule(
    val balloonRepository: BalloonRepository,
    val secretKeyRepository: SecretKeyRepository,
    val volunteerRepository: VolunteerRepository,
)

fun databaseModule(databaseConfig: DatabaseConfig): DatabaseModule {
    val dbConnection = databaseConfig.createConnection()
    val jooq =
        DSL.using(
            DefaultConfiguration()
                .set(ExecutorProvider { Dispatchers.IO.asExecutor() })
                .set(dbConnection)
                .set(SQLDialect.H2),
        )
    val balloonRepository = BalloonRepository(jooq)
    val secretKeyRepository = SecretKeyRepository(jooq)
    val volunteerRepository = VolunteerRepository(jooq)
    return DatabaseModule(balloonRepository, secretKeyRepository, volunteerRepository)
}
