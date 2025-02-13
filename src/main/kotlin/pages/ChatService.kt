package com.stableform.pages

import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.service.AiServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChatService(private val apiKey: String) {
    val modelName: String = "gpt-4o-mini"
    private val streamingModel: StreamingChatLanguageModel = OpenAiStreamingChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
        .temperature(0.7)
        .build()

    private val fileAssistant: FileAssistant = AiServices.builder(FileAssistant::class.java)
        .streamingChatLanguageModel(streamingModel)
        .chatMemoryProvider { MessageWindowChatMemory.withMaxMessages(20) }
        .build()

    private val processingStates = mutableMapOf<String, Boolean>()

    fun isProcessing(sessionId: String): Boolean = processingStates[sessionId] ?: false

    fun chatStream(sessionId: String, message: String, files: List<String>): Flow<String> = channelFlow {
        processingStates[sessionId] = true
        
        try {
            val fileContents = buildString {
                files.forEach { filePath ->
                    try {
                        val file = File(filePath)
                        if (file.exists()) {
                            val content = file.readText().take(10000) // Limit each file to 10K chars
                            appendLine("File: ${file.name}")
                            appendLine("Content:")
                            appendLine(content)
                            appendLine("---")
                            appendLine()
                        }
                    } catch (e: Exception) {
                        // Skip files that can't be read
                    }
                }
            }.trim()

            if (fileContents.isEmpty()) {
                trySend("I don't see any files to analyze. Please upload some files first.")
                processingStates[sessionId] = false
                return@channelFlow
            }

            suspendCancellableCoroutine { continuation ->
                try {
                    val tokenStream = fileAssistant.chat(sessionId, fileContents, message)
                    tokenStream
                        .onPartialResponse { token -> trySend(token) }
                        .onError { error -> 
                            processingStates[sessionId] = false
                            continuation.resumeWithException(error) 
                        }
                        .onCompleteResponse { _ -> 
                            processingStates[sessionId] = false
                            continuation.resume(Unit) 
                        }
                        .start()
                } catch (e: Exception) {
                    processingStates[sessionId] = false
                    continuation.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            processingStates[sessionId] = false
            throw e
        }
    }

    fun clearMemory(sessionId: String) {
        // Memory is handled automatically by the AI Service
        processingStates.remove(sessionId)
    }
} 