package org.icpclive.balloons.db

import org.icpclive.balloons.db.tables.references.SECRET_KEY
import org.jooq.DSLContext
import java.security.SecureRandom

class SecretKeyRepository(private val jooq: DSLContext) {
    val secretKey by lazy { jooq.fetchOne(SECRET_KEY)?.data ?: storeKey() }

    private fun storeKey(): ByteArray {
        val secureRandom = SecureRandom.getInstanceStrong()
        val data = ByteArray(32)
        secureRandom.nextBytes(data)
        jooq.insertInto(SECRET_KEY).set(SECRET_KEY.DATA, data).execute()
        return data
    }
}
