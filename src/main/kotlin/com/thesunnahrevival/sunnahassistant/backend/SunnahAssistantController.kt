package com.thesunnahrevival.sunnahassistant.backend

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SunnahAssistantController(private val sunnahAssistantService: SunnahAssistantService) {

    @GetMapping("geocoding-data")
    suspend fun getGeocodingData(address: String): GeocodingData {
        return sunnahAssistantService.getGeocodingData(address)
    }

}