package com.moodavatar.auth.routes

import com.moodavatar.auth.TestDatabase
import com.moodavatar.auth.models.EmailVerifications
import com.moodavatar.auth.models.PasswordResets
import com.moodavatar.auth.models.RefreshTokens
import com.moodavatar.auth.models.Users
import com.moodavatar.auth.plugins.configureSecurity
import com.moodavatar.auth.plugins.configureSerialization
import com.moodavatar.auth.services.AuthService
import com.moodavatar.auth.services.EmailService
import com.moodavatar.auth.services.UserServiceClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRoutesTest {
    private val db = TestDatabase
    private val authService = AuthService(TestDatabase.config)

    // EmailService and UserServiceClient point to a non-existent host;
    // exceptions are caught inside the route handlers, so tests still pass.
    private val emailService = EmailService(TestDatabase.config)
    private val userServiceClient by lazy {
        val httpClient = io.ktor.client.HttpClient(io.ktor.client.engine.cio.CIO)
        UserServiceClient(httpClient, "http://localhost:9999")
    }

    private fun Application.installTestApp() {
        configureSecurity()
        configureSerialization()
        routing { authRoutes(authService, emailService, userServiceClient) }
    }

    /** Wraps testApplication with the test environment config so that
     *  configureSecurity() can resolve jwt.secret / jwt.issuer / jwt.audience. */
    private fun routeTest(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            environment { config = TestDatabase.config }
            application { installTestApp() }
            block()
        }

    @BeforeTest
    fun cleanDb() {
        transaction {
            EmailVerifications.deleteAll()
            PasswordResets.deleteAll()
            RefreshTokens.deleteAll()
            Users.deleteAll()
        }
    }

    // ── POST /auth/register ───────────────────────────────────────────────────

    @Test
    fun `POST register returns 201 and user body for valid request`() =
        routeTest {
            val response =
                client.post("/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"reg@test.com","username":"reguser","password":"Password1"}""")
                }
            assertEquals(HttpStatusCode.Created, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("reguser", body["username"]?.jsonPrimitive?.content)
        }

    @Test
    fun `POST register returns 409 when email is already taken`() =
        routeTest {
            client.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"dup@test.com","username":"user1","password":"Password1"}""")
            }
            val response =
                client.post("/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"dup@test.com","username":"user2","password":"Password1"}""")
                }
            assertEquals(HttpStatusCode.Conflict, response.status)
        }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Test
    fun `POST login returns 200 and tokens for valid credentials`() =
        routeTest {
            client.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"login@test.com","username":"loginuser","password":"Password1"}""")
            }
            val response =
                client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"login@test.com","password":"Password1"}""")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertTrue(body.containsKey("accessToken"))
            assertTrue(body.containsKey("refreshToken"))
        }

    @Test
    fun `POST login returns 401 for wrong password`() =
        routeTest {
            client.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"wrong@test.com","username":"wronguser","password":"Password1"}""")
            }
            val response =
                client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"wrong@test.com","password":"BadPassword1"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `POST login returns 401 for unknown email`() =
        routeTest {
            val response =
                client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"nobody@test.com","password":"Password1"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    // ── POST /auth/refresh ────────────────────────────────────────────────────

    @Test
    fun `POST refresh returns 200 and new access token`() =
        routeTest {
            client.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"ref@test.com","username":"refuser","password":"Password1"}""")
            }
            val loginBody =
                Json
                    .parseToJsonElement(
                        client
                            .post("/auth/login") {
                                contentType(ContentType.Application.Json)
                                setBody("""{"email":"ref@test.com","password":"Password1"}""")
                            }.bodyAsText(),
                    ).jsonObject
            val refreshToken = loginBody["refreshToken"]!!.jsonPrimitive.content

            val response =
                client.post("/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"refreshToken":"$refreshToken"}""")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertTrue(body.containsKey("accessToken"))
        }

    @Test
    fun `POST refresh returns 401 for invalid token`() =
        routeTest {
            val response =
                client.post("/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"refreshToken":"garbage-token"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    // ── GET /auth/me ──────────────────────────────────────────────────────────

    @Test
    fun `GET me returns 401 without Authorization header`() =
        routeTest {
            val response = client.get("/auth/me")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `GET me returns 200 with valid JWT`() =
        routeTest {
            client.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"me@test.com","username":"meuser","password":"Password1"}""")
            }
            val loginBody =
                Json
                    .parseToJsonElement(
                        client
                            .post("/auth/login") {
                                contentType(ContentType.Application.Json)
                                setBody("""{"email":"me@test.com","password":"Password1"}""")
                            }.bodyAsText(),
                    ).jsonObject
            val token = loginBody["accessToken"]!!.jsonPrimitive.content

            val response = client.get("/auth/me") { bearerAuth(token) }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("me@test.com", body["email"]?.jsonPrimitive?.content)
        }

    // ── GET /auth/health ──────────────────────────────────────────────────────

    @Test
    fun `GET health returns 200`() =
        routeTest {
            val response = client.get("/auth/health")
            assertEquals(HttpStatusCode.OK, response.status)
        }
}
