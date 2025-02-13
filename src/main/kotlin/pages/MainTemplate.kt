package com.stableform.pages

import com.stableform.datastar.sdk.*

import kotlinx.html.*
import java.io.File

fun HTML.mainPage(mainState: MainState) {
    head {
        title {
            +"StableForm"
        }
        script { type = "module"; src = DataStar.SCRIPT_IMPORT }
        script { src = "https://cdn.tailwindcss.com" }
        script {
            unsafe {
                +"""
                tailwind.config = {
                    theme: {
                        extend: {
                            typography: {
                                DEFAULT: {
                                    css: {
                                        maxWidth: 'none',
                                        code: {
                                            backgroundColor: '#f0f0f0',
                                            padding: '0.2em 0.4em',
                                            borderRadius: '3px',
                                            fontSize: '85%'
                                        },
                                        'code::before': {
                                            content: '""'
                                        },
                                        'code::after': {
                                            content: '""'
                                        },
                                        pre: {
                                            backgroundColor: '#f6f8fa',
                                            padding: '1em',
                                            borderRadius: '6px',
                                            fontSize: '85%'
                                        }
                                    }
                                }
                            }
                        }
                    },
                    plugins: [
                        window.tailwindcss.typography
                    ]
                }
                """
            }
        }
        script { src = "https://cdn.tailwindcss.com?plugins=typography" }
        script {
            unsafe {
                +"""
                window.showFilePreview = function(filePath) {
                    const modal = document.getElementById('filePreviewModal');
                    const filePathInput = document.getElementById('previewFilePath');
                    const contentDiv = document.getElementById('filePreviewContent');
                    const preContent = contentDiv.querySelector('pre');
                    
                    // Show loading state
                    preContent.textContent = 'Loading...';
                    modal.classList.add('show');
                    filePathInput.value = filePath;
                    
                    // Submit the form to get file content
                    fetch('/preview-file', {
                        method: 'POST',
                        body: new URLSearchParams({filePath: filePath})
                    })
                    .then(response => response.text())
                    .then(content => {
                        preContent.textContent = content;
                    })
                    .catch(error => {
                        preContent.textContent = 'Error loading file: ' + error.message;
                    });
                };
                
                window.hideFilePreview = function() {
                    const modal = document.getElementById('filePreviewModal');
                    modal.classList.remove('show');
                    
                    // Clear the content
                    const contentDiv = document.getElementById('filePreviewContent');
                    const preContent = contentDiv.querySelector('pre');
                    preContent.textContent = '';
                };
                
                // Close modal when clicking outside
                document.addEventListener('DOMContentLoaded', function() {
                    const modal = document.getElementById('filePreviewModal');
                    modal.addEventListener('click', function(event) {
                        if (event.target === modal) {
                            hideFilePreview();
                        }
                    });
                });
                
                window.sendChatMessage = function(message) {
                    const form = document.getElementById('chatForm');
                    const input = form.querySelector('input[name="message"]');
                    input.value = message;
                    form.requestSubmit();
                };
                """
            }
        }
        style {
            +"""
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
            * { font-family: 'Inter', sans-serif; }
            .modal {
                display: none;
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                z-index: 50;
            }
            .modal.show {
                display: flex;
            }
            pre {
                white-space: pre-wrap;
                word-wrap: break-word;
                max-width: 100%;
            }
            """
        }
    }
    body {
        attributes["data-on-load"] = getAction("/updates")
        div {
            mainComponent(mainState)
        }
        
        // File Preview Modal
        div {
            id = "filePreviewModal"
            classes = setOf("modal")
            
            div {
                classes = setOf(
                    "bg-white", "w-full", "max-w-4xl", "mx-auto", "my-8",
                    "rounded-xl", "shadow-xl", "overflow-hidden",
                    "flex", "flex-col", "max-h-[90vh]"
                )
                
                // Modal Header
                div {
                    classes = setOf(
                        "flex", "justify-between", "items-center",
                        "px-6", "py-4", "border-b", "border-gray-200",
                        "bg-gray-50"
                    )
                    h3 {
                        classes = setOf("text-lg", "font-semibold", "text-gray-900")
                        +"File Preview"
                    }
                    button {
                        type = ButtonType.button
                        onClick = "hideFilePreview()"
                        classes = setOf(
                            "text-gray-400", "hover:text-gray-600",
                            "p-2", "rounded-full",
                            "hover:bg-gray-100", "transition-colors"
                        )
                        +"×"
                    }
                }
                
                // Modal Body
                form {
                    id = "filePreviewForm"
                    attributes["data-on-submit__prevent"] = postAction("/preview-file", HttpActionOptions.form)
                    input {
                        id = "previewFilePath"
                        type = InputType.hidden
                        name = "filePath"
                    }
                }
                
                div {
                    id = "filePreviewContent"
                    classes = setOf(
                        "flex-1", "overflow-auto", "p-6",
                        "bg-gray-50"
                    )
                    pre {
                        classes = setOf(
                            "whitespace-pre-wrap", "font-mono",
                            "text-sm", "text-gray-800"
                        )
                    }
                }
            }
        }
    }
}

fun DIV.fileCard(filePath: String, showPreview: Boolean = true) {
    div {
        classes = setOf(
            "bg-white", "p-6", "rounded-xl",
            "border", "border-gray-200",
            "shadow-sm",
            "hover:shadow-md", "transition-shadow", "duration-200"
        )
        
        // File Header
        div {
            classes = setOf("flex", "justify-between", "items-start", "mb-4")
            div {
                h3 {
                    classes = setOf(
                        "text-lg", "font-semibold", "text-gray-900",
                        if (showPreview) "cursor-pointer hover:text-blue-600" else ""
                    )
                    if (showPreview) {
                        onClick = "showFilePreview('$filePath')"
                    }
                    +File(filePath).name
                }
                p {
                    classes = setOf("text-sm", "text-gray-500", "mt-1")
                    +formatFileSize(File(filePath).length())
                }
            }
            
            // Remove File Button
            form {
                attributes["data-on-submit__prevent"] = postAction("/remove-file", HttpActionOptions.form)
                input {
                    type = InputType.hidden
                    name = "filePath"
                    value = filePath
                }
                button {
                    type = ButtonType.submit
                    classes = setOf(
                        "text-red-500", "hover:text-red-700",
                        "p-1", "rounded-full",
                        "hover:bg-red-50", "transition-colors"
                    )
                    +"×"
                }
            }
        }
        
        // File Details
        div {
            classes = setOf("text-sm", "text-gray-600", "mb-4")
            +"Location: "
            span {
                classes = setOf("font-medium", "break-all")
                +filePath
            }
        }
    }
}

fun DIV.fileInfoComponent(files: List<String>) {
    div {
        classes = setOf("max-w-6xl", "mx-auto", "px-4")

        div {
            classes = setOf("text-center", "mb-8")
            h1 {
                classes = setOf("text-3xl", "font-bold", "text-gray-900", "mb-2")
                +"Selected Files"
            }
            p {
                classes = setOf("text-gray-600")
                +"View and manage your selected files below"
            }
        }

        // Files Grid
        div {
            classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-6")
            
            files.forEach { filePath ->
                fileCard(filePath)
            }
        }

        // Actions
        div {
            classes = setOf(
                "flex", "justify-center", "space-x-4",
                "mt-8", "pt-6", "border-t", "border-gray-200"
            )
            
            // Start Chat button (only if files are selected)
            if (files.isNotEmpty()) {
                form {
                    attributes["data-on-submit__prevent"] = postAction("/start-chat", HttpActionOptions.form)
                    button {
                        type = ButtonType.submit
                        classes = setOf(
                            "bg-green-500", "text-white",
                            "px-6", "py-3", "rounded-xl",
                            "hover:bg-green-600",
                            "transition-colors", "duration-200",
                            "font-medium",
                            "shadow-sm", "hover:shadow"
                        )
                        +"💬 Chat with Files"
                    }
                }
            }
            
            // Upload More Files button
            form {
                attributes["data-on-submit__prevent"] = postAction("/add-more", HttpActionOptions.form)
                button {
                    type = ButtonType.submit
                    classes = setOf(
                        "bg-blue-500", "text-white",
                        "px-6", "py-3", "rounded-xl",
                        "hover:bg-blue-600",
                        "transition-colors", "duration-200",
                        "font-medium",
                        "shadow-sm", "hover:shadow"
                    )
                    +"⬆️ Upload More Files"
                }
            }
            
            // Clear All button (only if files are selected)
            if (files.isNotEmpty()) {
                form {
                    attributes["data-on-submit__prevent"] = deleteAction("/files", HttpActionOptions.form)
                    button {
                        type = ButtonType.submit
                        classes = setOf(
                            "bg-red-500", "text-white",
                            "px-6", "py-3", "rounded-xl",
                            "hover:bg-red-600",
                            "transition-colors", "duration-200",
                            "font-medium",
                            "shadow-sm", "hover:shadow"
                        )
                        +"🗑️ Clear All"
                    }
                }
            }
        }
    }
}

fun DIV.chatComponent(mainState: MainState) {
    div {
        classes = setOf("w-full", "max-w-6xl", "mx-auto", "h-full", "px-4")

        // Header with file info
        div {
            classes = setOf("flex", "justify-between", "items-center", "mb-6", "w-full")
            div {
                h1 {
                    classes = setOf("text-3xl", "font-bold", "text-gray-900", "mb-2")
                    +"Chat with Files"
                }
                p {
                    classes = setOf("text-sm", "text-gray-600", "flex", "items-center", "flex-wrap", "gap-2")
                    +"Discussing: "
                    mainState.files.forEach { filePath ->
                        span {
                            classes = setOf(
                                "inline-flex", "items-center",
                                "px-2", "py-1",
                                "bg-gray-100", "rounded-md",
                                "text-gray-900", "font-medium",
                                "cursor-pointer", "hover:bg-gray-200",
                                "transition-colors"
                            )
                            onClick = "showFilePreview('$filePath')"
                            +File(filePath).name
                        }
                    }
                }
            }
            
            // Back to Files button
            form {
                attributes["data-on-submit__prevent"] = postAction("/back-to-file", HttpActionOptions.form)
                button {
                    type = ButtonType.submit
                    classes = setOf(
                        "text-gray-600", "hover:text-gray-900",
                        "px-4", "py-2", "rounded-lg",
                        "flex", "items-center", "space-x-2",
                        "border", "border-gray-200",
                        "hover:bg-gray-50", "transition-all", "duration-200"
                    )
                    +"← Back to Files"
                }
            }
        }

        // Chat Messages
        div {
            classes = setOf(
                "flex-1", "overflow-y-auto", "p-6", "space-y-8", "w-full",
                "bg-gradient-to-b", "from-gray-50", "to-white"
            )
            
            mainState.messages.forEach { message ->
                div {
                    classes = setOf(
                        "flex", "w-full",
                        if (message.role == "user") "justify-end" else "justify-start"
                    )
                    
                    // Add scroll-into-view to the last message
                    if (message == mainState.messages.last()) {
                        attributes["data-scroll-into-view__smooth__vend"] = ""
                    }
                    
                    div {
                        classes = setOf(
                            "max-w-[70%]", "w-fit",
                            "rounded-2xl",
                            "px-6", "py-4",
                            "shadow-sm",
                            if (message.role == "user") {
                                "bg-gradient-to-br from-blue-500 to-blue-600 text-white"
                            } else {
                                "bg-white border border-gray-200"
                            }
                        )
                        // Add role indicator
                        div {
                            classes = setOf(
                                "text-xs", "mb-2",
                                if (message.role == "user") "text-blue-100" else "text-gray-400"
                            )
                            +(if (message.role == "user") "You" else "AI Assistant")
                        }
                        div {
                            classes = setOf(
                                "markdown-rendered", "prose", "prose-sm", "max-w-none",
                                if (message.role == "user") {
                                    "prose-invert prose-p:text-white/90 prose-pre:bg-blue-400/20 prose-pre:text-white"
                                } else {
                                    "prose-gray prose-pre:bg-gray-50"
                                }
                            )
                            unsafe {
                                +MarkdownUtil.markdownToHtml(message.content)
                            }
                        }
                        // Add timestamp (you'll need to add this to your ChatMessage class)
                        div {
                            classes = setOf(
                                "text-xs", "mt-2", "text-right",
                                if (message.role == "user") "text-blue-100" else "text-gray-400"
                            )
                            +"Just now" // Replace with actual timestamp when available
                        }
                    }
                }
            }

            // Update welcome message styling
            if (mainState.messages.isEmpty()) {
                div {
                    classes = setOf(
                        "w-full", "flex", "flex-col", "items-center", 
                        "justify-center", "mt-12", "px-4"
                    )
                    div {
                        classes = setOf(
                            "text-6xl", "mb-6",
                            "bg-gradient-to-br", "from-blue-500", "to-blue-600",
                            "rounded-full", "p-6", "shadow-lg"
                        )
                        +"💭"
                    }
                    p {
                        classes = setOf(
                            "text-2xl", "font-bold", "text-gray-900",
                            "mb-3", "text-center",
                            "bg-gradient-to-r", "from-blue-600", "to-blue-800",
                            "bg-clip-text", "text-transparent"
                        )
                        +"Ask any questions about your files!"
                    }
                    p {
                        classes = setOf(
                            "text-base", "text-gray-600", "mb-8",
                            "text-center", "max-w-lg"
                        )
                        +"Get insights and understanding about your documents. I'm here to help analyze and explain the content."
                    }
                    div {
                        classes = setOf(
                            "w-full", "max-w-md", "mx-auto",
                            "bg-white", "rounded-xl", "p-6",
                            "border", "border-gray-200",
                            "shadow-lg"
                        )
                        p {
                            classes = setOf(
                                "text-sm", "font-semibold", "text-gray-900",
                                "mb-4", "flex", "items-center", "gap-2"
                            )
                            span {
                                classes = setOf(
                                    "bg-blue-100", "text-blue-600",
                                    "px-2", "py-1", "rounded-md",
                                    "text-xs", "uppercase", "tracking-wide"
                                )
                                +"Suggestions"
                            }
                            +"Try asking:"
                        }
                        ul {
                            classes = setOf("space-y-3")
                            listOf(
                                "What are these files about?",
                                "Can you summarize the key points?",
                                "What are the main topics discussed?",
                                "Compare the content between files",
                                "Find specific information about..."
                            ).forEach { suggestion ->
                                li {
                                    classes = setOf(
                                        "flex", "items-center", "gap-3",
                                        "p-3", "rounded-lg",
                                        "hover:bg-blue-50", "cursor-pointer",
                                        "transition-all", "duration-200",
                                        "group"
                                    )
                                    onClick = "sendChatMessage('$suggestion')"
                                    span {
                                        classes = setOf(
                                            "text-blue-500",
                                            "group-hover:scale-110",
                                            "transition-transform"
                                        )
                                        +"💬"
                                    }
                                    span {
                                        classes = setOf(
                                            "text-gray-600",
                                            "group-hover:text-blue-600"
                                        )
                                        +suggestion
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Update input form styling
        form {
            id = "chatForm"
            classes = setOf(
                "border-t", "border-gray-200",
                "p-4", "bg-white", "w-full",
                "sticky", "bottom-0"
            )
            attributes["data-on-submit__prevent"] = postAction("/chat", HttpActionOptions.form)

            div {
                classes = setOf(
                    "flex", "space-x-4", "max-w-4xl",
                    "mx-auto", "w-full", "items-end"
                )
                input {
                    type = InputType.text
                    name = "message"
                    placeholder = if (mainState.isProcessing) "AI is thinking..." else "Type your message..."
                    disabled = mainState.isProcessing
                    classes = setOf(
                        "flex-1",
                        "rounded-xl",
                        "border", "border-gray-300",
                        "px-6", "py-4",
                        "bg-white",
                        "text-gray-900", "placeholder-gray-400",
                        "focus:outline-none", "focus:border-blue-500",
                        "focus:ring-2", "focus:ring-blue-500/20",
                        "shadow-sm",
                        "text-base",
                        if (mainState.isProcessing) "cursor-not-allowed bg-gray-50" else ""
                    )
                }
                button {
                    type = ButtonType.submit
                    disabled = mainState.isProcessing
                    classes = setOf(
                        "px-6", "py-4",
                        "rounded-xl",
                        "transition-all", "duration-200",
                        "font-medium",
                        "shadow-sm",
                        "hover:shadow-md",
                        "hover:translate-y-[-1px]",
                        "active:translate-y-[1px]",
                        "flex", "items-center", "space-x-2",
                        if (mainState.isProcessing) {
                            "bg-gray-400 cursor-not-allowed text-white"
                        } else {
                            "bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white"
                        }
                    )
                    if (mainState.isProcessing) {
                        // Add a loading spinner
                        div {
                            classes = setOf(
                                "animate-spin",
                                "rounded-full",
                                "h-5",
                                "w-5",
                                "border-3",
                                "border-white/30",
                                "border-t-white",
                                "mr-2"
                            )
                        }
                        +"Processing..."
                    } else {
                        +"Send Message"
                    }
                }
            }
        }
    }
}

fun DIV.fileUploadForm(isAdditionalUpload: Boolean = false) {
    div {
        classes = setOf("max-w-4xl", "mx-auto", "w-full")

        // Hero Section
        div {
            classes = setOf("text-center", "mb-12")
            div {
                classes = setOf(
                    "inline-flex", "items-center", "justify-center",
                    "w-20", "h-20", "mb-6",
                    "rounded-2xl",
                    "bg-gradient-to-br", "from-blue-500", "to-blue-600",
                    "shadow-lg"
                )
                span {
                    classes = setOf("text-4xl")
                    +"📄"
                }
            }
            h1 {
                classes = setOf(
                    "text-4xl", "font-bold", "mb-4",
                    "bg-gradient-to-r", "from-blue-600", "to-blue-800",
                    "bg-clip-text", "text-transparent"
                )
                +if (isAdditionalUpload) "Add More Files" else "Welcome to StableForm"
            }
            p {
                classes = setOf("text-lg", "text-gray-600", "max-w-2xl", "mx-auto")
                +if (isAdditionalUpload) 
                    "Expand your analysis by adding more files to the conversation."
                else 
                    "Upload your files and start an intelligent conversation about their contents. Get insights, summaries, and answers to your questions."
            }
        }

        // Upload Form
        form {
            attributes["data-on-submit__prevent"] = postAction("/file", HttpActionOptions.form)

            div {
                classes = setOf("space-y-8")

                // Upload Area
                div {
                    classes = setOf(
                        "relative",
                        "group"
                    )
                    label {
                        classes = setOf(
                            "flex", "flex-col", "items-center", "justify-center",
                            "w-full", "min-h-[300px]",
                            "border-2", "border-blue-200", "border-dashed", "rounded-2xl",
                            "bg-blue-50/50",
                            "cursor-pointer",
                            "transition-all", "duration-200",
                            "group-hover:border-blue-300",
                            "group-hover:bg-blue-50"
                        )

                        input {
                            type = InputType.file
                            name = "file"
                            classes = setOf("hidden")
                            multiple = true
                        }

                        div {
                            classes = setOf(
                                "flex", "flex-col", "items-center", "justify-center",
                                "p-8", "text-center", "space-y-4"
                            )
                            
                            // Upload Icon
                            div {
                                classes = setOf(
                                    "w-16", "h-16",
                                    "flex", "items-center", "justify-center",
                                    "rounded-full",
                                    "bg-blue-100",
                                    "text-blue-600",
                                    "mb-4",
                                    "group-hover:scale-110",
                                    "transition-transform", "duration-200"
                                )
                                +"⬆️"
                            }

                            div {
                                classes = setOf("space-y-2")
                                p {
                                    classes = setOf("text-lg", "font-medium", "text-gray-700")
                                    +"Drop your files here or "
                                    span {
                                        classes = setOf("text-blue-600", "group-hover:text-blue-700")
                                        +"browse"
                                    }
                                }
                                p {
                                    classes = setOf("text-sm", "text-gray-500")
                                    +"Support for multiple files"
                                }
                            }
                        }

                        // Supported File Types
                        div {
                            classes = setOf(
                                "absolute", "bottom-4", "left-0", "right-0",
                                "flex", "justify-center", "gap-2",
                                "text-xs", "text-gray-500"
                            )
                            listOf("TXT", "PDF", "DOC", "DOCX", "MD").forEach { format ->
                                span {
                                    classes = setOf(
                                        "px-2", "py-1",
                                        "rounded-md",
                                        "bg-white/80",
                                        "border", "border-gray-200"
                                    )
                                    +format
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                div {
                    classes = setOf(
                        "flex", "items-center", "justify-center",
                        "gap-4", "pt-4"
                    )
                    button {
                        type = ButtonType.submit
                        classes = setOf(
                            "px-8", "py-4",
                            "rounded-xl",
                            "font-medium",
                            "text-white",
                            "bg-gradient-to-r", "from-blue-500", "to-blue-600",
                            "hover:from-blue-600", "hover:to-blue-700",
                            "shadow-sm", "hover:shadow-md",
                            "transition-all", "duration-200",
                            "hover:translate-y-[-1px]",
                            "active:translate-y-[1px]",
                            "flex", "items-center", "gap-2"
                        )
                        span { +"⬆️" }
                        +if (isAdditionalUpload) "Add Files" else "Upload Files"
                    }
                    
                    if (isAdditionalUpload) {
                        button {
                            type = ButtonType.button
                            onClick = "window.history.back()"
                            classes = setOf(
                                "px-6", "py-4",
                                "rounded-xl",
                                "font-medium",
                                "text-gray-700",
                                "bg-gray-100",
                                "hover:bg-gray-200",
                                "transition-colors", "duration-200"
                            )
                            +"Cancel"
                        }
                    }
                }

                // Features Section
                if (!isAdditionalUpload) {
                    div {
                        classes = setOf(
                            "grid", "grid-cols-1", "md:grid-cols-3",
                            "gap-8", "mt-16", "pt-16",
                            "border-t", "border-gray-200"
                        )
                        
                        // Feature 1
                        div {
                            classes = setOf("text-center", "space-y-3")
                            div {
                                classes = setOf(
                                    "w-12", "h-12",
                                    "mx-auto",
                                    "flex", "items-center", "justify-center",
                                    "rounded-xl",
                                    "bg-blue-100",
                                    "text-blue-600"
                                )
                                +"🔍"
                            }
                            h3 {
                                classes = setOf("font-semibold", "text-gray-900")
                                +"Smart Analysis"
                            }
                            p {
                                classes = setOf("text-sm", "text-gray-600")
                                +"Get instant insights and understanding from your documents"
                            }
                        }
                        
                        // Feature 2
                        div {
                            classes = setOf("text-center", "space-y-3")
                            div {
                                classes = setOf(
                                    "w-12", "h-12",
                                    "mx-auto",
                                    "flex", "items-center", "justify-center",
                                    "rounded-xl",
                                    "bg-blue-100",
                                    "text-blue-600"
                                )
                                +"💬"
                            }
                            h3 {
                                classes = setOf("font-semibold", "text-gray-900")
                                +"Interactive Chat"
                            }
                            p {
                                classes = setOf("text-sm", "text-gray-600")
                                +"Ask questions and get detailed answers about your files"
                            }
                        }
                        
                        // Feature 3
                        div {
                            classes = setOf("text-center", "space-y-3")
                            div {
                                classes = setOf(
                                    "w-12", "h-12",
                                    "mx-auto",
                                    "flex", "items-center", "justify-center",
                                    "rounded-xl",
                                    "bg-blue-100",
                                    "text-blue-600"
                                )
                                +"🚀"
                            }
                            h3 {
                                classes = setOf("font-semibold", "text-gray-900")
                                +"Fast & Secure"
                            }
                            p {
                                classes = setOf("text-sm", "text-gray-600")
                                +"Quick processing with secure file handling"
                            }
                        }
                    }
                }
            }
        }
    }
}

fun DIV.mainComponent(mainState: MainState) {
    div {
        id = "main"
        classes = setOf(
            "min-h-screen", "bg-gray-100", "py-6", "flex", "flex-col",
            if (mainState.view == "chat") "px-0" else "px-4",
            "sm:py-8"
        )
        
        when (mainState.view) {
            "chat" -> chatComponent(mainState)
            "upload" -> div {
                classes = setOf("relative", "py-3", "w-full", "max-w-6xl", "mx-auto")
                div {
                    classes = setOf(
                        "relative", "px-4", "py-8",
                        "bg-white", "shadow", "rounded-3xl",
                        "sm:p-8", "mx-auto", "w-full"
                    )
                    div {
                        classes = setOf("max-w-3xl", "mx-auto")

                        div {
                            classes = setOf("text-center", "mb-8")
                            h1 {
                                classes = setOf("text-2xl", "font-semibold", "text-gray-900")
                                +if (mainState.files.isEmpty()) "File Upload" else "Upload More Files"
                            }
                            p {
                                classes = setOf("mt-2", "text-gray-600")
                                +if (mainState.files.isEmpty()) 
                                    "Please select files to upload" 
                                else 
                                    "Add more files to your selection"
                            }
                        }

                        div {
                            classes = setOf("mt-8")
                            fileUploadForm(isAdditionalUpload = mainState.files.isNotEmpty())
                        }
                    }
                }
            }
            else -> div {
                classes = setOf("relative", "py-3", "w-full", "max-w-6xl", "mx-auto")
                div {
                    classes = setOf(
                        "relative", "px-4", "py-8",
                        "bg-white", "shadow", "rounded-3xl",
                        "sm:p-8", "mx-auto", "w-full"
                    )
                    if (mainState.files.isNotEmpty()) {
                        fileInfoComponent(mainState.files)
                    }
                }
            }
        }
    }
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