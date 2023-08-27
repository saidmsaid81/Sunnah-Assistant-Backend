package com.thesunnahrevival.sunnahassistant.backend

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.gson.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SunnahAssistantService {

    @Value("\${GEOCODING_API_KEY}")
    private lateinit var geocodingApiKey: String

    @Value("\${DOMAIN_NAME}")
    private lateinit var domainName: String

    @Value("\${SENDER_EMAIL}")
    private lateinit var senderEmail: String

    @Value("\${MY_EMAIL}")
    private lateinit var myEmail: String


    suspend fun getGeocodingData(address: String): GeocodingData {
        try {
            val successfulStatuses = listOf("OK", "ZERO_RESULTS")

            val geocodingApiResponse = KtorClient.get().get("https://maps.googleapis.com/maps/api/geocode/json") {
                parameter("address", address)
                parameter("key", geocodingApiKey)
            }

            val geocodingData = geocodingApiResponse.body<GeocodingData>()

            return if (successfulStatuses.contains(geocodingData.status)) {
                geocodingData
            } else {
                reportGeocodingServerError(geocodingData.status)
            }

        } catch (exception: Exception) {
            return reportGeocodingServerError("Your server has experienced an exception.\n ${exception.message}")
        }

    }

    suspend fun reportGeocodingServerError(status: String): GeocodingData {

        KtorClient.get().post("https://api.mailgun.net/v3/$domainName/messages") {
            parameter(
                "from",
                "Sunnah Assistant Backend <$senderEmail>"
            )
            parameter("to", myEmail)
            parameter("subject", "Sunnah Assistant Api Failure")
            parameter("text", status)
        }

        return GeocodingData(ArrayList(),"AN_ERROR_OCCURRED")
    }

}

object KtorClient {

    private val client by lazy {
        HttpClient {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = "api", password = MAILGUN_API_KEY)
                    }
                }
            }
            install(ContentNegotiation) {
                gson()
            }
        }
    }

    fun get(): HttpClient {
        return client
    }
}