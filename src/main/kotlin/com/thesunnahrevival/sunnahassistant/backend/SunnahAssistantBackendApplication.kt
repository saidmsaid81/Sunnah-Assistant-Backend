package com.thesunnahrevival.sunnahassistant.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SunnahAssistantBackendApplication

fun main(args: Array<String>) {
	runApplication<SunnahAssistantBackendApplication>(*args)
	Runtime.getRuntime().addShutdownHook(Thread {
		KtorClient.get().close()
	})
}
