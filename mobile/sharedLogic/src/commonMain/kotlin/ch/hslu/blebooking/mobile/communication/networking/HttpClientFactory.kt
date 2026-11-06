package ch.hslu.blebooking.mobile.communication.networking

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory responsible for creating and configuring [HttpClient] instances.
 */
object HttpClientFactory {


    /**
     * Creates a configured [HttpClient] instance.
     *
     * @param engine Optional platform-specific engine (e.g. OkHttp on Android, Darwin on iOS)
     *               or MockEngine for testing. If null, the default engine is selected.
     */
    fun create(
        engine: HttpClientEngine? = null,
        baseUrl: String
    ): HttpClient {
        return if (engine != null) {
            HttpClient(engine) {
                configureDefaults(baseUrl)
            }
        } else {
            HttpClient {
                configureDefaults(baseUrl)
            }
        }
    }

    private fun HttpClientConfig<*>.configureDefaults(
        baseUrl: String
    ) {
        defaultRequest {
            url(baseUrl)
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
}

