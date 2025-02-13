package com.stableform.pages

import com.stableform.datastar.sdk.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.sse.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.File

class MainRouter(
    val pageServiceManager: PageServiceManager,
    private val fileManager: FileManager,
    private val chatService: ChatService
) {
    fun routes(app: Application) = app.routing {

        get("/") {
            val service = pageServiceManager.getPageService(sessionID().id)
            call.respondHtml {
                mainPage(service.mainState)
            }
        }
        sse("/updates") {
            createHeartbeat()
            val id = call.sessions.get<SessionID>()?.id ?: throw Exception("Session ID not found")
            val dataStar = ServerSentEventGenerator(KtorResponseAdapter(this))
            val service = pageServiceManager.getPageService(id)
            service.mainStateFlow.collect {
                dataStar.mergeFragments(
                    MergeFragmentsOptions(
                        fragments = fragment {
                            mainComponent(it)
                        }
                    ))
            }
        }
        post("/file") {
            val parameters = call.receiveMultipart()
            val service = pageService()
            parameters.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val uploadedFile = fileManager.processFileUpload(part)
                    service.updateMainState { prevState ->
                        prevState.copy(
                            files = prevState.files + uploadedFile.filePath,
                            view = "preview"
                        )
                    }
                }
                part.dispose()
            }
            call.respond(HttpStatusCode.OK)
        }
        post("/remove-file") {
            val service = pageService()
            val filePath = call.receiveParameters()["filePath"]
            if (filePath != null) {
                service.updateMainState {
                    it.copy(files = it.files - filePath)
                }
            }
            call.respond(HttpStatusCode.OK)
        }
        post("/add-more") {
            val service = pageService()
            service.updateMainState {
                it.copy(view = "upload")
            }
            call.respond(HttpStatusCode.OK)
        }
        delete("/files") {
            val service = pageService()
            val sessionId = call.sessions.get<SessionID>()?.id ?: throw Exception("Session ID not found")
            val currentFiles = service.mainState.files
            
            // Delete all files from storage
            currentFiles.forEach { filePath ->
                fileManager.deleteFile(File(filePath).name)
            }
            
            // Clear the chat memory
            chatService.clearMemory(sessionId)
            
            // Clear the state
            service.updateMainState {
                MainState()
            }
            call.respond(HttpStatusCode.OK)
        }

        post("/start-chat") {
            val service = pageService()
            service.updateMainState {
                it.copy(view = "chat")
            }
            call.respond(HttpStatusCode.OK)
        }

        post("/back-to-file") {
            val service = pageService()
            service.updateMainState {
                it.copy(view = "preview")
            }
            call.respond(HttpStatusCode.OK)
        }

        post("/chat") {
            val service = pageService()
            val sessionId = call.sessions.get<SessionID>()?.id ?: throw Exception("Session ID not found")
            val message = call.receiveParameters()["message"] ?: ""

            // Set processing state to true
            service.updateMainState {
                it.copy(
                    messages = it.messages + ChatMessage(message, "user"),
                    isProcessing = true
                )
            }

            // Create an empty assistant message that we'll update as we receive chunks
            service.updateMainState {
                it.copy(messages = it.messages + ChatMessage("", "assistant"))
            }

            // Get streaming response from OpenAI
            chatService.chatStream(sessionId, message, service.mainState.files).collect { chunk ->
                // Update the last message with the accumulated content
                service.updateMainState { state ->
                    val messages = state.messages.toMutableList()
                    val lastMessage = messages.last()
                    messages[messages.lastIndex] = lastMessage.copy(
                        content = lastMessage.content + chunk
                    )
                    state.copy(messages = messages)
                }
            }

            // Set processing state to false when complete
            service.updateMainState {
                it.copy(isProcessing = false)
            }

            call.respond(HttpStatusCode.OK)
        }

        // Add new route to list uploaded files
        get("/files") {
            val files = fileManager.getUploadedFiles()
            call.respond(files)
        }

        post("/preview-file") {
            val filePath = call.receiveParameters()["filePath"]
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    try {
                        val content = file.readText().take(10000) // Limit preview to first 10K characters
                        val preview = buildString {
                            append("File: ${file.name}\n")
                            append("Size: ${formatFileSize(file.length())}\n")
                            append("Last Modified: ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss").format(file.lastModified())}\n")
                            append("\nContent Preview:\n")
                            append("----------------------------------------\n\n")
                            append(content)
                            if (content.length == 10000) {
                                append("\n\n... (file truncated, showing first 10,000 characters)")
                            }
                        }
                        call.respondText(preview)
                    } catch (e: Exception) {
                        call.respondText("Error reading file: ${e.message}", status = HttpStatusCode.InternalServerError)
                    }
                } else {
                    call.respondText("File not found", status = HttpStatusCode.NotFound)
                }
            } else {
                call.respondText("No file path provided", status = HttpStatusCode.BadRequest)
            }
        }
    }

    fun RoutingContext.pageService(): PageService {
        val id = call.sessions.get<SessionID>()?.id ?: throw Exception("Session ID not found")
        return pageServiceManager.getPageService(id)
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.2f %s".format(size, units[unitIndex])
    }
}
