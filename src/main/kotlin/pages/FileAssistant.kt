package com.stableform.pages

import dev.langchain4j.service.*

interface FileAssistant {
    @SystemMessage("""
        You are a helpful assistant analyzing files and documents. You have access to the following files:
        
        {{files}}
        
        Help the user understand these files and answer questions about them. When referring to file content, be specific 
        about which file you're discussing. If a user asks about content that isn't in the provided files, let them know 
        that information isn't available in the current files.
    """)
    fun chat(@MemoryId sessionId: String, @V("files") files: String, @UserMessage message: String): TokenStream
} 