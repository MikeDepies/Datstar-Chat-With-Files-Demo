package com.stableform.pages

import io.ktor.http.content.*
import java.io.File
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UploadedFile(
    val fileName: String,
    val filePath: String,
    val uploadTime: Long,
    val sizeBytes: Long
)

class FileManager {
    private val uploadDirectory = "data/uploads"
    private val uploadedFiles = mutableMapOf<String, UploadedFile>()

    init {
        // Create upload directory if it doesn't exist
        File(uploadDirectory).mkdirs()
        
        // Load existing files on startup
        loadExistingFiles()
    }

    private fun loadExistingFiles() {
        File(uploadDirectory).listFiles()?.forEach { file ->
            if (file.isFile) {
                uploadedFiles[file.name] = UploadedFile(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    uploadTime = file.lastModified(),
                    sizeBytes = file.length()
                )
            }
        }
    }

    suspend fun processFileUpload(fileItem: PartData.FileItem): UploadedFile {
        val fileName = fileItem.originalFileName ?: throw IllegalArgumentException("File name is required")
        val filePath = "$uploadDirectory/$fileName"
        val file = File(filePath)

        // Save the file
        fileItem.streamProvider().use { input ->
            file.outputStream().buffered().use { output ->
                input.copyTo(output)
            }
        }

        // Create and store file metadata
        val uploadedFile = UploadedFile(
            fileName = fileName,
            filePath = filePath,
            uploadTime = Instant.now().toEpochMilli(),
            sizeBytes = file.length()
        )
        uploadedFiles[fileName] = uploadedFile

        return uploadedFile
    }

    fun getUploadedFiles(): List<UploadedFile> {
        return uploadedFiles.values.toList().sortedByDescending { it.uploadTime }
    }

    fun getFile(fileName: String): UploadedFile? {
        return uploadedFiles[fileName]
    }

    fun deleteFile(fileName: String): Boolean {
        val file = uploadedFiles[fileName] ?: return false
        val success = File(file.filePath).delete()
        if (success) {
            uploadedFiles.remove(fileName)
        }
        return success
    }

    fun clearAllFiles(): Boolean {
        var success = true
        uploadedFiles.values.forEach { file ->
            if (!File(file.filePath).delete()) {
                success = false
            }
        }
        uploadedFiles.clear()
        return success
    }
} 