package com.thesunnahrevival.sunnahassistant.backend

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import org.springframework.stereotype.Component

@Component
class KtorClient {

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) {
                gson()
            }
        }
    }

    fun getKtorClient(): HttpClient {
        return client
    }
}