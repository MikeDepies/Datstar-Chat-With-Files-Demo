package com.stableform.datastar.sdk

import kotlinx.html.Tag
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Represents the content type for HTTP actions.
 * @property type The string representation of the content type
 */
@Serializable
enum class ActionContentType(val type: String) {
    /** JSON content type */
    json("json"),
    /** Form content type */
    form("form");

    override fun toString(): String = type
}

/**
 * Configuration options for HTTP actions.
 * @property contentType The type of content being sent (JSON or form)
 * @property includeLocal Whether to include local data
 * @property selector Optional CSS selector to target specific elements
 * @property headers Custom HTTP headers to include with the request
 * @property openWhenHidden Whether to trigger the action when the element is hidden
 * @property retryInterval Initial interval in milliseconds between retry attempts
 * @property retryScaler Factor by which the retry interval increases
 * @property retryMaxWaitMs Maximum wait time in milliseconds between retries
 * @property retryMaxCount Maximum number of retry attempts
 */
@Serializable
data class HttpActionOptions(
    val contentType: ActionContentType = ActionContentType.json,
    val includeLocal: Boolean = false,
    val selector: String? = null,
    val headers: Map<String, String> = mapOf(),
    val openWhenHidden: Boolean = false,
    val retryInterval: Int = 1000,
    val retryScaler: Int = 2,
    val retryMaxWaitMs: Int = 30000,
    val retryMaxCount: Int = 10
) {
    companion object {
        /** Predefined options for form submissions */
        val form = HttpActionOptions(ActionContentType.form)
    }
}

/**
 * Creates a GET action attribute for an HTML tag.
 * @param url The URL to send the GET request to
 * @param options Configuration options for the request
 * @param json JSON serializer instance
 * @return A string representing the GET action attribute
 */
fun Tag.getAction(url: String, options: HttpActionOptions = HttpActionOptions(), json: Json = Json) =
    "@get(\"$url\", ${json.encodeToString(options)})"

/**
 * Creates a POST action attribute for an HTML tag.
 * @param url The URL to send the POST request to
 * @param options Configuration options for the request
 * @param json JSON serializer instance
 * @return A string representing the POST action attribute
 */
fun Tag.postAction(url: String, options: HttpActionOptions = HttpActionOptions(), json: Json = Json) =
    "@post(\"$url\", ${json.encodeToString(options)})"

/**
 * Creates a PUT action attribute for an HTML tag.
 * @param url The URL to send the PUT request to
 * @param options Configuration options for the request
 * @param json JSON serializer instance
 * @return A string representing the PUT action attribute
 */
fun Tag.putAction(url: String, options: HttpActionOptions = HttpActionOptions(), json: Json = Json) =
    "@put(\"$url\", ${json.encodeToString(options)})"

/**
 * Creates a DELETE action attribute for an HTML tag.
 * @param url The URL to send the DELETE request to
 * @param options Configuration options for the request
 * @param json JSON serializer instance
 * @return A string representing the DELETE action attribute
 */
fun Tag.deleteAction(url: String, options: HttpActionOptions = HttpActionOptions(), json: Json = Json) =
    "@delete(\"$url\", ${json.encodeToString(options)})"

/**
 * Creates a PATCH action attribute for an HTML tag.
 * @param url The URL to send the PATCH request to
 * @param options Configuration options for the request
 * @param json JSON serializer instance
 * @return A string representing the PATCH action attribute
 */
fun Tag.patchAction(url: String, options: HttpActionOptions = HttpActionOptions(), json: Json = Json) =
    "@patch(\"$url\", ${json.encodeToString(options)})"