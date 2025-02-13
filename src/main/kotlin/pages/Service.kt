package com.stableform.pages

import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ChatMessage(
    val content : String, val role: String
)

@Serializable
data class MainState(
    val files: List<String> = emptyList(),
    val view: String = "upload", // Can be "upload", "preview", or "chat"
    val messages: List<ChatMessage> = emptyList(),
    val isProcessing: Boolean = false
)

class PageServiceManager {
    val sessionMap: MutableMap<String, PageService> = mutableMapOf()
    fun getPageService(id: String): PageService {
        return sessionMap[id] ?: PageService().also { sessionMap[id] = it }
    }
}

data class ServerState<T>(val state: T, val flow: MutableSharedFlow<T>)

class PageService(
    var mainSessionState: ServerState<MainState> = ServerState(MainState(), MutableSharedFlow())
) {
    suspend fun updateMainState(block: (MainState) -> MainState) {
        val newState = block(mainSessionState.state)
        mainSessionState = mainSessionState.copy(state = newState)
        mainSessionState.flow.emit(newState)
    }

    val mainState get() = mainSessionState.state
    val mainStateFlow: Flow<MainState> get() = mainSessionState.flow
}

@Serializable
data class SessionID(val id: String = UUID.randomUUID().toString())

fun RoutingContext.sessionID() = call.sessions.get<SessionID>() ?: SessionID().also {
    call.sessions.set(it)
    println(call.sessions.get<SessionID>())
}