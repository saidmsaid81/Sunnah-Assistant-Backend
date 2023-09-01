package com.thesunnahrevival.sunnahassistant.backend

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfiguration {

    @Bean
    fun init(ktorClient: KtorClient) = ApplicationRunner {
        Runtime.getRuntime().addShutdownHook(Thread {
            ktorClient.getKtorClient().close()
        })
    }
}