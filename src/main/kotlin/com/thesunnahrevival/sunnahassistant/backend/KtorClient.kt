package com.thesunnahrevival.sunnahassistant.backend

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.gson.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class KtorClient {

    @Value("\${MAILGUN_API_KEY}")
    private lateinit var mailGunApiKey: String

    private val client by lazy {
        HttpClient {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = "api", password = mailGunApiKey)
                    }
                }
            }
            install(ContentNegotiation) {
                gson()
            }
        }
    }

    fun getKtorClient(): HttpClient {
        return client
    }

    suspend fun sendEmailToDeveloper(domainName: String, senderEmail: String, myEmail: String, message: String) {
        client.post("https://api.mailgun.net/v3/$domainName/messages") {
            parameter(
                "from",
                "Sunnah Assistant Backend <$senderEmail>"
            )
            parameter("to", myEmail)
            parameter("subject", "Sunnah Assistant Api Failure")
            parameter("text", message)
        }
    }
}