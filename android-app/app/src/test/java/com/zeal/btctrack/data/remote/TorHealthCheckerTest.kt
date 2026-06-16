package com.zeal.btctrack.data.remote

import com.zeal.btctrack.domain.model.AppSettings
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TorHealthCheckerTest {
    @Test
    fun `reports success when onion tip height is reachable`() = runBlocking {
        val checker = TorHealthChecker(
            baseUrl = "http://mempoolhqx4isw62.onion/api/",
            appSettings = AppSettings(),
            httpClient = OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("900000".toResponseBody())
                        .build()
                })
                .build(),
        )

        val status = checker.check()

        assertTrue(status.ok)
        assertTrue(status.message.contains("tip height=900000"))
    }

    @Test
    fun `reports failure when proxy request errors`() = runBlocking {
        val checker = TorHealthChecker(
            baseUrl = "http://mempoolhqx4isw62.onion/api/",
            appSettings = AppSettings(),
            httpClient = OkHttpClient.Builder()
                .addInterceptor(Interceptor { throw IOException("proxy down") })
                .build(),
        )

        val status = checker.check()

        assertFalse(status.ok)
        assertTrue(status.message.contains("proxy down"))
    }
}
