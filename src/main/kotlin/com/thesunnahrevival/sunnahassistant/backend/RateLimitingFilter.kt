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
        val ip = httpRequest.remoteAddr
        val bucket = buckets.get(ip)
        val consumptionProbe = bucket.tryConsumeAndReturnRemaining(1)
        if (consumptionProbe.isConsumed) {
            chain.doFilter(request, response)
        } else {
            val httpResponse = response as HttpServletResponse
            httpResponse.status = HttpStatusCode.TooManyRequests.value
            httpResponse.addHeader("Retry-After", consumptionProbe.nanosToWaitForRefill.div(1_000_000_000.0).toString())

            //For analytics purposes
            CoroutineScope(Dispatchers.IO).launch {
                ktorClient.sendEmailToDeveloper(domainName, senderEmail, myEmail, "Too many requests")
            }
        }
    }
}