package com.stableform.datastar.sdk

import io.ktor.http.*
import io.ktor.server.sse.*
import java.net.URLDecoder

import io.ktor.server.request.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import kotlinx.serialization.json.*

object MergeMode {
    const val MORPH = "morph"             // Use idiomorph to merge the fragment into the DOM
    const val INNER = "inner"             // Replace the innerHTML of the selector with the fragment
    const val OUTER = "outer"             // Replace the outerHTML of the selector with the fragment
    const val PREPEND = "prepend"         // Prepend the fragment to the selector
    const val APPEND = "append"           // Append the fragment to the selector
    const val BEFORE = "before"           // Insert the fragment before the selector
    const val AFTER = "after"             // Insert the fragment after the selector
    const val UPSERT_ATTRIBUTES = "upsertAttributes" // Update the attributes of the selector with the fragment
}
data class MergeFragmentsOptions(

    val fragments: String,
    val selector: String? = null,
    val mergeMode: String? = null,
    val settleDuration: Long? = null,
    val useViewTransition: Boolean? = null,
    val id : String? = null
)

data class DataStarEvent(
    val eventType: String,
    val dataLines: List<String>,
    val id : String? = null
)


data class RemoveFragmentsOptions(
    val selector: String,
    val settleDuration: Long? = null,
    val useViewTransition: Boolean = false,
    val id : String? = null
)


data class MergeSignalsOptions(
    val signals: String,
    val onlyIfMissing: Boolean = false,
    val id : String? = null
)

data class RemoveSignalsOptions(
    val paths: List<String>,
    val id : String? = null
)

data class ExecuteScriptOptions(
    val script: String,
    val attributes: String? = null,
    val autoRemove: Boolean? = null,
    val id : String? = null
)

fun ExecuteScriptOptions.toDataStarEvent(): DataStarEvent {
    val dataLines = mutableListOf<String>()

    // Add autoRemove if false (default is true)
    if (autoRemove != DataStar.DEFAULT_EXECUTE_SCRIPT_AUTO_REMOVE) {
        dataLines.add(DataStar.AUTO_REMOVE_DATALINE_LITERAL + autoRemove)
    }

    // Add attributes if not default
    if (attributes != null && attributes != DataStar.DEFAULT_EXECUTE_SCRIPT_ATTRIBUTES) {
        dataLines.add(DataStar.ATTRIBUTES_DATALINE_LITERAL + attributes)
    }

    // Add script
    dataLines.add(DataStar.SCRIPT_DATALINE_LITERAL + script)

    return DataStarEvent(eventType = "datastar-execute-script", dataLines = dataLines, id = id)
}

fun MergeSignalsOptions.toDataStarEvent(): DataStarEvent {
    val dataLines = mutableListOf<String>()

    // Only add onlyIfMissing if different from default
    if (onlyIfMissing != DataStar.DEFAULT_MERGE_SIGNALS_ONLY_IF_MISSING) {
        dataLines.add(DataStar.ONLY_IF_MISSING_DATALINE_LITERAL + onlyIfMissing)
    }

    // Add signals data
    dataLines.add(DataStar.SIGNALS_DATALINE_LITERAL + signals.trim().replace("\n",""))

    return DataStarEvent(eventType = "datastar-merge-signals", dataLines = dataLines, id = id)
}


fun RemoveFragmentsOptions.toDataStarEvent(): DataStarEvent {
    val dataLines = mutableListOf<String>()

    dataLines.add(DataStar.SELECTOR_DATALINE_LITERAL + selector)
    if (settleDuration != null)
        dataLines.add(DataStar.SETTLE_DURATION_DATALINE_LITERAL + settleDuration)
    if (useViewTransition)
        dataLines.add(DataStar.USE_VIEW_TRANSITION_DATALINE_LITERAL + useViewTransition)
    return DataStarEvent(eventType = "datastar-remove-fragments", dataLines = dataLines, id = id)
}

fun RemoveSignalsOptions.toDataStarEvent(): DataStarEvent {
    val dataLines = mutableListOf<String>()
    paths.forEach { path ->
        dataLines.add(DataStar.PATHS_DATALINE_LITERAL + path)
    }
    return DataStarEvent(eventType = "datastar-remove-signals", dataLines = dataLines, id = id)
}


fun MergeFragmentsOptions.toDataStarEvent(): DataStarEvent {
    val dataLines = mutableListOf<String>()
    dataLines.add(DataStar.FRAGMENTS_DATALINE_LITERAL + fragments)
    if (selector != null) {
        dataLines.add(DataStar.SELECTOR_DATALINE_LITERAL + selector)
    }
    if (mergeMode != null) {
        dataLines.add(DataStar.MERGE_MODE_DATALINE_LITERAL + mergeMode)
    }
    if (settleDuration != null) {
        dataLines.add(DataStar.SETTLE_DURATION_DATALINE_LITERAL + settleDuration)
    }
    if (useViewTransition != null) {
        dataLines.add(DataStar.USE_VIEW_TRANSITION_DATALINE_LITERAL + useViewTransition)
    }

    fragments.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { dataLines.add(DataStar.FRAGMENTS_DATALINE_LITERAL + it) }
    return DataStarEvent(eventType = "datastar-merge-fragments", dataLines = dataLines, id = id)
}

object DataStar {
    const val DATASTAR_KEY = "datastar"
    const val VERSION = "1.0.0-beta.5"
    const val SCRIPT_IMPORT = "https://cdn.jsdelivr.net/gh/starfederation/datastar@$VERSION/bundles/datastar.js"

    // The default duration for settling during fragment merges. Allows for CSS transitions to complete.
    const val DEFAULT_FRAGMENTS_SETTLE_DURATION = 300

    // The default duration for retrying SSE on connection reset. This is part of the underlying retry mechanism of SSE.
    const val DEFAULT_SSE_RETRY_DURATION = 1000

    // Should fragments be merged using the ViewTransition API?
    const val DEFAULT_FRAGMENTS_USE_VIEW_TRANSITIONS = false

    // Should a given set of signals merge if they are missing?
    const val DEFAULT_MERGE_SIGNALS_ONLY_IF_MISSING = false

    // Should script element remove itself after execution?
    const val DEFAULT_EXECUTE_SCRIPT_AUTO_REMOVE = true

    // The default attributes for <script/> element use when executing scripts. It is a set of of key-value pairs delimited by a newline \n character.
    const val DEFAULT_EXECUTE_SCRIPT_ATTRIBUTES = "type module"

    // The mode in which a fragment is merged into the DOM.
    val DEFAULT_FRAGMENT_MERGE_MODE = MergeMode.MORPH

    // Dataline literals.
    const val SELECTOR_DATALINE_LITERAL = "selector "
    const val MERGE_MODE_DATALINE_LITERAL = "mergeMode "
    const val SETTLE_DURATION_DATALINE_LITERAL = "settleDuration "
    const val FRAGMENTS_DATALINE_LITERAL = "fragments "
    const val USE_VIEW_TRANSITION_DATALINE_LITERAL = "useViewTransition "
    const val SIGNALS_DATALINE_LITERAL = "signals "
    const val ONLY_IF_MISSING_DATALINE_LITERAL = "onlyIfMissing "
    const val PATHS_DATALINE_LITERAL = "paths "
    const val SCRIPT_DATALINE_LITERAL = "script "
    const val ATTRIBUTES_DATALINE_LITERAL = "attributes "
    const val AUTO_REMOVE_DATALINE_LITERAL = "autoRemove "
}

interface ResponseAdapter {
    suspend fun send(dataStarEvent: DataStarEvent)
}

class KtorResponseAdapter(private val serverSSESession: ServerSSESession) : ResponseAdapter {
    override suspend fun send(dataStarEvent: DataStarEvent) {
        serverSSESession.send(
            event = dataStarEvent.eventType,
            data = dataStarEvent.dataLines.joinToString("\n"),
            id = dataStarEvent.id
        )
    }

}

class ServerSentEventGenerator(private val responseAdapter: ResponseAdapter) {

    suspend fun send(dataStarEvent: DataStarEvent) {
        responseAdapter.send(dataStarEvent)
    }

    suspend fun mergeFragments(fragmentsOptions: MergeFragmentsOptions) {
        send(fragmentsOptions.toDataStarEvent())
    }

    suspend fun removeFragments(fragmentsOptions: RemoveFragmentsOptions) {
        send(fragmentsOptions.toDataStarEvent())
    }

    suspend fun mergeSignals(signalsOptions: MergeSignalsOptions) {
        send(signalsOptions.toDataStarEvent())
    }

    suspend fun removeSignals(signalsOptions: RemoveSignalsOptions) {
        send(signalsOptions.toDataStarEvent())
    }

    suspend fun executeScript(scriptOptions: ExecuteScriptOptions) {
        send(scriptOptions.toDataStarEvent())
    }

}

interface RequestAdapter {
    suspend fun readSignals(): String
}

class KtorRequestAdapter(private val request: ApplicationRequest) : RequestAdapter {
    override suspend fun readSignals(): String {
        return when (request.httpMethod) {
            HttpMethod.Get -> {
                // For GET requests, parse datastar query param as URL encoded JSON
                request.queryParameters["datastar"]?.let { URLDecoder.decode(it, "UTF-8") }
                    ?: throw IllegalArgumentException("Missing datastar query parameter")
            }

            else -> {
                // For other methods, read body as JSON string
                request.call.receiveText().also { println(it) }
            }
        }
    }
}

val json = Json {
    this.isLenient = true
    this.ignoreUnknownKeys = true
}

suspend inline fun <reified T> readSignals(requestAdapter: RequestAdapter): T {
    val jsonString = requestAdapter.readSignals()

    return try {
        json.decodeFromString<T>(jsonString)
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid JSON format", e)
    }
}



fun ServerSSESession.createHeartbeat(heartbeatInterval: Long = 5_000): Job {
    val serverSSESession = this
    return launch {
        while (true) {
            try {
                serverSSESession.send()
                delay(heartbeatInterval)
            } catch (e: Exception) {
                serverSSESession.cancel()
                return@launch
            }
        }
    }
}


fun fragment(fragment: (DIV).() -> Unit) = buildString {
    appendHTML().div {
        fragment()
    }
}.drop("<div>".length).dropLast("</ div>".length)