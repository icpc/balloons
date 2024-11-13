package org.icpclive.balloons.auth

import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText

/**
 * JavaScript doesn't allow to send HTTP headers while establishing WebSocket connection,
 * so we expect token in the first frame.
 */
class WebSocketAuthenticator(
    private val jwtVerifier: JWTVerifier,
    private val credentialValidator: CredentialValidator,
) {
    suspend fun authenticate(session: WebSocketSession): VolunteerPrincipal? {
        while (true) {
            val frame = session.incoming.receive()
            if (frame !is Frame.Text) {
                continue
            }

            val decoded =
                try {
                    jwtVerifier.verify(frame.readText())
                } catch (exc: JWTVerificationException) {
                    return null
                }

            val principal = credentialValidator.validate(JWTCredential(decoded))

            if (principal?.volunteer?.canAccess != true) {
                return null
            }

            return principal
        }
    }
}
