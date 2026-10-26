package com.landezhaole.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// ---- 应用下载：走网站同款代理（landezhaole-app-proxy），App 内直接搜、直接下、直接装 ----
// 不跳浏览器：结果在 App 内渲染，下载走分段（<8MB 单路，>=8MB 4 路并行、4MB 一段、
// 每段失败重试 3 次），下完调系统安装器；文件名按包名存缓存，装完系统自己处理。

private const val APPDL_PROXY = "https://landezhaole-app-proxy.307779523.workers.dev"
private const val APPDL_CHUNK = 4L * 1024 * 1024

data class AppEntry(
    val appId: String,
    val label: String,
    val pkg: String,
    val verName: String,
    val size: Long,
    val downloads: Long,
    val icon: String,
    val category: String
)

sealed interface DlState {
    data object Idle : DlState
    data class Downloading(val progress: Float, val done: Long, val total: Long) : DlState
    data class Done(val file: File) : DlState
    data class Failed(val msg: String) : DlState
}

fun appdlSafePkg(entry: AppEntry): String =
    entry.pkg.filter { it.isLetterOrDigit() || it == '.' }.ifBlank { entry.appId }

fun appdlFile(ctx: Context, entry: AppEntry, tmp: Boolean): File {
    val dir = File(ctx.cacheDir, "update/appdl").apply { mkdirs() }
    val name = appdlSafePkg(entry) + ".apk" + if (tmp) ".tmp" else ""
    return File(dir, name)
}

suspend fun searchApps(query: String, offset: Int): Pair<Int, List<AppEntry>> =
    withContext(Dispatchers.IO) {
        val url = "$APPDL_PROXY/?q=" + URLEncoder.encode(query, "UTF-8") + "&offset=" + offset
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        if (conn.responseCode != 200) throw IOException("搜索失败 " + conn.responseCode)
        val j = JSONObject(conn.inputStream.bufferedReader().readText())
        val total = j.optInt("total", 0)
        val arr: JSONArray = j.optJSONArray("entries") ?: JSONArray()
        val list = mutableListOf<AppEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                AppEntry(
                    o.optString("app_id"), o.optString("label"), o.optString("package"),
                    o.optString("ver_name"), o.optLong("size"), o.optLong("downloads"),
                    o.optString("icon"), o.optString("category")
                )
            )
        }
        total to list
    }

fun formatAppSize(size: Long): String {
    if (size <= 0) return "大小未知"
    if (size < 1024 * 1024) return (size / 1024).toString() + " KB"
    return String.format("%.1f MB", size / 1024.0 / 1024.0)
}

fun formatDownloads(n: Long): String {
    if (n < 10000) return n.toString() + "次下载"
    return String.format("%.1f万次下载", n / 10000.0)
}

suspend fun downloadAppApk(
    ctx: Context,
    entry: AppEntry,
    onProgress: (Long, Long) -> Unit
): File = withContext(Dispatchers.IO) {
    var size = entry.size
    if (size <= 0) {
        try {
            val conn =
                URL("$APPDL_PROXY/info?app_id=" + URLEncoder.encode(entry.appId, "UTF-8"))
                    .openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            if (conn.responseCode == 200) {
                size = JSONObject(conn.inputStream.bufferedReader().readText()).optLong("size")
            }
        } catch (_: Exception) {
        }
    }
    if (size <= 0) throw IOException("拿不到安装包大小")
    val tmp = appdlFile(ctx, entry, true)
    val out = appdlFile(ctx, entry, false)
    val raf = RandomAccessFile(tmp, "rw")
    try {
        raf.setLength(size)
        val claim = AtomicInteger(0)
        val written = AtomicLong(0)
        val lastEmit = AtomicLong(0)
        coroutineScope {
            val lanes = if (size < 8L * 1024 * 1024) 1 else 4
            val jobs = (0 until lanes).map {
                async {
                    while (true) {
                        ensureActive()
                        val idx = claim.getAndIncrement()
                        val start = idx * APPDL_CHUNK
                        if (start >= size) break
                        val end = minOf(size, start + APPDL_CHUNK) - 1
                        var attempt = 0
                        while (true) {
                            ensureActive()
                            try {
                                val segUrl = "$APPDL_PROXY/segment?app_id=" +
                                    URLEncoder.encode(entry.appId, "UTF-8") +
                                    "&start=" + start + "&end=" + end
                                val conn = URL(segUrl).openConnection() as HttpURLConnection
                                conn.connectTimeout = 15000
                                conn.readTimeout = 30000
                                if (conn.responseCode != 200 && conn.responseCode != 206) {
                                    throw IOException("分段 " + conn.responseCode)
                                }
                                conn.inputStream.use { inp ->
                                    val buf = ByteArray(64 * 1024)
                                    var pos = start
                                    while (true) {
                                        ensureActive()
                                        val n = inp.read(buf)
                                        if (n < 0) break
                                        synchronized(raf) {
                                            raf.seek(pos)
                                            raf.write(buf, 0, n)
                                        }
                                        pos += n
                                        val w = written.addAndGet(n.toLong())
                                        val now = System.currentTimeMillis()
                                        if (now - lastEmit.get() > 150) {
                                            lastEmit.set(now)
                                            onProgress(w, size)
                                        }
                                    }
                                    if (pos != end + 1) throw IOException("分段不完整")
                                }
                                break
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                attempt++
                                if (attempt >= 3) throw e
                                delay(if (attempt == 1) 300L else 900L)
                            }
                        }
                    }
                }
            }
            jobs.forEach { it.await() }
        }
        if (written.get() < size) throw IOException("下载不完整")
    } finally {
        try {
            raf.close()
        } catch (_: Exception) {
        }
    }
    onProgress(size, size)
    if (out.exists()) out.delete()
    tmp.renameTo(out)
    out
}

fun installAppApk(ctx: Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(intent)
}

@Composable
fun AppIcon(iconUrl: String, label: String) {
    var bitmap by remember(iconUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(iconUrl) {
        if (iconUrl.isBlank()) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) { fetchBitmap(iconUrl) }
    }
    Box(
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(11.dp))
            .background(if (bitmap != null) Color.White else avatarColor(label)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(), contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit
            )
        } else {
            Text(label.take(2), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AppResultsSection(
    results: List<AppEntry>,
    total: Int,
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onClear: () -> Unit,
    pal: AppPalette,
    dlStates: Map<String, DlState>,
    onDownload: (AppEntry) -> Unit,
    onCancel: (AppEntry) -> Unit,
    onInstall: (AppEntry, File) -> Unit
) {
    WebSection(pal = pal) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape)
                        .background(Color(0xFF2563EB)).shadow(4.dp, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "应用下载结果", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = pal.danger, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(pal.subtleFill)
                        .padding(horizontal = 10.dp, vertical = 1.dp)
                ) { Text("$total 个", fontSize = 12.sp, color = pal.secondary) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onClear,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pal.barBackground, contentColor = pal.secondary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp, vertical = 3.dp
                    ),
                    modifier = Modifier.height(30.dp)
                ) { Text("清空搜索", fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (error != null) {
                Text(error, fontSize = 14.sp, color = pal.danger)
            } else if (results.isEmpty() && !loading) {
                Text("没找到相关应用，换个关键词试试", fontSize = 14.sp, color = pal.secondary)
            } else {
                for (entry in results) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(entry.icon, entry.label)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.label, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                color = pal.foreground, maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val meta = listOf(
                                entry.verName, formatAppSize(entry.size),
                                formatDownloads(entry.downloads), entry.category
                            ).filter { it.isNotBlank() }.joinToString(" · ")
                            Text(
                                meta, fontSize = 12.sp, color = pal.secondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            when (val st = dlStates[entry.appId]) {
                                is DlState.Downloading -> {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { st.progress },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = pal.accent
                                    )
                                    Text(
                                        "${(st.progress * 100).toInt()}% · " +
                                            formatAppSize(st.done) + " / " + formatAppSize(st.total),
                                        fontSize = 11.sp, color = pal.secondary
                                    )
                                }
                                is DlState.Failed -> {
                                    Text(st.msg, fontSize = 12.sp, color = pal.danger)
                                }
                                else -> {}
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        when (val st = dlStates[entry.appId]) {
                            is DlState.Downloading -> {
                                TextButton(onClick = { onCancel(entry) }) {
                                    Text("取消", fontSize = 13.sp, color = pal.danger)
                                }
                            }
                            is DlState.Done -> {
                                Button(
                                    onClick = { onInstall(entry, st.file) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pal.accent, contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 16.dp, vertical = 6.dp
                                    )
                                ) { Text("安装", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            }
                            is DlState.Failed -> {
                                Button(
                                    onClick = { onDownload(entry) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pal.accent, contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 16.dp, vertical = 6.dp
                                    )
                                ) { Text("重试", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            }
                            else -> {
                                Button(
                                    onClick = { onDownload(entry) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pal.accent, contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 16.dp, vertical = 6.dp
                                    )
                                ) { Text("下载", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                if (loading) {
                    Text("正在加载…", fontSize = 13.sp, color = pal.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (hasMore && !loading) {
                    Button(
                        onClick = onLoadMore,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pal.barBackground, contentColor = pal.foreground
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("找更多", fontSize = 14.sp) }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}
