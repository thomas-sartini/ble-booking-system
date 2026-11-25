package ch.hslu.blebooking.mobile.communication.networking

import ch.hslu.blebooking.mobile.communication.dto.request.RefreshTokenRequestDto
import ch.hslu.blebooking.mobile.communication.dto.response.RefreshTokenResponseDto
import ch.hslu.blebooking.mobile.token.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory responsible for creating and configuring [HttpClient] instances.
 */
internal object HttpClientFactory {

    /**
     * Creates a configured [HttpClient] instance.
     *
     * @param engine Optional platform-specific engine (e.g. OkHttp on Android, Darwin on iOS)
     *               or MockEngine for testing. If null, the default engine is selected.
     * @param baseUrl Base URL for backend requests.
     * @param tokenStorage Optional [TokenStorage] to manage authenticated Bearer tokens and auto-refresh.
     */
    fun create(
        engine: HttpClientEngine? = null,
        baseUrl: String,
        tokenStorage: TokenStorage? = null
    ): HttpClient {
        return if (engine != null) {
            HttpClient(engine) {
                configureDefaults(baseUrl, tokenStorage)
            }
        } else {
            HttpClient {
                configureDefaults(baseUrl, tokenStorage)
            }
        }
    }

    private fun HttpClientConfig<*>.configureDefaults(
        baseUrl: String,
        tokenStorage: TokenStorage?
    ) {
        defaultRequest {
            url(baseUrl)
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        if (tokenStorage != null) {
            install(Auth) {
                bearer {
                    loadTokens {
                        val access = tokenStorage.getAccessToken() ?: return@loadTokens null
                        val refresh = tokenStorage.getRefreshToken() ?: ""
                        BearerTokens(accessToken = access, refreshToken = refresh)
                    }

                    refreshTokens {
                        val currentRefreshToken = tokenStorage.getRefreshToken()
                        if (currentRefreshToken.isNullOrBlank()) {
                            tokenStorage.clearTokens()
                            return@refreshTokens null
                        }

                        val response = client.post("auth/refresh") {
                            markAsRefreshTokenRequest()
                            contentType(ContentType.Application.Json)
                            setBody(RefreshTokenRequestDto(refreshToken = currentRefreshToken))
                        }
                        when (response.status.value) {
                            in 200..299 -> {
                                val tokens: RefreshTokenResponseDto = response.body()
                                tokenStorage.storeTokens(tokens.accessToken, tokens.refreshToken)
                                BearerTokens(tokens.accessToken, tokens.refreshToken)
                            }
                            401 -> {
                                tokenStorage.clearTokens()
                                null
                            }
                            else -> throw io.ktor.client.plugins.ResponseException(
                                response, "Token refresh failed"
                            )
                        }
                    }

                    sendWithoutRequest { request ->
                        val url = request.url
                        val path = url.encodedPathSegments.joinToString("/")
                        val isPublicEndpoint = path.startsWith("auth/login") || path.startsWith("auth/refresh")

                        // Restrict credential transmission to the intended backend host only
                        val backendUrl = io.ktor.http.Url(baseUrl)
                        val isIntendedHost = url.host.equals(backendUrl.host, ignoreCase = true)

                        !isPublicEndpoint && isIntendedHost
                    }
                }
            }
        }
    }
}

