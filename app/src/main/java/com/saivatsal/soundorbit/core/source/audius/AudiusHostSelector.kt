package com.saivatsal.soundorbit.core.source.audius

import com.saivatsal.soundorbit.core.common.Dispatcher
import com.saivatsal.soundorbit.core.common.SoundOrbitDispatchers
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiusHostSelector @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    @Dispatcher(SoundOrbitDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    private val mutex = Mutex()
    private var cachedHost: String? = null
    private var lastFetchTimeMs: Long = 0L

    var overrideHost: String? = null
    var coordinatorUrl: String = COORDINATOR_URL

    private val fallbackHosts = listOf(
        "https://discoveryprovider.audius.co",
        "https://audius-discovery-1.cultur3stake.com",
        "https://audius-discovery-2.cultur3stake.com",
        "https://audius-dp.amsterdam.creatorseed.com",
        "https://discoveryprovider3.audius.co"
    )

    suspend fun getHealthyHost(): String = withContext(ioDispatcher) {
        overrideHost?.let { return@withContext it }
        val now = System.currentTimeMillis()
        mutex.withLock {
            val current = cachedHost
            if (current != null && (now - lastFetchTimeMs) < CACHE_TTL_MS) {
                return@withContext current
            }

            val fetched = fetchHostsFromCoordinator()
            val selected = fetched.firstOrNull() ?: fallbackHosts.first()
            cachedHost = selected
            lastFetchTimeMs = now
            return@withContext selected
        }
    }

    suspend fun reportHostFailure(failedHost: String) = withContext(ioDispatcher) {
        mutex.withLock {
            if (cachedHost == failedHost) {
                cachedHost = fallbackHosts.firstOrNull { it != failedHost } ?: fallbackHosts.first()
            }
        }
    }

    private fun fetchHostsFromCoordinator(): List<String> {
        return try {
            val request = Request.Builder()
                .url(coordinatorUrl)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val adapter = moshi.adapter<Map<String, Any>>(type)
                val map = adapter.fromJson(body)
                @Suppress("UNCHECKED_CAST")
                val data = map?.get("data") as? List<String>
                data?.filter { it.startsWith("http") } ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val COORDINATOR_URL = "https://api.audius.co"
        private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes
    }
}
