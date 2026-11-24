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
    val releaseNotes: String = "",
    val apkUrl: String? = null,
    val statusMessage: String = "Not checked yet",
    val updateAvailable: Boolean = false
)

object UpdateManager {
    private const val RELEASE_API =
        "https://api.github.com/repos/2022Brave/Auvionyx/releases/latest"
    private const val APK_MIME = "application/vnd.android.package-archive"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(AppUpdateState())
    val state: StateFlow<AppUpdateState> = mutableState.asStateFlow()

    fun check(context: Context, force: Boolean = false) {
        if (mutableState.value.checking) return
        scope.launch {
            mutableState.value = mutableState.value.copy(
                checking = true,
                statusMessage = "Checking GitHub Releases…"
            )
            val result = runCatching {
                val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    setRequestProperty("User-Agent", "LumaNAV-Android")
                }
                try {
                    if (connection.responseCode !in 200..299) {
                        error("GitHub returned HTTP ${connection.responseCode}")
                    }
                    val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val tag = json.optString("tag_name").removePrefix("v")
                    val latestCode = tag.substringAfterLast('.', "").toIntOrNull()
                    val currentCode = BuildConfig.VERSION_CODE
                    val apk = json.optJSONArray("assets")
                    var apkUrl: String? = null
                    if (apk != null) {
                        for (i in 0 until apk.length()) {
                            val asset = apk.optJSONObject(i) ?: continue
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
                        releaseNotes = json.optString("body").ifBlank { "No release notes provided." },
                        apkUrl = apkUrl,
                        updateAvailable = latestIsNewer && apkUrl != null,
                        statusMessage = when {
                            latestIsNewer && apkUrl == null -> "A release exists, but no APK asset was attached."
                            latestIsNewer -> "Update available"
                            else -> "You're up to date"
                        }
                    )
                } finally {
                    connection.disconnect()
                }
            }
            mutableState.value = result.getOrElse { error ->
                mutableState.value.copy(
                    statusMessage = if (force) "Check failed: ${error.message ?: "offline"}" else "Could not check (offline or GitHub unavailable)"
                )
            }.copy(checking = false)
        }
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
