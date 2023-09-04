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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

private const val userAgentHeader = "User-Agent"

private const val appVersionHeader = "app-version"

private const val currentAppVersion = 15

@Component
class RateLimitingFilter(private val ktorClient: KtorClient) : Filter {

    @Value("\${DOMAIN_NAME}")
    private lateinit var domainName: String

    @Value("\${SENDER_EMAIL}")
    private lateinit var senderEmail: String

    @Value("\${MY_EMAIL}")
    private lateinit var myEmail: String

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
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val userAgentHeader = httpRequest.getHeader(userAgentHeader)
        val appVersionHeader = httpRequest.getHeader(appVersionHeader)

        when {
            userAgentHeader == null || !userAgentHeader.matches("SunnahAssistant-Android-App".toRegex()) -> {
                response.status = HttpStatusCode.Unauthorized.value
                notifyDeveloper("Invalid User Agent: $userAgentHeader")
            }
            appVersionHeader == null -> {
                response.status = HttpStatusCode.Unauthorized.value
                notifyDeveloper("Invalid App Version")
            }
            appVersionHeader.toIntOrNull() != null && appVersionHeader.toIntOrNull()!! < currentAppVersion -> {
                response.status = HttpStatusCode.UpgradeRequired.value
            }
            else -> {
                val ip = httpRequest.remoteAddr
                val bucket = buckets.get(ip)
                val consumptionProbe = bucket.tryConsumeAndReturnRemaining(1)
                if (consumptionProbe.isConsumed) {
                    chain.doFilter(request, response)
                } else {
                    httpResponse.status = HttpStatusCode.TooManyRequests.value
                    httpResponse.addHeader("Retry-After", consumptionProbe.nanosToWaitForRefill.div(1_000_000_000.0).toString())
                    notifyDeveloper("Too many requests: $ip")
                }
            }
        }
    }

    private fun notifyDeveloper(message: String) {
        //For analytics purposes and preventing abuse
        CoroutineScope(Dispatchers.IO).launch {
            ktorClient.sendEmailToDeveloper(domainName, senderEmail, myEmail, message)
        }
    }
}