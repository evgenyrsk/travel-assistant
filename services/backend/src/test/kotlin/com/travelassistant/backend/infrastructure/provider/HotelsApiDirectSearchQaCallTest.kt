package com.travelassistant.backend.infrastructure.provider

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class HotelsApiDirectSearchQaCallTest {

    @Test
    fun `runs a direct QA search only with explicit opt in`() = runBlocking {
        val result = HotelsApiDirectSearchQaHarness(
            environment = System.getenv(),
            clientFactory = {
                HttpClient(CIO) {
                    applyHotelsApiQaPolicy()
                }
            },
        ).run()

        when (result) {
            HotelsApiDirectSearchQaHarness.Result.Disabled -> return@runBlocking
            is HotelsApiDirectSearchQaHarness.Result.Rejected -> {
                fail("Hotels API QA call was rejected: ${result.issue.name}")
            }
            is HotelsApiDirectSearchQaHarness.Result.Success -> {
                assertEquals(200, result.statusCode)
                assertTrue(result.hotelCount > 0)
                assertTrue(result.offerCount > 0)
                println(
                    "STAGE_9_16_SAFE_RESULT " +
                        "status=${result.statusCode} " +
                        "contentType=${result.contentType} " +
                        "hotelCount=${result.hotelCount} " +
                        "offerCount=${result.offerCount} " +
                        "isLoadingCompleted=${result.isLoadingCompleted} " +
                        "hasNextOffset=${result.hasNextOffset}",
                )
            }
        }
    }
}
