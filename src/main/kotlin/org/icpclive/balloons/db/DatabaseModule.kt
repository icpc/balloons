package org.icpclive.balloons.db

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.newFixedThreadPoolContext
import org.jooq.ExecutorProvider
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration

data class DatabaseModule(
    val balloonRepository: BalloonRepository,
    val secretKeyRepository: SecretKeyRepository,
    val volunteerRepository: VolunteerRepository,
)

@OptIn(DelicateCoroutinesApi::class)
fun databaseModule(databaseConfig: DatabaseConfig): DatabaseModule {
    val dbConnection = databaseConfig.createConnection()
    val jooq =
        DSL.using(
            DefaultConfiguration()
                .set(ExecutorProvider { newFixedThreadPoolContext(16, "jooq").asExecutor() })
                .set(dbConnection)
                .set(SQLDialect.H2),
        )
    val balloonRepository = BalloonRepository(jooq)
    val secretKeyRepository = SecretKeyRepository(jooq)
    val volunteerRepository = VolunteerRepository(jooq)
    return DatabaseModule(balloonRepository, secretKeyRepository, volunteerRepository)
}
