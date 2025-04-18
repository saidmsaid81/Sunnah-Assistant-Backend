package com.thesunnahrevival.sunnahassistant.backend

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.stereotype.Service
import org.springframework.mail.javamail.JavaMailSender

@Service
class EmailService {

    @Autowired
    private lateinit var mailSender: JavaMailSender

    @Value("\${MY_EMAIL}")
    private lateinit var myEmail: String

    fun sendEmailToDeveloper(message: String) {
        val simpleMessage = SimpleMailMessage()
        simpleMessage.from = "Sunnah Assistant Backend <$myEmail>"
        simpleMessage.setTo(myEmail)
        simpleMessage.subject = "Sunnah Assistant Api Failure"
        simpleMessage.text = message

        mailSender.send(simpleMessage)
    }
}