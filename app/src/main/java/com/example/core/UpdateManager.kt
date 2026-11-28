package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateState(
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val downloadProgress: Int = 0,
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val latestVersion: String? = null,
    val latestDisplayVersion: String? = null,
    val apkSizeBytes: Long? = null,
    val releaseNotes: String = "",
    val apkUrl: String? = null,
    val statusMessage: String = "Not checked yet",
    val updateAvailable: Boolean = false
)

object UpdateManager {
    private const val RELEASE_API =
        "https://api.github.com/repos/2022Brave/LumaNAV/releases/latest"
    private const val APK_MIME = "application/vnd.android.package-archive"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(AppUpdateState())
    val state: StateFlow<AppUpdateState> = mutableState.asStateFlow()

    fun check(context: Context, force: Boolean = false) {
        if (mutableState.value.checking) return
        scope.launch {
            mutableState.value = mutableState.value.copy(
                checking = true,
                statusMessage = "Checking LumaNAV development updates…"
            )
            val result = runCatching {
                // The personal development channel is checked first. Its raw manifest
                // avoids GitHub API rate limits and is updated after each successful CI build.
                val release = fetchLatestDevelopmentUpdate()
                val tag = release.optString("tag_name").removePrefix("v")
                val latestCode = release.optInt("version_code", -1).takeIf { it > 0 }
                    ?: tag.substringAfterLast('.', "").toIntOrNull()
                val currentCode = BuildConfig.VERSION_CODE
                val assets = release.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                            break
                        }
                    }
                }
                val latestIsNewer = latestCode?.let { it > currentCode }
                    ?: (tag != BuildConfig.VERSION_NAME && tag.isNotBlank())
                AppUpdateState(
                    currentVersion = BuildConfig.VERSION_NAME,
                    latestVersion = tag,
                    latestDisplayVersion = release.optString("display_version").takeIf { it.isNotBlank() } ?: tag,
                    apkSizeBytes = release.optLong("apk_size_bytes", -1L).takeIf { it > 0L },
                    releaseNotes = release.optString("body").ifBlank { "No release notes provided." },
                    apkUrl = apkUrl,
                    updateAvailable = latestIsNewer && apkUrl != null,
                    statusMessage = when {
                        latestIsNewer && apkUrl == null -> "Release $tag exists, but no APK asset was attached."
                        latestIsNewer -> "Development update available"
                        else -> "You're up to date"
                    }
                )
            }
            mutableState.value = result.getOrElse { error ->
                val detail = error.message ?: error.javaClass.simpleName
                android.util.Log.e("LumaNAV-Updater", "Development update check failed", error)
                mutableState.value.copy(statusMessage = "Update check failed: $detail")
            }.copy(checking = false)
        }
    }

    private fun fetchLatestDevelopmentUpdate(): JSONObject {
        val response = requestGitHubJson(
            "https://raw.githubusercontent.com/2022Brave/LumaNAV/branding/lumanav-ui-overhaul/latest-dev.json"
        )
        if (response.first == HttpURLConnection.HTTP_OK) {
            val manifest = JSONObject(response.second)
            val versionName = manifest.optString("versionName").ifBlank {
                throw IllegalStateException("Development OTA manifest is missing versionName")
            }
            val apkUrl = manifest.optString("apkUrl")
            val assets = org.json.JSONArray()
            if (apkUrl.isNotBlank()) {
                val assetName = Uri.parse(apkUrl).lastPathSegment?.takeIf { it.endsWith(".apk", true) }
                    ?: "LumaNAV-development-update.apk"
                assets.put(JSONObject().put("name", assetName).put("browser_download_url", apkUrl))
            }
            return JSONObject()
                .put("tag_name", versionName)
                .put("version_code", manifest.optInt("versionCode", -1))
                .put("display_version", manifest.optString("displayVersion"))
                .put("apk_size_bytes", manifest.optLong("apkSizeBytes", -1L))
                .put("body", manifest.optString("notes", "Development build"))
                .put("assets", assets)
        }
        if (response.first == HttpURLConnection.HTTP_NOT_FOUND) {
            // Before the first development OTA has been published, retain stable OTA support.
            return fetchLatestRelease()
        }
        throw IllegalStateException(
            "Development manifest request failed: HTTP ${response.first}: ${githubErrorMessage(response.second)}"
        )
    }

    private fun fetchLatestRelease(): JSONObject {
        val latestUrl = "https://api.github.com/repos/2022Brave/LumaNAV/releases/latest"
        val latestResponse = requestGitHubJson(latestUrl)
        if (latestResponse.first == HttpURLConnection.HTTP_OK) {
            return JSONObject(latestResponse.second)
        }
        if (latestResponse.first == HttpURLConnection.HTTP_FORBIDDEN ||
            latestResponse.first == 429
        ) {
            return fetchLatestReleaseManifest()
        }
        if (latestResponse.first != HttpURLConnection.HTTP_NOT_FOUND) {
            throw IllegalStateException("HTTP ${latestResponse.first}: ${githubErrorMessage(latestResponse.second)}")
        }

        val listResponse = requestGitHubJson(
            "https://api.github.com/repos/2022Brave/LumaNAV/releases?per_page=1"
        )
        if (listResponse.first !in 200..299) {
            throw IllegalStateException("HTTP ${listResponse.first}: ${githubErrorMessage(listResponse.second)}")
        }
        val releases = org.json.JSONArray(listResponse.second)
        if (releases.length() == 0) {
            throw IllegalStateException("HTTP 404: No published release found")
        }
        return releases.getJSONObject(0)
    }

    private fun fetchLatestReleaseManifest(): JSONObject {
        val manifestResponse = requestGitHubJson(
            "https://raw.githubusercontent.com/2022Brave/LumaNAV/main/latest.json"
        )
        if (manifestResponse.first !in 200..299) {
            throw IllegalStateException(
                "GitHub API rate limited and raw manifest fallback failed: HTTP ${manifestResponse.first}"
            )
        }
        val manifest = JSONObject(manifestResponse.second)
        val versionName = manifest.optString("versionName").ifBlank {
            throw IllegalStateException("Updater manifest is missing versionName")
        }
        val apkUrl = manifest.optString("apkUrl")
        val assets = org.json.JSONArray()
        if (apkUrl.isNotBlank()) {
            val assetName = Uri.parse(apkUrl).lastPathSegment?.takeIf { it.endsWith(".apk", true) }
                ?: "LumaNAV-update.apk"
            assets.put(JSONObject().put("name", assetName).put("browser_download_url", apkUrl))
        }
        return JSONObject()
            .put("tag_name", versionName)
            .put("version_code", manifest.optInt("versionCode", -1))
            .put("display_version", manifest.optString("displayVersion"))
            .put("apk_size_bytes", manifest.optLong("apkSizeBytes", -1L))
            .put("body", manifest.optString("notes", "No release notes provided."))
            .put("assets", assets)
    }

    private fun requestGitHubJson(endpoint: String): Pair<Int, String> {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "LumaNAV-Android")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            code to body
        } finally {
            connection.disconnect()
        }
    }

    private fun githubErrorMessage(body: String): String {
        if (body.isBlank()) return "GitHub did not provide an error description"
        return runCatching { JSONObject(body).optString("message").ifBlank { body.take(180) } }
            .getOrDefault(body.take(180))
    }

    fun downloadAndInstall(context: Context) {
        val url = mutableState.value.apkUrl ?: return
        if (mutableState.value.downloading) return
        scope.launch {
            mutableState.value = mutableState.value.copy(downloading = true, downloadProgress = 0)
            val result = runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "LumaNAV-Android")
                }
                val apkFile = File(context.cacheDir, "lumanav-update.apk")
                try {
                    val total = connection.contentLengthLong
                    connection.inputStream.use { input ->
                        apkFile.outputStream().use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var copied = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                copied += count
                                val progress = if (total > 0) ((copied * 100) / total).toInt() else 0
                                mutableState.value = mutableState.value.copy(downloadProgress = progress.coerceIn(0, 100))
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                withContext(Dispatchers.Main) {
                    val packageManager = context.packageManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        !packageManager.canRequestPackageInstalls()
                    ) {
                        mutableState.value = mutableState.value.copy(
                            downloading = false,
                            statusMessage = "Enable Install unknown apps for LumaNAV, then tap Install Update again."
                        )
                        val permissionIntent = Intent(
                            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(permissionIntent)
                    } else {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            apkFile
                        )
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, APK_MIME)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(installIntent)
                        mutableState.value = mutableState.value.copy(
                            downloading = false,
                            downloadProgress = 100,
                            statusMessage = "APK downloaded. Android installer opened."
                        )
                    }
                }
            }
            if (result.isFailure) {
                mutableState.value = mutableState.value.copy(
                    downloading = false,
                    statusMessage = "Download failed: ${result.exceptionOrNull()?.message ?: "unknown error"}"
                )
            }
        }
    }
}
