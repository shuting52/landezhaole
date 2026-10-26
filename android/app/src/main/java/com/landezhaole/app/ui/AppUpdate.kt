package com.landezhaole.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.landezhaole.app.AppPalette
import com.landezhaole.app.Category
import com.landezhaole.app.parseCategories
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// ---- 云端版本信息：全部放在 GitHub 仓库（dist/app-version.json），HTTPS 拉取 ----
// APK 下发前用里面的 sha256 验包：哈希对不上就拒绝安装并删包，单改 APK 骗不过。
const val UPDATE_VERSION_URL =
    "https://raw.githubusercontent.com/shuting52/landezhaole/main/dist/app-version.json"
const val CLOUD_DATA_URL =
    "https://raw.githubusercontent.com/shuting52/landezhaole/main/data.json"

private const val TAG = "landezhaole-update"

data class RemoteVersion(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notes: List<String>
)

fun currentVersionCode(ctx: Context): Long {
    return try {
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        PackageInfoCompat.getLongVersionCode(info)
    } catch (e: Exception) {
        0L
    }
}

suspend fun fetchRemoteVersion(): RemoteVersion? {
    return withContext(Dispatchers.IO) {
        try {
            val conn = (URL(UPDATE_VERSION_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode != 200) return@withContext null
            val text = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(text)
            val notes = mutableListOf<String>()
            val arr = obj.optJSONArray("notes")
            if (arr != null) for (i in 0 until arr.length()) notes.add(arr.optString(i))
            RemoteVersion(
                versionCode = obj.optLong("versionCode"),
                versionName = obj.optString("versionName"),
                apkUrl = obj.optString("apkUrl"),
                sha256 = obj.optString("sha256"),
                notes = notes
            )
        } catch (e: Exception) {
            Log.w(TAG, "查云端版本失败: $e")
            null
        }
    }
}

/** 云端站点数据：拉得到就用云端的，拉不到/解析失败就继续用包里自带的。
 *  双源：raw.githubusercontent 慢或不通时走 jsdelivr CDN 镜像（同一仓库同一文件）。 */
suspend fun fetchCloudCategories(): List<Category>? {
    return withContext(Dispatchers.IO) {
        val urls = listOf(
            CLOUD_DATA_URL,
            "https://cdn.jsdelivr.net/gh/shuting52/landezhaole@main/data.json"
        )
        for (url in urls) {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                if (conn.responseCode != 200) continue
                val cats = parseCategories(conn.inputStream.bufferedReader().readText())
                if (cats.isNotEmpty()) return@withContext cats
            } catch (e: Exception) {
                Log.w(TAG, "拉云端数据失败($url): $e")
            }
        }
        null
    }
}

fun updateDir(ctx: Context): File = File(ctx.cacheDir, "update")

/**
 * 安装后清残留：上次触发安装的版本号已经装上（当前版本 >= 待装版本），
 * 就把更新目录整个删掉；没装上的情况不删，用户下次打开还能接着用。
 * 另：启动时顺手清掉更新目录里的临时分片。
 */
fun cleanupUpdateLeftovers(ctx: Context) {
    try {
        val prefs = ctx.getSharedPreferences("landezhaole-update", Context.MODE_PRIVATE)
        val pending = prefs.getLong("pending_code", -1L)
        val current = currentVersionCode(ctx)
        val dir = updateDir(ctx)
        if (pending != -1L && pending <= current) {
            if (dir.exists()) dir.deleteRecursively()
            prefs.edit().remove("pending_code").apply()
            Log.i(TAG, "新版本已装上，更新残留已清")
        } else if (dir.exists()) {
            dir.listFiles()?.forEach { if (it.name.endsWith(".tmp")) it.delete() }
        }
    } catch (e: Exception) {
        Log.w(TAG, "清残留失败: $e")
    }
}

fun installApk(ctx: Context, file: File, versionCode: Long) {
    ctx.getSharedPreferences("landezhaole-update", Context.MODE_PRIVATE)
        .edit().putLong("pending_code", versionCode).apply()
    val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(intent)
}

// ---------------------------------------------------------------------------
// 分段下载：按用户给的 NativeDownloader 核心算法移植（去 Flutter 通道、去 OkHttp，
// 只用 HttpURLConnection；相册重名那套 DownloadNames 与 APK 更新无关，没有移植）。
//   - 探针：Range bytes=0-0 问总长与是否支持 Range
//   - >= 8MB 且支持 Range：4MB 一段，多路并行、共享游标认领、每段 seek 直写
//   - 不支持 Range / 未知大小：整条顺序收
//   - 每段失败重试 3 次（300/900ms 退避），取消时断开全部连接并删残包
//   - 完整性不看文件长度（预分配会撑大长度），只看实际写进的字节数 + SHA-256
// ---------------------------------------------------------------------------

internal fun claimedChunk(claim: Long, size: Long, chunkBytes: Long): LongRange? {
    require(size > 0 && chunkBytes > 0 && claim >= 0)
    val start = claim * chunkBytes
    if (start >= size) return null
    return start..(minOf(size, start + chunkBytes) - 1)
}

internal fun contentRangeStart(header: String?): Long? {
    val value = header?.trim().orEmpty()
    if (!value.startsWith("bytes", ignoreCase = true)) return null
    val range = value.substringAfter(' ', "").substringBefore('/').trim()
    return range.substringBefore('-').trim().toLongOrNull()
}

class UpdateDownloader {
    private val cancelled = AtomicBoolean(false)
    private val conns = ConcurrentHashMap.newKeySet<HttpURLConnection>()

    fun cancel() {
        cancelled.set(true)
        conns.toList().forEach { runCatching { it.disconnect() } }
    }

    fun isCancelled(): Boolean = cancelled.get()

    private fun openConn(url: String, start: Long, end: Long): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        if (start >= 0) conn.setRequestProperty("Range", "bytes=$start-$end")
        conn.setRequestProperty("Accept-Encoding", "identity")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        conn.connectTimeout = 20000
        conn.readTimeout = 30000
        conns.add(conn)
        return conn
    }

    private fun closeConn(conn: HttpURLConnection) {
        conns.remove(conn)
        runCatching { conn.disconnect() }
    }

    private data class Probe(val size: Long, val rangeSupported: Boolean)

    private fun probe(url: String): Probe {
        val conn = openConn(url, 0, 0)
        try {
            if (cancelled.get()) throw InterruptedException()
            return when (conn.responseCode) {
                206 -> {
                    val total = conn.getHeaderField("Content-Range")
                        ?.substringAfterLast('/')?.toLongOrNull() ?: 0L
                    Probe(total, true)
                }
                200 -> Probe(conn.contentLengthLong, false)
                else -> throw IOException("HTTP ${conn.responseCode}")
            }
        } finally {
            closeConn(conn)
        }
    }

    /**
     * 下完返回文件；失败抛异常（调用方删包）。onProgress 在后台线程回调
     * （received, total, speedBytesPerSec），内部 200ms 节流。
     */
    suspend fun download(
        ctx: Context,
        url: String,
        outFile: File,
        expectedSha256: String,
        onProgress: (Long, Long, Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        UpdateKeepAlive.sync(ctx, 1)
        try {
            outFile.parentFile?.mkdirs()
            val probe = probe(url)
            val total = probe.size
            val received = AtomicLong(0)
            var lastReportAt = 0L
            var lastReported = 0L
            val report = { force: Boolean ->
                val now = System.currentTimeMillis()
                if (force || now - lastReportAt >= 200) {
                    val dt = (now - lastReportAt).coerceAtLeast(1)
                    val speed = if (lastReportAt == 0L) 0L
                        else (received.get() - lastReported) * 1000 / dt
                    lastReportAt = now
                    lastReported = received.get()
                    onProgress(received.get(), total, speed)
                }
            }
            if (total >= 8L * 1024 * 1024 && probe.rangeSupported) {
                downloadSegmented(url, outFile, total, received, report)
            } else {
                downloadWhole(url, outFile, total, received, report)
            }
            if (cancelled.get()) throw InterruptedException()
            val written = if (total > 0) outFile.length() else received.get()
            // 注意：分段下载预分配了文件长度，这里不能只看长度；written 取的是
            // 实际累计字节（分段路径里 received 就是它），长度校验只作兜底。
            if (total > 0 && received.get() < total) {
                throw IOException("下载不完整 ${received.get()}/$total")
            }
            report(true)
            if (expectedSha256.isNotBlank()) {
                val actual = sha256Hex(outFile)
                if (!actual.equals(expectedSha256.trim().lowercase(), ignoreCase = true)) {
                    throw SecurityException("安装包校验失败（SHA-256 对不上），已拒绝安装")
                }
            }
            outFile
        } finally {
            UpdateKeepAlive.sync(ctx, 0)
        }
    }

    private fun downloadSegmented(
        url: String,
        outFile: File,
        total: Long,
        received: AtomicLong,
        report: (Boolean) -> Unit
    ) {
        val segmentBytes = 4L * 1024 * 1024
        val segments = (total + segmentBytes - 1) / segmentBytes
        val lanes = minOf(8, segments.toInt())
        RandomAccessFile(outFile, "rw").use { it.setLength(total) }
        val claim = AtomicLong(0)
        val firstError = AtomicReference<Throwable?>(null)
        val pool = Executors.newFixedThreadPool(lanes)
        val latch = CountDownLatch(lanes)
        repeat(lanes) {
            pool.execute {
                try {
                    while (!cancelled.get()) {
                        val range = claimedChunk(claim.getAndIncrement(), total, segmentBytes)
                            ?: break
                        downloadRange(url, outFile, range, received, report)
                    }
                } catch (e: Throwable) {
                    firstError.compareAndSet(null, e)
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        pool.shutdown()
        if (cancelled.get()) throw InterruptedException()
        firstError.get()?.let { throw it }
    }

    private fun downloadRange(
        url: String,
        outFile: File,
        range: LongRange,
        received: AtomicLong,
        report: (Boolean) -> Unit
    ) {
        var writtenInRange = 0L
        val wanted = range.last - range.first + 1
        var attempt = 0
        val delays = longArrayOf(300, 900)
        while (true) {
            if (cancelled.get()) throw InterruptedException()
            try {
                val start = range.first + writtenInRange
                val conn = openConn(url, start, range.last)
                try {
                    val code = conn.responseCode
                    if (code != 206) {
                        // 服务端忽略 Range 直接回整条、且区间正好是整条时才认
                        val whole = code == 200 && start == 0L &&
                            conn.contentLengthLong == range.last + 1
                        if (!whole) throw IOException("HTTP $code")
                    }
                    val declared = contentRangeStart(conn.getHeaderField("Content-Range"))
                    if (code == 206 && declared != null && declared != start) {
                        throw IOException("Content-Range 偏移不符")
                    }
                    RandomAccessFile(outFile, "rw").use { raf ->
                        raf.seek(start)
                        val buf = ByteArray(8192)
                        var n = 0
                        val input = conn.inputStream
                        while (!cancelled.get() &&
                            input.read(buf).also { n = it } != -1
                        ) {
                            raf.write(buf, 0, n)
                            writtenInRange += n
                            received.addAndGet(n.toLong())
                            report(false)
                            if (writtenInRange >= wanted) break
                        }
                    }
                } finally {
                    closeConn(conn)
                }
                if (writtenInRange >= wanted) return
                if (cancelled.get()) throw InterruptedException()
                throw IOException("分段少收 ${writtenInRange}/$wanted")
            } catch (e: InterruptedException) {
                throw e
            } catch (e: Throwable) {
                attempt++
                if (attempt >= 3) throw e
                Thread.sleep(delays.getOrElse(attempt - 1) { 900 })
            }
        }
    }

    private fun downloadWhole(
        url: String,
        outFile: File,
        total: Long,
        received: AtomicLong,
        report: (Boolean) -> Unit
    ) {
        var attempt = 0
        val delays = longArrayOf(300, 900)
        while (true) {
            if (cancelled.get()) throw InterruptedException()
            try {
                val conn = openConn(url, -1, -1)
                try {
                    if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
                    outFile.outputStream().buffered().use { out ->
                        val buf = ByteArray(8192)
                        var n = 0
                        val input = conn.inputStream
                        while (!cancelled.get() &&
                            input.read(buf).also { n = it } != -1
                        ) {
                            out.write(buf, 0, n)
                            received.addAndGet(n.toLong())
                            report(false)
                        }
                    }
                } finally {
                    closeConn(conn)
                }
                if (cancelled.get()) throw InterruptedException()
                return
            } catch (e: InterruptedException) {
                throw e
            } catch (e: Throwable) {
                attempt++
                if (attempt >= 3) throw e
                Thread.sleep(delays.getOrElse(attempt - 1) { 900 })
            }
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buf = ByteArray(8192)
            var n = 0
            while (input.read(buf).also { n = it } != -1) digest.update(buf, 0, n)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

private fun fmtMb(bytes: Long): String {
    val mb = bytes / 1024.0 / 1024.0
    return if (mb < 10) "%.1f".format(mb) else "%.0f".format(mb)
}

private fun fmtSpeed(bps: Long): String {
    return when {
        bps <= 0 -> "-- MB/s"
        bps < 1024 * 1024 -> "%d KB/s".format(bps / 1024)
        else -> "%.1f MB/s".format(bps / 1024.0 / 1024.0)
    }
}

// ---- 更新弹窗：进度卡片（进度条 + 百分比 + 速度 + 取消），按用户下载进度卡片的形态 ----

@Composable
fun UpdateDialog(version: RemoteVersion, pal: AppPalette, onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var received by remember { mutableStateOf(0L) }
    var total by remember { mutableStateOf(0L) }
    var speed by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloader by remember { mutableStateOf<UpdateDownloader?>(null) }

    fun startDownload() {
        error = null
        downloading = true
        received = 0L
        total = 0L
        speed = 0L
        val dl = UpdateDownloader()
        downloader = dl
        scope.launch {
            val dir = updateDir(ctx)
            // 开下前先清掉上次的残包，不让旧包混进来
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "landezhaole-update.apk")
            try {
                dl.download(ctx, version.apkUrl, target, version.sha256) { r, t, s ->
                    received = r
                    total = t
                    speed = s
                }
                if (dl.isCancelled()) return@launch
                installApk(ctx, target, version.versionCode)
                onDismiss()
            } catch (e: InterruptedException) {
                target.delete()
                downloading = false
            } catch (e: Throwable) {
                target.delete()
                downloading = false
                error = e.message ?: "下载失败"
                Log.w(TAG, "更新下载失败: $e")
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { Text("发现新版本 ${version.versionName}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (version.notes.isNotEmpty()) {
                    version.notes.forEach { note ->
                        Text("• $note", fontSize = 13.sp, color = pal.secondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (downloading) {
                    val fraction = if (total > 0) (received.toFloat() / total).coerceIn(0f, 1f) else 0f
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${(fraction * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${fmtMb(received)} MB / ${fmtMb(total)} MB · ${fmtSpeed(speed)}", fontSize = 12.sp, color = pal.secondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = pal.accent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("切到别的应用也会继续下载", fontSize = 11.sp, color = pal.secondary)
                } else if (error != null) {
                    Text(error!!, fontSize = 13.sp, color = pal.danger)
                } else {
                    Text("安装后直接覆盖旧版本，不用卸载；装好后下载的安装包会自动删掉。", fontSize = 13.sp, color = pal.secondary)
                }
            }
        },
        confirmButton = {
            if (downloading) {
                TextButton(onClick = {
                    downloader?.cancel()
                    downloading = false
                }) { Text("取消下载") }
            } else {
                Button(
                    onClick = { startDownload() },
                    colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = Color.White)
                ) { Text(if (error != null) "重新下载" else "立即更新") }
            }
        },
        dismissButton = {
            if (!downloading) TextButton(onClick = onDismiss) { Text("稍后") }
        }
    )
}
