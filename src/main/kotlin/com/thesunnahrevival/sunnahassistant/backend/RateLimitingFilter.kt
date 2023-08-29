package com.thesunnahrevival.sunnahassistant.backend

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.*
import io.ktor.http.*
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class RateLimitingFilter : Filter {

    private val buckets = Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS) // expire after 1 hour of inactivity
        .build<String, Bucket> { createBucket() }

    private fun createBucket(): Bucket {
        val limit = Bandwidth.classic(1, Refill.greedy(10, Duration.ofHours(1)))
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
        }
    }
}