package org.icpclive.balloons

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.icpclive.balloons.admin.Role
import org.icpclive.balloons.db.DatabaseConfig
import org.icpclive.balloons.db.databaseModule
import org.icpclive.balloons.event.ClientMessage
import org.icpclive.balloons.event.EventStream
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.api.Color
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.ContestResultType
import org.icpclive.cds.api.PenaltyRoundingMode
import org.icpclive.cds.api.ProblemInfo
import org.icpclive.cds.api.RunInfo
import org.icpclive.cds.api.TeamInfo
import org.icpclive.cds.api.Verdict
import org.icpclive.cds.api.toProblemId
import org.icpclive.cds.api.toRunId
import org.icpclive.cds.api.toTeamId
import java.nio.file.Path
import java.sql.Statement
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

const val PASSWORD = "secret1"

// Use bcrypt cost 4 to make the tests faster.
private val PASSWORD_HASH = BCrypt.withDefaults().hashToString(4, PASSWORD.toCharArray())

class TestDatabase(
    directory: Path,
) {
    private val config = DatabaseConfig(directory.resolve("h2"))
    val module = databaseModule(config)
    private val connection = config.createConnection()

    /** @return id of the new volunteer, their password is [PASSWORD] */
    fun addVolunteer(
        login: String,
        role: Role = Role.VOLUNTEER,
    ): Long =
        connection
            .prepareStatement(
                "insert into volunteer (login, password_hash, can_access, can_manage) values (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            ).use { statement ->
                statement.setString(1, login)
                statement.setString(2, PASSWORD_HASH)
                statement.setBoolean(3, role.canAccess)
                statement.setBoolean(4, role.canManage)
                statement.executeUpdate()
                statement.generatedKeys.use { keys ->
                    keys.next()
                    keys.getLong(1)
                }
            }

    fun token(volunteerId: Long): String =
        JWT.create().withSubject(volunteerId.toString()).sign(Algorithm.HMAC256(module.secretKeyRepository.secretKey))

    /** Token of a freshly added volunteer. */
    fun tokenFor(
        login: String,
        role: Role = Role.VOLUNTEER,
    ): String = token(addVolunteer(login, role))
}

/**
 * Mock contest data source. CDS library will compute FTS and already solved flags.
 *
 * The contest has problems A and B, teams t1 (place "B2", hall "North") and t2 (no custom fields), and hidden team t3.
 */
class FakeCds {
    private val updates = Channel<ContestUpdate>(Channel.UNLIMITED)
    private val runTimes = mutableMapOf<String, Duration>()

    val flow: Flow<ContestUpdate> = updates.receiveAsFlow().addComputedData()

    fun contest(
        name: String = "Test contest",
        hiddenTeams: Set<String> = setOf("t3"),
    ) {
        updates.trySend(
            InfoUpdate(
                ContestInfo(
                    name = name,
                    resultType = ContestResultType.ICPC,
                    startTime = Clock.System.now() - 1.hours,
                    contestLength = 5.hours,
                    freezeTime = null,
                    problemList =
                        listOf(
                            ProblemInfo("B".toProblemId(), "B", "Problem B", ordinal = 1),
                            ProblemInfo("A".toProblemId(), "A", "Problem A", ordinal = 0, color = Color.normalize("#ff0000")),
                        ),
                    teamList =
                        listOf(
                            team("t2", isHidden = "t2" in hiddenTeams),
                            team("t1", isHidden = "t1" in hiddenTeams, customFields = mapOf("place" to "B2", "hall" to "North")),
                            team("t3", isHidden = "t3" in hiddenTeams),
                        ),
                    groupList = emptyList(),
                    organizationList = emptyList(),
                    languagesList = emptyList(),
                    penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
                ),
            ),
        )
    }

    /** Each new run is submitted a minute after the previous one; sending a known [id] again is a rejudge of that run. */
    fun run(
        id: String,
        team: String,
        problem: String,
        verdict: Verdict = Verdict.Accepted,
        isHidden: Boolean = false,
    ) {
        updates.trySend(
            RunUpdate(
                RunInfo(
                    id = id.toRunId(),
                    result = verdict.toICPCRunResult(),
                    problemId = problem.toProblemId(),
                    teamId = team.toTeamId(),
                    time = runTimes.getOrPut(id) { (runTimes.size + 1).minutes },
                    languageId = null,
                    isHidden = isHidden,
                ),
            ),
        )
    }

    private fun team(
        id: String,
        isHidden: Boolean,
        customFields: Map<String, String> = emptyMap(),
    ) = TeamInfo(
        id = id.toTeamId(),
        fullName = "Team $id",
        displayName = id,
        groups = emptyList(),
        hashTag = null,
        medias = emptyMap(),
        isHidden = isHidden,
        isOutOfContest = false,
        organizationId = null,
        customFields = customFields,
    )
}

/** Everything the stream sends to a new subscriber, in order. */
fun EventStream.inbox(scope: CoroutineScope): ReceiveChannel<ClientMessage> {
    val messages = Channel<ClientMessage>(Channel.UNLIMITED)
    subscribe(scope) { messages.send(it) }
    return messages
}

/** Next element, failing the test instead of hanging when nothing arrives. Uses real time even inside `runTest`. */
suspend fun <T> ReceiveChannel<T>.next(): T = withContext(Dispatchers.Default) { withTimeout(10.seconds) { receive() } }

class Api(
    val client: HttpClient,
    val db: TestDatabase,
    val cds: FakeCds,
) {
    suspend fun get(
        path: String,
        token: String? = null,
    ) = client.get(path) { token?.let { bearerAuth(it) } }

    suspend fun post(
        path: String,
        body: String,
        token: String? = null,
    ) = client.post(path) {
        token?.let { bearerAuth(it) }
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    suspend fun patch(
        path: String,
        body: String,
        token: String? = null,
    ) = client.patch(path) {
        token?.let { bearerAuth(it) }
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}

/** Runs the whole application (as in production, except for the contest data source) over a fresh database. */
fun apiTest(
    directory: Path,
    disableRegistration: Boolean = false,
    block: suspend Api.() -> Unit,
) = testApplication {
    val db = TestDatabase(directory)
    val cds = FakeCds()
    application {
        balloonsModule(db.module, EventStream(db.module.balloonRepository, cds.flow), Language.EN, disableRegistration)
    }
    Api(createClient { install(WebSockets) }, db, cds).block()
}

fun credentials(
    login: String,
    password: String = PASSWORD,
) = """{"login": "$login", "password": "$password"}"""

fun String.toJson(): JsonObject = Json.parseToJsonElement(this).jsonObject

suspend fun HttpResponse.json(): JsonObject = bodyAsText().toJson()

/** String form of a JSON primitive, `null` for JSON null or a missing key. */
val JsonElement?.text: String? get() = this?.jsonPrimitive?.contentOrNull
