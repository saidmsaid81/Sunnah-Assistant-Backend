package com.thesunnahrevival.sunnahassistant.backend

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class OpenWeatherDto(
    val name: String,
    @field:Expose @field:SerializedName("local_names") val localNames: Map<String, String>?,
    val lat: Float,
    val lon: Float,
    val country: String,
    val state: String?
)
