package dev.milan.cryptogram.data.daily

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Fetches raw daily files from the content repo. 5s connect / 5s read, no retries. */
class DailyRemoteSource(
    baseUrl: String,
    client: OkHttpClient,
) {
    private val base = baseUrl.trimEnd('/')
    private val http = client.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchDaily(date: String): DailyFile = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$base/daily/$date.json").build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} for $date")
            val body = resp.body?.string() ?: error("empty body for $date")
            json.decodeFromString(DailyFile.serializer(), body)
        }
    }
}
