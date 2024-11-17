package org.icpclive.balloons.db

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.icpclive.balloons.db.tables.references.BALLOON
import org.icpclive.balloons.db.tables.references.VOLUNTEER
import org.icpclive.balloons.event.Balloon
import org.jooq.DSLContext
import org.jooq.Record

class BalloonRepository(private val jooq: DSLContext) {
    /**
     * @return record containing delivery status (`BALLOON.DELIVERED`) and responsible volunteer login (`VOLUNTEER.LOGIN`)
     */
    suspend fun getDelivery(balloon: Balloon): Record? =
        jooq.select(BALLOON.DELIVERED, VOLUNTEER.LOGIN)
            .from(BALLOON)
            .leftJoin(VOLUNTEER).on(BALLOON.VOLUNTEER_ID.eq(VOLUNTEER.ID))
            .where(
                BALLOON.PROBLEM_ID.eq(balloon.problemId),
                BALLOON.TEAM_ID.eq(balloon.teamId),
            )
            .awaitFirstOrNull()

    /**
     * @return `true` if balloon is reserved for this volunteer (even if it already was), `false` otherwise
     */
    suspend fun reserveBalloon(
        balloon: Balloon,
        volunteerId: Long,
    ): Boolean =
        jooq.mergeInto(BALLOON)
            .using(jooq.selectOne())
            .on(
                BALLOON.PROBLEM_ID.eq(balloon.problemId),
                BALLOON.TEAM_ID.eq(balloon.teamId),
            )
            .whenMatchedAnd(BALLOON.VOLUNTEER_ID.isNull.or(BALLOON.VOLUNTEER_ID.eq(volunteerId)))
            .thenUpdate().set(BALLOON.VOLUNTEER_ID, volunteerId)
            .whenNotMatchedThenInsert()
            .set(BALLOON.PROBLEM_ID, balloon.problemId)
            .set(BALLOON.TEAM_ID, balloon.teamId)
            .set(BALLOON.VOLUNTEER_ID, volunteerId)
            .awaitSingle() > 0

    /**
     * @return `true` if balloon was dropped, `false` otherwise
     */
    suspend fun dropBalloon(
        balloon: Balloon,
        volunteerId: Long,
    ): Boolean =
        jooq.update(BALLOON)
            .setNull(BALLOON.VOLUNTEER_ID)
            .where(
                BALLOON.PROBLEM_ID.eq(balloon.problemId),
                BALLOON.TEAM_ID.eq(balloon.teamId),
                BALLOON.VOLUNTEER_ID.eq(volunteerId),
                BALLOON.DELIVERED.eq(false),
            )
            .awaitSingle() > 0

    /**
     * @return `true` if balloon is delivered (even if it already was), `false` otherwise
     */
    suspend fun deliverBalloon(
        balloon: Balloon,
        volunteerId: Long,
    ): Boolean =
        jooq.update(BALLOON)
            .set(BALLOON.DELIVERED, true)
            .where(
                BALLOON.PROBLEM_ID.eq(balloon.problemId),
                BALLOON.TEAM_ID.eq(balloon.teamId),
                BALLOON.VOLUNTEER_ID.eq(volunteerId),
            )
            .awaitSingle() > 0
}
