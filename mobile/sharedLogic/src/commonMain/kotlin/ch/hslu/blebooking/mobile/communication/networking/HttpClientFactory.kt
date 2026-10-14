package ch.hslu.blebooking.mobile.communication.networking

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine


fun create(engine: HttpClientEngine): HttpClient {
    return HttpClient(engine){
        // TODO configure client
    }
}
