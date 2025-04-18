package com.thesunnahrevival.sunnahassistant.backend

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import io.ktor.http.*
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

private const val userAgentHeader = "User-Agent"

private const val appVersionHeader = "App-Version"

@Component
class RateLimitingFilter(
    private val asyncEmailService: AsyncEmailService
) : Filter {

    @Value("\${EXPECTED_USER_AGENT}")
    private lateinit var expectedUserAgent: String

    @Value("\${CURRENT_APP_VERSION}")
    private lateinit var currentAppVersion: String

    private val buckets = Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS) // expire after 1 hour of inactivity
        .build<String, Bucket> { createBucket() }

    private fun createBucket(): Bucket {
        val limit = Bandwidth.classic(15, Refill.greedy(15, Duration.ofHours(1)))
        return Bucket.builder()
            .addLimit(limit)
            .build()
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            val httpRequest = request as HttpServletRequest
            val httpResponse = response as HttpServletResponse

            // Exclude actuator endpoints from rate limiting and authentication
            if (httpRequest.requestURI.startsWith("/actuator")) {
                chain.doFilter(request, response)
                return
            }
            
            val ipAddress = httpRequest.remoteAddr
            val userAgentHeader = httpRequest.getHeader(userAgentHeader)
            val appVersionHeader = httpRequest.getHeader(appVersionHeader)

            when {
                userAgentHeader == null || !userAgentHeader.matches(expectedUserAgent.toRegex()) -> {
                    response.status = HttpStatusCode.Unauthorized.value
                }
                appVersionHeader == null -> {
                    response.status = HttpStatusCode.Unauthorized.value
                }
                appVersionHeader.toIntOrNull() != null && appVersionHeader.toIntOrNull()!! < currentAppVersion.toInt() -> {
                    response.status = HttpStatusCode.UpgradeRequired.value
                }
                else -> {
                    val bucket = buckets.get(ipAddress)
                    val consumptionProbe = bucket.tryConsumeAndReturnRemaining(1)
                    if (consumptionProbe.isConsumed) {
                        chain.doFilter(request, response)
                    } else {
                        httpResponse.status = HttpStatusCode.TooManyRequests.value
                        httpResponse.addHeader("Retry-After", consumptionProbe.nanosToWaitForRefill.div(1_000_000_000.0).toString())
                        asyncEmailService.sendEmailToDeveloper("Too many requests $ipAddress") // For analytics purposes and preventing abuse

                    }
                }
            }
        } catch (exception: Exception) {
            asyncEmailService.sendEmailToDeveloper("Your server has experienced an exception.\n ${exception.message}")
        }
    }

}