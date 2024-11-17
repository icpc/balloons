package org.icpclive.balloons.db

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.icpclive.balloons.db.tables.records.VolunteerRecord
import org.icpclive.balloons.db.tables.references.VOLUNTEER
import org.jooq.DSLContext
import org.jooq.UpdateSetMoreStep
import org.jooq.impl.DSL

private const val BCRYPT_COST = 12

class VolunteerRepository(private val jooq: DSLContext) {
    private val bcryptHasher: BCrypt.Hasher = BCrypt.withDefaults()

    fun getById(id: Long) = jooq.fetchOne(VOLUNTEER, VOLUNTEER.ID.eq(id))

    fun getByLogin(login: String) = jooq.fetchOne(VOLUNTEER, VOLUNTEER.LOGIN.eq(login))

    fun all(): List<VolunteerRecord> = jooq.selectFrom(VOLUNTEER).orderBy(VOLUNTEER.ID).fetchInto(VolunteerRecord::class.java)

    /**
     * @return registered [VolunteerRecord] on success, `null` on failure
     */
    suspend fun register(
        login: String,
        password: String,
        canAccess: Boolean,
        canManage: Boolean = false,
    ): VolunteerRecord? =
        jooq.selectFrom(
            DSL.finalTable(
                DSL.mergeInto(VOLUNTEER)
                    .using(jooq.selectOne())
                    .on(VOLUNTEER.LOGIN.eq(login))
                    .whenNotMatchedThenInsert()
                    .set(VOLUNTEER.LOGIN, login)
                    .set(VOLUNTEER.PASSWORD_HASH, getHash(password))
                    .set(VOLUNTEER.CAN_ACCESS, canAccess)
                    .set(VOLUNTEER.CAN_MANAGE, canManage),
            ),
        ).awaitFirstOrNull()

    suspend fun setPassword(
        id: Long,
        password: String,
    ) {
        jooq.update(VOLUNTEER)
            .set(VOLUNTEER.PASSWORD_HASH, getHash(password))
            .where(VOLUNTEER.ID.eq(id))
            .awaitSingle()
    }

    suspend fun updateFlags(
        id: Long,
        canAccess: Boolean? = null,
        canManage: Boolean? = null,
    ) {
        if (canAccess == null && canManage == null) {
            throw IllegalStateException("At least one flag should be set")
        }

        val query = jooq.update(VOLUNTEER)

        canAccess?.let { query.set(VOLUNTEER.CAN_ACCESS, it) }
        canManage?.let { query.set(VOLUNTEER.CAN_MANAGE, it) }

        (query as UpdateSetMoreStep<*>).where(VOLUNTEER.ID.eq(id)).awaitSingle()
    }

    private fun getHash(password: String): String = bcryptHasher.hashToString(BCRYPT_COST, password.toCharArray())
}
