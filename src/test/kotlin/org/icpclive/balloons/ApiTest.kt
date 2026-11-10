package org.icpclive.balloons

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.icpclive.balloons.admin.Role
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ApiTest {
    @TempDir
    lateinit var directory: Path

    private fun test(
        disableRegistration: Boolean = false,
        block: suspend Api.() -> Unit,
    ) = apiTest(directory, disableRegistration, block)

    private suspend fun Api.login(
        login: String,
        password: String = PASSWORD,
    ) = post("/api/login", credentials(login, password))

    /** Login to role of every volunteer, as a manager sees them. */
    private suspend fun Api.roles(managerToken: String): Map<String, String?> =
        Json
            .parseToJsonElement(get("/api/volunteers", managerToken).bodyAsText())
            .jsonArray
            .associate { it.jsonObject["login"].text!! to it.jsonObject["role"].text }

    @Nested
    inner class Info {
        @Test
        fun `anonymous user sees whether registration is open`() =
            test {
                val info = get("/api/info").json()

                assertEquals("true", info["canRegister"].text)
                assertEquals("EN", info["language"].text)
                assertNull(info["login"].text)
            }

        @Test
        fun `anonymous user sees that registration is closed`() =
            test(disableRegistration = true) {
                assertEquals("false", get("/api/info").json()["canRegister"].text)
            }

        @Test
        fun `logged in users see their login and permissions`() =
            test {
                val expected =
                    mapOf(
                        Role.PENDING to listOf("false", "false"),
                        Role.VOLUNTEER to listOf("true", "false"),
                        Role.MANAGER to listOf("true", "true"),
                    )
                for ((role, flags) in expected) {
                    val info = get("/api/info", db.tokenFor("user-$role", role)).json()

                    assertEquals("user-$role", info["login"].text)
                    assertEquals(flags, listOf(info["canAccess"].text, info["canManage"].text))
                }
            }

        @Test
        fun `bad token and token of a deleted volunteer are rejected`() =
            test {
                assertEquals(HttpStatusCode.Unauthorized, get("/api/info", "garbage").status)
                assertEquals(HttpStatusCode.Unauthorized, get("/api/info", db.token(12345)).status)
            }
    }

    @Nested
    inner class Login {
        @Test
        fun `correct password gives a working token`() =
            test {
                db.addVolunteer("alice")

                val response = login("alice")
                assertEquals(HttpStatusCode.OK, response.status)
                val token = assertNotNull(response.json()["token"].text)
                assertEquals("alice", get("/api/info", token).json()["login"].text)
            }

        @Test
        fun `wrong password and unknown login are rejected`() =
            test {
                db.addVolunteer("alice")

                assertEquals(HttpStatusCode.Forbidden, login("alice", "wrong-password").status)
                assertEquals(HttpStatusCode.Forbidden, login("nobody").status)
            }
    }

    @Nested
    inner class Register {
        @Test
        fun `self-registered user is logged in but pending`() =
            test {
                val response = post("/api/register", credentials("newbie"))
                assertEquals(HttpStatusCode.OK, response.status)

                val info = get("/api/info", assertNotNull(response.json()["token"].text)).json()
                assertEquals(
                    listOf("newbie", "false", "false"),
                    listOf(info["login"], info["canAccess"], info["canManage"]).map {
                        it.text
                    },
                )
                assertEquals(HttpStatusCode.OK, login("newbie").status)
            }

        @Test
        fun `occupied login is rejected`() =
            test {
                db.addVolunteer("alice")

                assertEquals(HttpStatusCode.Conflict, post("/api/register", credentials("alice", "another-password")).status)
                assertEquals(HttpStatusCode.Forbidden, login("alice", "another-password").status)
            }

        @Test
        fun `self-registration can be disabled`() =
            test(disableRegistration = true) {
                assertEquals(HttpStatusCode.Forbidden, post("/api/register", credentials("newbie")).status)
                assertEquals(HttpStatusCode.Forbidden, login("newbie").status)
            }

        @Test
        fun `manager registers volunteers with access even when self-registration is disabled`() =
            test(disableRegistration = true) {
                val manager = db.tokenFor("boss", Role.MANAGER)

                assertEquals(HttpStatusCode.Created, post("/api/register", credentials("newbie"), manager).status)
                assertEquals("VOLUNTEER", roles(manager)["newbie"])
            }

        @Test
        fun `logged in non-manager can not register others`() =
            test {
                val volunteer = db.tokenFor("alice")

                assertEquals(HttpStatusCode.Forbidden, post("/api/register", credentials("newbie"), volunteer).status)
                assertEquals(HttpStatusCode.Forbidden, login("newbie").status)
            }
    }

    @Nested
    inner class ListVolunteers {
        @Test
        fun `manager sees everyone with roles`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)
                db.addVolunteer("alice")
                db.addVolunteer("newbie", Role.PENDING)

                assertEquals(mapOf("boss" to "MANAGER", "alice" to "VOLUNTEER", "newbie" to "PENDING"), roles(manager))
            }

        @Test
        fun `only managers may list volunteers`() =
            test {
                assertEquals(HttpStatusCode.Unauthorized, get("/api/volunteers").status)
                assertEquals(HttpStatusCode.Forbidden, get("/api/volunteers", db.tokenFor("alice")).status)
                assertEquals(HttpStatusCode.Forbidden, get("/api/volunteers", db.tokenFor("newbie", Role.PENDING)).status)
            }
    }

    @Nested
    inner class CreateVolunteer {
        @Test
        fun `new volunteer gets the requested role and can log in`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)

                for (role in Role.entries) {
                    val body = """{"login": "user-$role", "password": "$PASSWORD", "role": "$role"}"""
                    assertEquals(HttpStatusCode.Created, post("/api/volunteers", body, manager).status)
                    assertEquals(HttpStatusCode.OK, login("user-$role").status)
                }

                assertEquals(
                    mapOf("boss" to "MANAGER", "user-PENDING" to "PENDING", "user-VOLUNTEER" to "VOLUNTEER", "user-MANAGER" to "MANAGER"),
                    roles(manager),
                )
            }

        @Test
        fun `role defaults to volunteer and creation ignores disabled self-registration`() =
            test(disableRegistration = true) {
                val manager = db.tokenFor("boss", Role.MANAGER)

                assertEquals(HttpStatusCode.Created, post("/api/volunteers", credentials("alice"), manager).status)
                assertEquals("VOLUNTEER", roles(manager)["alice"])
            }

        @Test
        fun `occupied login is rejected and keeps the old password`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)
                db.addVolunteer("alice")

                assertEquals(HttpStatusCode.Conflict, post("/api/volunteers", credentials("alice", "another-password"), manager).status)
                assertEquals(HttpStatusCode.OK, login("alice").status)
            }

        @Test
        fun `blank login, short password and unknown role are rejected`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)

                assertEquals(HttpStatusCode.BadRequest, post("/api/volunteers", credentials(" "), manager).status)
                assertEquals(HttpStatusCode.BadRequest, post("/api/volunteers", credentials("alice", "12345"), manager).status)
                assertEquals(
                    HttpStatusCode.BadRequest,
                    post("/api/volunteers", """{"login": "alice", "password": "$PASSWORD", "role": "GOD"}""", manager).status,
                )
                assertEquals(HttpStatusCode.BadRequest, post("/api/volunteers", """{"login": "alice"}""", manager).status)
                assertEquals(setOf("boss"), roles(manager).keys)
            }

        @Test
        fun `only managers may create volunteers`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)

                assertEquals(HttpStatusCode.Unauthorized, post("/api/volunteers", credentials("mallory")).status)
                assertEquals(HttpStatusCode.Forbidden, post("/api/volunteers", credentials("mallory"), db.tokenFor("alice")).status)
                assertEquals(setOf("boss", "alice"), roles(manager).keys)
            }
    }

    @Nested
    inner class UpdateVolunteer {
        @Test
        fun `manager changes roles`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)
                val newbie = db.addVolunteer("newbie", Role.PENDING)

                for (role in listOf("VOLUNTEER", "MANAGER", "PENDING")) {
                    assertEquals(HttpStatusCode.NoContent, patch("/api/volunteers/$newbie", """{"role": "$role"}""", manager).status)
                    assertEquals(role, roles(manager)["newbie"])
                }
            }

        @Test
        fun `role change takes effect for an already issued token`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)
                val alice = db.addVolunteer("alice", Role.MANAGER)

                assertEquals(HttpStatusCode.OK, get("/api/volunteers", db.token(alice)).status)
                patch("/api/volunteers/$alice", """{"role": "VOLUNTEER"}""", manager)
                assertEquals(HttpStatusCode.Forbidden, get("/api/volunteers", db.token(alice)).status)
            }

        @Test
        fun `manager changes password without touching the role`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)
                val alice = db.addVolunteer("alice")

                assertEquals(HttpStatusCode.NoContent, patch("/api/volunteers/$alice", """{"password": "new-password"}""", manager).status)

                assertEquals(HttpStatusCode.Forbidden, login("alice").status)
                assertEquals(HttpStatusCode.OK, login("alice", "new-password").status)
                assertEquals("VOLUNTEER", roles(manager)["alice"])
            }

        @Test
        fun `role and password can be changed in one request`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)
                val alice = db.addVolunteer("alice")

                val body = """{"role": "MANAGER", "password": "new-password"}"""
                assertEquals(HttpStatusCode.NoContent, patch("/api/volunteers/$alice", body, manager).status)

                assertEquals(HttpStatusCode.OK, login("alice", "new-password").status)
                assertEquals("MANAGER", roles(manager)["alice"])
            }

        @Test
        fun `managers can change their own password but not their own role`() =
            test {
                val boss = db.addVolunteer("boss", Role.MANAGER)
                val manager = db.token(boss)

                assertEquals(HttpStatusCode.BadRequest, patch("/api/volunteers/$boss", """{"role": "VOLUNTEER"}""", manager).status)
                assertEquals(
                    HttpStatusCode.BadRequest,
                    patch("/api/volunteers/$boss", """{"role": "MANAGER", "password": "new-password"}""", manager).status,
                )
                assertEquals("MANAGER", roles(manager)["boss"])
                assertEquals(HttpStatusCode.OK, login("boss").status)

                assertEquals(HttpStatusCode.NoContent, patch("/api/volunteers/$boss", """{"password": "new-password"}""", manager).status)
                assertEquals(HttpStatusCode.OK, login("boss", "new-password").status)
            }

        @Test
        fun `invalid requests are rejected and change nothing`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)
                val alice = db.addVolunteer("alice")

                assertEquals(HttpStatusCode.BadRequest, patch("/api/volunteers/$alice", "{}", manager).status)
                assertEquals(HttpStatusCode.BadRequest, patch("/api/volunteers/$alice", """{"role": "GOD"}""", manager).status)
                assertEquals(HttpStatusCode.BadRequest, patch("/api/volunteers/$alice", """{"password": "12345"}""", manager).status)
                assertEquals(
                    HttpStatusCode.BadRequest,
                    patch("/api/volunteers/$alice", """{"role": "MANAGER", "password": "12345"}""", manager).status,
                )
                assertEquals(HttpStatusCode.BadRequest, patch("/api/volunteers/abc", """{"role": "MANAGER"}""", manager).status)
                assertEquals(HttpStatusCode.NotFound, patch("/api/volunteers/12345", """{"role": "MANAGER"}""", manager).status)

                assertEquals("VOLUNTEER", roles(manager)["alice"])
                assertEquals(HttpStatusCode.OK, login("alice").status)
            }

        @Test
        fun `only managers may update volunteers`() =
            test {
                val manager = db.tokenFor("boss", Role.MANAGER)
                val alice = db.addVolunteer("alice")
                val body = """{"role": "MANAGER", "password": "new-password"}"""

                assertEquals(HttpStatusCode.Unauthorized, patch("/api/volunteers/$alice", body).status)
                assertEquals(HttpStatusCode.Forbidden, patch("/api/volunteers/$alice", body, db.token(alice)).status)

                assertEquals("VOLUNTEER", roles(manager)["alice"])
                assertEquals(HttpStatusCode.OK, login("alice").status)
            }
    }
}
