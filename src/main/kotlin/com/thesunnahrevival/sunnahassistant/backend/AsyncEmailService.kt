package com.thesunnahrevival.sunnahassistant.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class AsyncEmailService(private val emailService: EmailService) {
    
    fun sendEmailToDeveloper(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            emailService.sendEmailToDeveloper(message)
        }
    }
}
