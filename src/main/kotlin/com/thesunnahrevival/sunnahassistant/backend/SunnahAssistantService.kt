package com.thesunnahrevival.sunnahassistant.backend

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class SunnahAssistantService(private val ktorClient: KtorClient) {

    @Value("\${GEOCODING_API_KEY}")
    private lateinit var geocodingApiKey: String

    @Value("\${OPENWEATHER_API_KEY}")
    private lateinit var openWeatherGeocodingKey: String

    @Value("\${DOMAIN_NAME}")
    private lateinit var domainName: String

    @Value("\${SENDER_EMAIL}")
    private lateinit var senderEmail: String

    @Value("\${MY_EMAIL}")
    private lateinit var myEmail: String


    suspend fun getGeocodingData(address: String, language: String): GeocodingData {
        return try {
            getGoogleGeocodingData(address, language)

        } catch (exception: Exception) {
            reportGeocodingServerError("Your server has experienced an exception.\n ${exception.message}")
        }

    }

    private suspend fun SunnahAssistantService.getGoogleGeocodingData(
        address: String,
        language: String
    ): GeocodingData {
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
            reportGeocodingServerError("Google Geocoding Api ${geocodingData.status}")
            getOpenWeatherGeocodingData(address, language)
        }
    }

    private suspend fun SunnahAssistantService.getOpenWeatherGeocodingData(
        address: String,
        language: String
    ): GeocodingData {

        val geocodingApiResponse = ktorClient.getKtorClient().get("https://api.openweathermap.org/geo/1.0/direct") {
            parameter("q", address)
            parameter("appid", openWeatherGeocodingKey)
            parameter("limit", 1)
        }

        val openWeatherGeocodingData = geocodingApiResponse.body<List<OpenWeatherDto>>()

        return if (geocodingApiResponse.status.isSuccess()) {
            convertToGeocodingData(openWeatherGeocodingData.firstOrNull(), language)
        } else {
            reportGeocodingServerError("OpenWeather Api ${geocodingApiResponse.status}")
        }
    }

    private fun convertToGeocodingData(data: OpenWeatherDto?, language: String): GeocodingData {
        if (data == null) {
            return GeocodingData(emptyList(), "ZERO_RESULTS")
        }
        val name = data.localNames?.getOrDefault(language, data.name)
        val formattedAddress = listOfNotNull(name, data.state, data.country).joinToString(", ")

        val location = Location(data.lat, data.lon)
        val geometry = Geometry(location)

        return GeocodingData(listOf(Result(formattedAddress, geometry)), HttpStatus.OK.toString())
    }


    private suspend fun reportGeocodingServerError(status: String): GeocodingData {
        ktorClient.sendEmailToDeveloper(domainName, senderEmail, myEmail, status)
        return GeocodingData(ArrayList(),"AN_ERROR_OCCURRED")
    }

}
