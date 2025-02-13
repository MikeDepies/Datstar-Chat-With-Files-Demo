package com.stableform

import com.stableform.pages.ChatService
import com.stableform.pages.FileManager
import com.stableform.pages.MainRouter
import com.stableform.pages.PageServiceManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(module {
            single {
                ChatService(TODO("PUT API KEY HERE"))
            }
            singleOf(::FileManager)
            singleOf(::PageServiceManager)
            singleOf(::MainRouter)
        })
    }
}
