package com.stableform

import com.stableform.pages.MainRouter
import io.ktor.server.application.*
import io.ktor.server.sse.*
import org.koin.ktor.ext.inject

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureFrameworks()
    configureSecurity()
    val mainRouter : MainRouter by inject()
    install(SSE)
    mainRouter.routes(this)
}
