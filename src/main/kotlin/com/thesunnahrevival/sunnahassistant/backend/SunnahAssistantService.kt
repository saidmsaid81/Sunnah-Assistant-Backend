package com.thesunnahrevival.sunnahassistant.backend

import io.ktor.client.call.*
import io.ktor.client.request.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SunnahAssistantService(private val ktorClient: KtorClient) {

    @Value("\${GEOCODING_API_KEY}")
    private lateinit var geocodingApiKey: String

    @Value("\${DOMAIN_NAME}")
    private lateinit var domainName: String

    @Value("\${SENDER_EMAIL}")
    private lateinit var senderEmail: String

    @Value("\${MY_EMAIL}")
    private lateinit var myEmail: String


    suspend fun getGeocodingData(address: String, language: String): GeocodingData {
        try {
            val successfulStatuses = listOf("OK", "ZERO_RESULTS")

            val geocodingApiResponse = ktorClient.getKtorClient().get("https://maps.googleapis.com/maps/api/geocode/json") {
                parameter("address", address)
                parameter("key", geocodingApiKey)
                parameter("language", language)
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
        ktorClient.sendEmailToDeveloper(domainName, senderEmail, myEmail, status)
        return GeocodingData(ArrayList(),"AN_ERROR_OCCURRED")
    }

}
