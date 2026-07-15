package com.travelassistant.backend.infrastructure.provider

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.io.IOException
import java.net.URI
import java.net.SocketTimeoutException

internal class PublicHotelsApiHttpTransport(
    private val httpClient: HttpClient,
    private val publicTarget: HotelsApiTargetConfig,
) {
    suspend fun postJson(
        path: String,
        body: String,
        userLanguage: String? = null,
    ): HotelsApiHttpResponse {
        val requestUrl = resolveRequestUrl(path)
        val response = try {
            httpClient.post(requestUrl) {
                expectSuccess = false
                timeout {
                    requestTimeoutMillis = publicTarget.timeoutMillis
                }
                accept(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                userLanguage?.let { header(USER_LANGUAGE_HEADER, it) }
                setBody(body)
            }
        } catch (exception: HttpRequestTimeoutException) {
            throw providerException(
                category = HotelProviderErrorCategory.TIMEOUT,
                message = "Hotels API request timed out",
                cause = exception,
            )
        } catch (exception: SocketTimeoutException) {
            throw providerException(
                category = HotelProviderErrorCategory.TIMEOUT,
                message = "Hotels API request timed out",
                cause = exception,
            )
        } catch (exception: IOException) {
            throw providerException(
                category = HotelProviderErrorCategory.UNAVAILABLE,
                message = "Hotels API is unavailable",
                cause = exception,
            )
        }

        if (!response.status.isSuccess()) {
            throw providerExceptionFor(response.status.value)
        }

        return HotelsApiHttpResponse(
            statusCode = response.status.value,
            contentType = response.contentType()?.toString(),
            body = response.body(),
        )
    }

    private fun resolveRequestUrl(path: String): String {
        val pathUri = runCatching { URI(path) }.getOrNull()
        val isRelativeApiPath = pathUri != null &&
            path.startsWith("/") &&
            !path.startsWith("//") &&
            !pathUri.isAbsolute &&
            pathUri.host == null &&
            pathUri.query == null &&
            pathUri.fragment == null

        if (!isRelativeApiPath) {
            throw providerException(
                category = HotelProviderErrorCategory.UNKNOWN,
                message = "Hotels API request path must be a relative API path",
            )
        }

        val baseUri = URI(publicTarget.baseUri)
        val resolvedUri = baseUri.resolve(pathUri)
        val targetsPublicHost = resolvedUri.scheme == baseUri.scheme &&
            resolvedUri.host == baseUri.host &&
            resolvedUri.port == baseUri.port

        if (!targetsPublicHost) {
            throw providerException(
                category = HotelProviderErrorCategory.UNKNOWN,
                message = "Hotels API request path must target the configured public host",
            )
        }

        return resolvedUri.toString()
    }

    private fun providerExceptionFor(statusCode: Int): HotelProviderException =
        when (statusCode) {
            401, 403 -> providerException(
                HotelProviderErrorCategory.AUTHENTICATION_FAILED,
                "Hotels API rejected request authentication",
            )
            408 -> providerException(
                HotelProviderErrorCategory.TIMEOUT,
                "Hotels API request timed out",
            )
            429 -> providerException(
                HotelProviderErrorCategory.RATE_LIMITED,
                "Hotels API rate limit was reached",
            )
            in 500..599 -> providerException(
                HotelProviderErrorCategory.UNAVAILABLE,
                "Hotels API is unavailable",
            )
            else -> providerException(
                HotelProviderErrorCategory.UNKNOWN,
                "Hotels API request failed",
            )
        }

    private fun providerException(
        category: HotelProviderErrorCategory,
        message: String,
        cause: Throwable? = null,
    ): HotelProviderException = HotelProviderException(category, message, cause)

    private companion object {
        const val USER_LANGUAGE_HEADER = "X-User-Language"
    }
}
