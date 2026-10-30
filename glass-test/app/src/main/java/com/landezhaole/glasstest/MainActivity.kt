package com.landezhaole.glasstest

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.blur
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// 懒得找了 · 网站原样版 v2.0.0
// 按 https://landezhaole.pages.dev/ 原样写：顶栏 + logo + 9 个搜索源 + 快捷图标行 +
// 分段网格（2 列，首屏每段 7 张 + → 查看更多进分类页）+ 右侧悬浮栏 + 左下 + 工具菜单。
// 子页：ChatGPT 玩法（89 类 / 703 条，点卡片复制）、食谱（100 道，搜索 / 用时筛 / 随机一桌 / 收藏 / 购物清单）、
// 省钱小助手（3 Tab / 22 张券卡）。主题：Libraries 浅深两套。
// 原生 Kotlin + Compose，没有 WebView，没有 HTML。更新：启动读仓库 app-version.json，
// 版本号比已装高就自动弹窗，更新内容按“优化 / 删除 / 增加”三段写。

private object Lib {
    val lightBg = Color(0xFFFDFDFD)
    val lightCard = Color(0xFFFFFFFF)
    val lightText = Color(0xFF0D0D0D)
    val lightMuted = Color(0xFF6C6C6C)
    val lightLine = Color(0x0F000000)
    val lightTabBar = Color(0xFFF1F1F1)
    val lightAccent = Color(0xFF0073E5)
    val darkBg = Color(0xFF121212)
    val darkCard = Color(0xFF181818)
    val darkText = Color(0xFFFFFFFF)
    val darkMuted = Color(0xFFCACACA)
    val darkLine = Color(0x14FFFFFF)
    val darkTabBar = Color(0x0DFFFFFF)
    val darkAccent = Color(0xFF55CFFF)
}

private const val PREFS = "glass_test_prefs"
private const val VERSION_URL =
    "https://raw.githubusercontent.com/shuting52/landezhaole/main/glass-test/app-version.json"
private const val APK_URL =
    "https://raw.githubusercontent.com/shuting52/landezhaole/main/glass-test/app-debug.apk"
private const val SITE_URL = "https://landezhaole.pages.dev/"

private data class Engine(val id: String, val name: String, val url: String?)
private val ENGINES = listOf(
    Engine("site", "站内", null),
    Engine("bing", "必应", "https://cn.bing.com/search?q="),
    Engine("baidu", "百度", "https://www.baidu.com/s?wd="),
    Engine("bilibili", "B站", "https://search.bilibili.com/all?keyword="),
    Engine("baidupan", "百度网盘", "https://wangpan.xin/search?keyword="),
    Engine("alipan", "阿里云盘", "https://v2pan.com/search?q="),
    Engine("quarkpan", "夸克网盘", "https://www.kkso.cc/search?kw="),
    Engine("google", "谷歌", "https://www.google.com/search?q="),
    Engine("appdl", "应用下载", "https://appteka.store/search?q="),
)

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(context, "打不开这个链接", Toast.LENGTH_SHORT).show()
    }
}

private fun hostOf(url: String): String = try {
    Uri.parse(url).host?.removePrefix("www.") ?: ""
} catch (_: Exception) { "" }

/** 站点图标：先用 data.json 的 i，失败依次走谷歌 favicon → 站点 apple-touch-icon → favicon.ico，全部失败用首字头像。 */
private val iconCache = mutableStateMapOf<String, Bitmap?>()

@Composable
private fun SiteIcon(site: Site, size: Int = 44) {
    val key = site.iconUrl.ifEmpty { "google:${hostOf(site.url)}" }
    val bmp = iconCache[key]
    if (bmp == null && !iconCache.containsKey(key)) {
        LaunchedEffect(key) {
            iconCache[key] = loadIconBitmap(site)
        }
    }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(), contentDescription = null,
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape(10.dp))
                .background(Color(avColorHex(site.title))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                site.title.firstOrNull()?.toString() ?: "🔍", color = Color.White,
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

private suspend fun loadIconBitmap(site: Site): Bitmap? = withContext(Dispatchers.IO) {
    val host = hostOf(site.url)
    val candidates = buildList {
        if (site.iconUrl.isNotEmpty()) add(site.iconUrl)
        if (host.isNotEmpty()) {
            add("https://www.google.com/s2/favicons?domain=$host&sz=128")
            add("https://$host/apple-touch-icon.png")
            add("https://$host/favicon.ico")
        }
    }
    for (u in candidates) {
        try {
            val conn = URL(u).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            if (conn.responseCode !in 200..299) continue
            val bmp = BitmapFactory.decodeStream(conn.inputStream) ?: continue
            if (bmp.width < 16 && u.contains("google.com")) continue
            return@withContext bmp
        } catch (_: Exception) {
            continue
        }
    }
    null
}

private data class UpdateInfo(
    val code: Int, val name: String, val opt: String, val del: String, val add: String,
)

private suspend fun fetchUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
    try {
        val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        if (conn.responseCode !in 200..299) return@withContext null
        val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val changes = root.optJSONObject("changes")
        UpdateInfo(
            root.optInt("versionCode", 0), root.optString("versionName"),
            changes?.optString("优化") ?: "", changes?.optString("删除") ?: "",
            changes?.optString("增加") ?: "",
        )
    } catch (_: Exception) {
        null
    }
}

private fun installedCode(context: Context): Int = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    PackageInfoCompat.getLongVersionCode(info).toInt()
} catch (_: Exception) { 0 }


/** 软件内直接下载更新包，不跳浏览器：DownloadManager 下载到 Download 目录，
 *  下载完成广播里自动调系统安装器；安装未知应用权限没开时先跳系统设置让用户开。 */
private fun startApkDownload(context: Context, apkUrl: String, fileName: String) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    if (prefs.getLong("pending_download_id", -1L) != -1L) {
        Toast.makeText(context, "正在下载中，下载完会自动弹出安装", Toast.LENGTH_SHORT).show()
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(context, "请先允许本应用安装未知应用，返回后再点更新", Toast.LENGTH_LONG).show()
            try {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                )
            } catch (_: Exception) { }
            return
        }
    }
    try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("懒得找了更新包")
            .setDescription("正在下载新版本，下载完自动安装")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val id = dm.enqueue(req)
        prefs.edit().putLong("pending_download_id", id).apply()
        Toast.makeText(context, "开始下载，不用跳转浏览器，下载完自动弹出安装", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "下载启动失败，请重试", Toast.LENGTH_SHORT).show()
    }
}

private fun installDownloadedApk(context: Context, downloadId: Long) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    prefs.edit().putLong("pending_download_id", -1L).apply()
    try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
        var ok = false
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                ok = status == DownloadManager.STATUS_SUCCESSFUL
            }
            cursor.close()
        }
        if (!ok) {
            Toast.makeText(context, "下载失败，请重新点更新", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = dm.getUriForDownloadedFile(downloadId) ?: run {
            Toast.makeText(context, "找不到下载好的安装包，请重新下载", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "调起安装失败，请在 Download 文件夹里手动安装", Toast.LENGTH_LONG).show()
    }
}

private sealed class Page {
    data object Home : Page()
    data object Prompts : Page()
    data object Recipes : Page()
    data object Sq : Page()
    data object Settings : Page()
    data object Favorites : Page()
    data class Category(val id: String) : Page()
}

class MainActivity : ComponentActivity() {
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (id != -1L && id == prefs.getLong("pending_download_id", -1L)) {
                installDownloadedApk(context, id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, filter)
        }
        setContent { AppRoot() }
    }

    override fun onDestroy() {
        try { unregisterReceiver(downloadReceiver) } catch (_: Exception) { }
        super.onDestroy()
    }

    @Composable
    private fun AppRoot() {
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
        val cats = remember { loadSiteData(context) }
        val prompts = remember { loadPrompts(context) }
        val dishes = remember { loadDishes(context) }
        val sq = remember { loadSq(context) }
        val systemDark = isSystemInDarkTheme()
        var themeMode by remember { mutableStateOf(prefs.getInt("theme_mode", 0)) }
        var scale by remember { mutableStateOf(prefs.getFloat("scale", 0.75f)) }
        val dark = when (themeMode) { 1 -> true; 2 -> systemDark; else -> false }
        val bg = if (dark) Lib.darkBg else Lib.lightBg
        val card = if (dark) Lib.darkCard else Lib.lightCard
        val text = if (dark) Lib.darkText else Lib.lightText
        val muted = if (dark) Lib.darkMuted else Lib.lightMuted
        val line = if (dark) Lib.darkLine else Lib.lightLine
        val tabBar = if (dark) Lib.darkTabBar else Lib.lightTabBar
        val accent = if (dark) Lib.darkAccent else Lib.lightAccent

        var stack by remember { mutableStateOf<List<Page>>(listOf(Page.Home)) }
        fun go(p: Page) { stack = stack + p }
        fun back() { if (stack.size > 1) stack = stack.dropLast(1) }
        BackHandler(enabled = stack.size > 1) { back() }
        val page = stack.last()

        var favorites by remember {
            mutableStateOf(prefs.getStringSet("fav_urls", emptySet())?.toSet() ?: emptySet())
        }
        fun toggleFav(url: String) {
            favorites = if (favorites.contains(url)) favorites - url else favorites + url
            prefs.edit().putStringSet("fav_urls", favorites).apply()
        }
        var detailSite by remember { mutableStateOf<Site?>(null) }
        var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        var updateChecked by remember { mutableStateOf(false) }
        var skipCode by remember { mutableStateOf(prefs.getInt("skip_update_code", 0)) }
        val scope = rememberCoroutineScope()
        fun checkUpdate(manual: Boolean) {
            scope.launch {
                val info = fetchUpdate()
                if (info != null && info.code > installedCode(context) && info.code != skipCode) {
                    updateInfo = info
                } else if (manual) {
                    Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (!updateChecked) {
            updateChecked = true
            checkUpdate(false)
        }

        Column(modifier = Modifier.fillMaxSize().background(bg)) {
          val baseDensity = LocalDensity.current
          CompositionLocalProvider(
              LocalDensity provides Density(baseDensity.density * scale, baseDensity.fontScale * scale)
          ) {
            Box(modifier = Modifier.weight(1f)) {
              val backdrop = rememberLayerBackdrop()
              Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                  BlobBackground()
              }
            when (page) {
                is Page.Home -> HomePage(
                    backdrop, cats, favorites, text, muted, line, tabBar, accent, card, bg,
                    onCategory = { go(Page.Category(it)) },
                    onPrompts = { go(Page.Prompts) },
                    onRecipes = { go(Page.Recipes) },
                    onSq = { go(Page.Sq) },
                    onSettings = { go(Page.Settings) },
                    onSite = { openUrl(context, it.url) },
                    onSiteDetail = { detailSite = it },
                    onTheme = {
                        themeMode = when (themeMode) { 0 -> 1; 1 -> 2; else -> 0 }
                        prefs.edit().putInt("theme_mode", themeMode).apply()
                    },
                )
                is Page.Category -> {
                    val cat = cats.firstOrNull { it.id == page.id }
                    if (cat != null) CategoryPage(
                        backdrop, cat, favorites, text, muted, line, tabBar, accent, card,
                        onBack = { back() },
                        onSite = { openUrl(context, it.url) },
                        onSiteDetail = { detailSite = it },
                        onToggleFav = { toggleFav(it) },
                    ) else back()
                }
                is Page.Prompts -> PromptsPage(
                    prompts, text, muted, line, accent, card, onBack = { back() },
                )
                is Page.Recipes -> RecipesPage(
                    dishes, text, muted, line, tabBar, accent, card, onBack = { back() },
                )
                is Page.Sq -> SqPage(
                    sq, text, muted, line, tabBar, accent, card, onBack = { back() },
                )
                is Page.Favorites -> {
                    val favSites = cats.flatMap { it.sites }.filter { favorites.contains(it.url) }
                    FavoritesPage(
                        backdrop, favSites, favorites, text, muted, line, accent, card,
                        onBack = { back() },
                        onSite = { openUrl(context, it.url) },
                        onSiteDetail = { detailSite = it },
                        onToggleFav = { toggleFav(it) },
                    )
                }
                is Page.Settings -> SettingsPage(
                    themeMode, scale, cats, text, muted, line, accent, card,
                    onTheme = { v -> themeMode = v; prefs.edit().putInt("theme_mode", v).apply() },
                    onScale = { v -> scale = v; prefs.edit().putFloat("scale", v).apply() },
                    onCheckUpdate = { checkUpdate(true) },
                    onClearFav = {
                        favorites = emptySet()
                        prefs.edit().putStringSet("fav_urls", emptySet()).apply()
                        Toast.makeText(context, "收藏已清空", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { back() },
                )
            }

            detailSite?.let { site ->
                val clipboard = LocalClipboardManager.current
                AlertDialog(
                    onDismissRequest = { detailSite = null },
                    title = { Text(site.title, color = text, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(site.desc, color = muted, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(site.url, color = accent, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { openUrl(context, site.url); detailSite = null }) {
                            Text("打开", color = accent)
                        }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(site.url))
                                Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                            }) { Text("复制链接", color = muted) }
                            TextButton(onClick = { toggleFav(site.url) }) {
                                Text(if (favorites.contains(site.url)) "取消收藏" else "收藏", color = muted)
                            }
                            TextButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${site.title} ${site.url}")
                                }
                                context.startActivity(Intent.createChooser(send, "分享"))
                            }) { Text("分享", color = muted) }
                        }
                    },
                    containerColor = card,
                )
            }

            updateInfo?.let { info ->
                AlertDialog(
                    onDismissRequest = { updateInfo = null },
                    title = { Text("发现新版本 ${info.name}", color = text, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            if (info.opt.isNotEmpty()) Text("优化：${info.opt}", color = text, fontSize = 13.sp)
                            if (info.del.isNotEmpty()) Text("删除：${info.del}", color = text, fontSize = 13.sp)
                            if (info.add.isNotEmpty()) Text("增加：${info.add}", color = text, fontSize = 13.sp)
                            if (info.opt.isEmpty() && info.del.isEmpty() && info.add.isEmpty())
                                Text("有新版本可用，点更新下载安装。", color = muted, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("下载后点安装即可直接覆盖，不用卸载旧版本。", color = muted, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { startApkDownload(context, APK_URL, "landezhaole-update.apk"); updateInfo = null }) {
                            Text("立即更新", color = accent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = {
                                skipCode = info.code
                                prefs.edit().putInt("skip_update_code", info.code).apply()
                                updateInfo = null
                            }) { Text("不再提示此版本", color = muted) }
                            TextButton(onClick = { updateInfo = null }) {
                                Text("稍后再说", color = muted)
                            }
                        }
                    },
                    containerColor = card,
                )
            }
        }
          }
          // 底栏：一排三个，不跟随界面缩放，字号正常，顶到系统导航栏上方
          Row(
              modifier = Modifier.fillMaxWidth().background(card)
                  .border(1.dp, line, RoundedCornerShape(0.dp))
                  .navigationBarsPadding().padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
          ) {
              val bottomItems = listOf(
                  Triple("首页", page is Page.Home) { stack = listOf(Page.Home) },
                  Triple("收藏", page is Page.Favorites) { if (page !is Page.Favorites) go(Page.Favorites) },
                  Triple("设置", page is Page.Settings) { if (page !is Page.Settings) go(Page.Settings) },
              )
              for ((label, selected, act) in bottomItems) {
                  Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier = Modifier.weight(1f).clickable { act() }.padding(vertical = 10.dp),
                  ) {
                      Text(label, color = if (selected) accent else muted, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                  }
              }
          }
        }
    }
}

@Composable
private fun BlobBackground() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val blobs = listOf(
            Triple(0.02f, 0.00f, Color(0xFF3B82F6)),
            Triple(0.55f, 0.06f, Color(0xFF06B6D4)),
            Triple(0.05f, 0.30f, Color(0xFFF97316)),
            Triple(0.58f, 0.42f, Color(0xFFA855F7)),
            Triple(0.02f, 0.60f, Color(0xFF22C55E)),
            Triple(0.55f, 0.76f, Color(0xFFEC4899)),
        )
        for ((fx, fy, c) in blobs) {
            Box(
                modifier = Modifier
                    .size(w * 0.55f, h * 0.22f)
                    .offset(x = w * fx, y = h * fy)
                    .blur(48.dp)
                    .background(c.copy(alpha = 0.45f), CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SiteCard(
    backdrop: Backdrop, site: Site, text: Color, muted: Color, line: Color, card: Color,
    onOpen: () -> Unit, onDetail: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .drawBackdrop(
                backdrop,
                shape = { RoundedRectangle(14.dp) },
                effects = { vibrancy(); blur(6.dp.toPx()); lens(24.dp.toPx(), 48.dp.toPx()) },
                onDrawSurface = { drawRect(card.copy(alpha = 0.62f)) },
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onOpen, onLongClick = onDetail)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SiteIcon(site)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(site.title, color = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(site.desc, color = muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        // 网站同款彩虹角标：没有 badge 时显示“精选”
        val badge = site.badge.ifEmpty { "精选" }
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .background(Color(0xFFEF4444), RoundedCornerShape(999.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassListCard(
    backdrop: Backdrop, site: Site, catName: String, subName: String,
    isFav: Boolean, text: Color, muted: Color, accent: Color, card: Color,
    onOpen: () -> Unit, onDetail: () -> Unit, onToggleFav: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .drawBackdrop(
                backdrop,
                shape = { RoundedRectangle(18.dp) },
                effects = { vibrancy(); blur(6.dp.toPx()); lens(24.dp.toPx(), 48.dp.toPx()) },
                onDrawSurface = { drawRect(card.copy(alpha = 0.62f)) },
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onOpen, onLongClick = onDetail)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            SiteIcon(site, size = 48)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(site.title, color = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(site.desc, color = muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(site.url, color = accent, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("$catName · $subName", color = accent, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                val badge = site.badge.ifEmpty { "精选" }
                Text(badge, color = accent, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isFav) "★" else "☆", color = accent, fontSize = 16.sp,
                    modifier = Modifier.clickable { onToggleFav() }.padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionGrid(
    backdrop: Backdrop, sites: List<Site>, text: Color, muted: Color, line: Color, card: Color,
    limit: Int, onOpen: (Site) -> Unit, onDetail: (Site) -> Unit, onMore: (() -> Unit)?,
) {
    val shown = if (onMore != null && sites.size > limit) sites.take(limit) else sites
    val rows = shown.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (site in row) {
                    Box(modifier = Modifier.weight(1f)) {
                        SiteCard(backdrop, site, text, muted, line, card, { onOpen(site) }, { onDetail(site) })
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (onMore != null && sites.size > limit) {
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .border(1.5.dp, Color(0xFF6B7280), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp)).clickable { onMore() }
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("→ 查看更多", color = text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PillRow(
    items: List<String>, selected: Int, onSelect: (Int) -> Unit,
    tabBar: Color, card: Color, text: Color, muted: Color,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(tabBar, RoundedCornerShape(999.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items.size) { i ->
            val on = i == selected
            Box(
                modifier = Modifier
                    .background(if (on) card else Color.Transparent, RoundedCornerShape(999.dp))
                    .clickable { onSelect(i) }.padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                Text(items[i], color = if (on) text else muted, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun HomePage(
    backdrop: Backdrop, cats: List<SiteCategory>, favorites: Set<String>,
    text: Color, muted: Color, line: Color, tabBar: Color, accent: Color, card: Color, bg: Color,
    onCategory: (String) -> Unit, onPrompts: () -> Unit, onRecipes: () -> Unit, onSq: () -> Unit,
    onSettings: () -> Unit, onSite: (Site) -> Unit, onSiteDetail: (Site) -> Unit, onTheme: () -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var engineIdx by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var siteQuery by remember { mutableStateOf("") }
    var fanOpen by remember { mutableStateOf(false) }
    var qrTitle by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
    val dateText = "${today.monthValue}月${today.dayOfMonth}日$weekday"
    val total = cats.sumOf { it.sites.size }
    val searchResults = if (siteQuery.isBlank()) emptyList() else
        cats.flatMap { it.sites }.filter {
            it.title.contains(siteQuery, ignoreCase = true) || it.desc.contains(siteQuery, ignoreCase = true)
        }
    val favSites = cats.flatMap { it.sites }.filter { favorites.contains(it.url) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("懒得找了 · 免费资源导航", color = text, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(dateText, color = muted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { startApkDownload(context, APK_URL, "landezhaole-update.apk") }) { Text("下载 Android App", color = accent, fontSize = 12.sp) }
                    TextButton(onClick = onTheme) { Text("主题", color = text, fontSize = 12.sp) }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.logo), contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("懒得找了.", color = text, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier.border(1.5.dp, accent, RoundedCornerShape(10.dp))
                            .clickable { onPrompts() }.padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("ChatGPT玩法", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ENGINES.size) { i ->
                        val on = i == engineIdx
                        Box(
                            modifier = Modifier
                                .background(if (on) accent else Color.Transparent, RoundedCornerShape(999.dp))
                                .clickable { engineIdx = i }.padding(horizontal = 12.dp, vertical = 5.dp),
                        ) {
                            Text(ENGINES[i].name, color = if (on) Color.White else muted, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    BasicTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.weight(1f).background(card, RoundedCornerShape(12.dp))
                            .border(1.dp, line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 12.dp),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text(
                                if (engineIdx == 0) "搜本站资源，或切到必应 / 百度 / B站 / 网盘 / 谷歌搜全网…"
                                else if (ENGINES[engineIdx].id == "appdl") "搜应用名，直接下载 APK…" else "搜全网：${ENGINES[engineIdx].name}",
                                color = muted, fontSize = 13.sp, maxLines = 1,
                            )
                            inner()
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val q = query.trim()
                            if (q.isEmpty()) return@Button
                            val eng = ENGINES[engineIdx]
                            if (eng.url == null) siteQuery = q
                            else openUrl(context, eng.url + URLEncoder.encode(q, "UTF-8"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("搜索", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(14.dp))
                // 快捷图标行：前 10 个分类，点开进分类页（网站是弹层，App 里是分类页）
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(cats.take(10)) { cat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onCategory(cat.id) }) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape)
                                    .background(Color(avColorHex(cat.name)).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(cat.name.first().toString(), color = Color(avColorHex(cat.name)), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(cat.name.take(4), color = muted, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            if (siteQuery.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(18.dp))
                            .border(1.dp, line, RoundedCornerShape(18.dp)).padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                            Spacer(Modifier.width(8.dp))
                            Text("站内搜索结果", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("${searchResults.size} 个结果", color = muted, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { siteQuery = ""; query = "" }) { Text("清空搜索", color = accent, fontSize = 12.sp) }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (searchResults.isEmpty()) {
                            Text("没找到相关资源，换个关键词试试", color = muted, fontSize = 13.sp)
                        } else {
                            SectionGrid(backdrop, searchResults, text, muted, line, card, Int.MAX_VALUE, onSite, onSiteDetail, null)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (favSites.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(18.dp))
                            .border(1.dp, line, RoundedCornerShape(18.dp)).padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                            Spacer(Modifier.width(8.dp))
                            Text("我的收藏", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("${favSites.size} 个站点", color = muted, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        SectionGrid(backdrop, favSites, text, muted, line, card, Int.MAX_VALUE, onSite, onSiteDetail, null)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            items(cats.size) { ci ->
                val cat = cats[ci]
                var subIdx by remember(cat.id) { mutableStateOf(0) }
                val sub = cat.subs.getOrNull(subIdx)
                val filtered = if (sub == null || sub.id == "all") cat.sites else {
                    if (cat.id == "ai_api") {
                        val map = mapOf(
                            "official" to listOf("openai", "google", "anthropic", "aliyun", "baidu", "tencent", "volc", "zhipu", "moonshot", "deepseek", "minimax", "step", "xfyun", "huawei", "baichuan", "yi", "kunlun", "jimeng", "github", "perplexity"),
                            "aggregate" to listOf("openrouter", "groq", "together", "replicate", "huggingface", "api2d", "gptapi", "closeai", "pandora", "oneapi", "newapi"),
                            "cloud" to listOf("cloudflare", "azure", "aws", "gcp"),
                        )
                        cat.sites.filter { (map[sub.id] ?: emptyList()).contains(it.sub) }
                    } else cat.sites.filter { it.sub == sub.id }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(18.dp))
                        .border(1.dp, line, RoundedCornerShape(18.dp)).padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                        Spacer(Modifier.width(8.dp))
                        Text(cat.name, color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.background(tabBar, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 1.dp)) {
                            Text("${cat.sites.size} 个站点", color = muted, fontSize = 12.sp)
                        }
                    }
                    if (cat.desc.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(cat.desc, color = muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (cat.subs.isNotEmpty()) {
                        PillRow(cat.subs.map { it.name }, subIdx, { subIdx = it }, tabBar, card, text, muted)
                        Spacer(Modifier.height(12.dp))
                    }
                    SectionGrid(backdrop, filtered, text, muted, line, card, 7, onSite, onSiteDetail) {
                        onCategory(cat.id)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Text("本站为免费资源导航，所有外链归原站所有 · 链接失效欢迎反馈", color = muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text("© 2026 懒得找了 · 共 $total 个站点", color = muted, fontSize = 12.sp)
                    TextButton(onClick = onSettings) { Text("设置 / 检查更新", color = accent, fontSize = 12.sp) }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
        // 右侧悬浮栏：网站同款 ↑ 分享 省 食谱 ↓
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for ((label, act) in listOf(
                "↑" to { scope.launch { listState.scrollToItem(0) } },
                "分享" to {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "懒得找了 · 免费资源导航 $SITE_URL")
                    }
                    context.startActivity(Intent.createChooser(send, "分享本站"))
                },
                "省" to onSq,
                "食谱" to onRecipes,
                "↓" to { scope.launch { listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1) } },
            )) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(card)
                        .border(1.dp, line, CircleShape).clickable { act() },
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = if (label == "省") accent else text, fontSize = if (label == "分享") 11.sp else 14.sp, fontWeight = FontWeight.Bold) }
            }
        }
        // 左下 + 工具菜单：网站同款扇形菜单
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            if (fanOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((label, act) in listOf(
                        "关于网站" to { qrTitle = "关于网站" },
                        "微信打赏" to { qrTitle = "微信打赏" },
                        "QQ好友" to { qrTitle = "QQ好友" },
                        "支付宝打赏" to { qrTitle = "支付宝打赏" },
                        "最新Q群" to { openUrl(context, "https://qm.qq.com/q/vsaFRiPuO4") },
                    )) {
                        Button(onClick = { fanOpen = false; act() }, colors = ButtonDefaults.buttonColors(containerColor = card), shape = RoundedCornerShape(999.dp)) {
                            Text(label, color = text, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            FloatingActionButton(onClick = { fanOpen = !fanOpen }, containerColor = card, contentColor = accent) {
                Text(if (fanOpen) "×" else "＋", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        qrTitle?.let { title ->
            AlertDialog(
                onDismissRequest = { qrTitle = null },
                title = { Text(title, color = text, fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (title == "关于网站") {
                            Text("懒得找了 · 免费资源导航，共 $total 个站点。\n\n本站为免费资源导航，所有外链归原站所有。", color = muted, fontSize = 13.sp)
                        } else {
                            val file = when (title) {
                                "微信打赏" -> "qr-wechat.jpg"
                                "QQ好友" -> "qr-qq.jpg"
                                else -> "qr-alipay.jpg"
                            }
                            val bmp = remember(file) {
                                try {
                                    BitmapFactory.decodeStream(context.assets.open(file))
                                } catch (_: Exception) { null }
                            }
                            if (bmp != null) Image(bmp.asImageBitmap(), null, modifier = Modifier.size(200.dp), contentScale = ContentScale.Fit)
                            Text(
                                when (title) {
                                    "微信打赏" -> "微信扫一扫，支持一下"
                                    "QQ好友" -> "QQ扫一扫，加我好友"
                                    else -> "支付宝扫一扫，支持一下"
                                }, color = muted, fontSize = 13.sp,
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { qrTitle = null }) { Text("知道了", color = accent) } },
                containerColor = card,
            )
        }
    }
}

@Composable
private fun CategoryPage(
    backdrop: Backdrop, cat: SiteCategory, favorites: Set<String>,
    text: Color, muted: Color, line: Color, tabBar: Color, accent: Color, card: Color,
    onBack: () -> Unit, onSite: (Site) -> Unit, onSiteDetail: (Site) -> Unit, onToggleFav: (String) -> Unit,
) {
    var subIdx by remember { mutableStateOf(0) }
    val sub = cat.subs.getOrNull(subIdx)
    val filtered = if (sub == null || sub.id == "all") cat.sites else cat.sites.filter { it.sub == sub.id }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 回导航", color = accent, fontWeight = FontWeight.Bold) }
            Text(cat.name, color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("${cat.sites.size} 个站点", color = muted, fontSize = 12.sp)
        }
        if (cat.desc.isNotEmpty()) Text(cat.desc, color = muted, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
        Spacer(Modifier.height(12.dp))
        if (cat.subs.isNotEmpty()) PillRow(cat.subs.map { it.name }, subIdx, { subIdx = it }, tabBar, card, text, muted)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered.size) { i ->
                val site = filtered[i]
                val subName = cat.subs.firstOrNull { it.id == site.sub }?.name ?: ""
                GlassListCard(
                    backdrop, site, cat.name, subName, favorites.contains(site.url),
                    text, muted, accent, card,
                    { onSite(site) }, { onSiteDetail(site) }, { onToggleFav(site.url) },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PromptsPage(
    prompts: List<PromptCategory>, text: Color, muted: Color, line: Color, accent: Color, card: Color,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by remember { mutableStateOf("") }
    var catIdx by remember { mutableStateOf(0) }
    val cat = prompts.getOrNull(catIdx)
    val list = (cat?.prompts ?: emptyList()).filter {
        query.isBlank() || it.key.contains(query, ignoreCase = true) || it.desc.contains(query, ignoreCase = true)
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 回导航", color = accent, fontWeight = FontWeight.Bold) }
            Text("ChatGPT 玩法", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("${prompts.size} 类 / ${prompts.sumOf { it.prompts.size }} 条", color = muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(12.dp))
                .border(1.dp, line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 12.dp),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) Text("搜玩法名称或说明，点卡片复制", color = muted, fontSize = 13.sp)
                inner()
            },
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(prompts.size) { i ->
                val on = i == catIdx
                Box(
                    modifier = Modifier
                        .background(if (on) accent else Color.Transparent, RoundedCornerShape(999.dp))
                        .clickable { catIdx = i }.padding(horizontal = 12.dp, vertical = 5.dp),
                ) { Text(prompts[i].name, color = if (on) Color.White else muted, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list.size) { i ->
                val p = list[i]
                Column(
                    modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(14.dp))
                        .border(1.dp, line, RoundedCornerShape(14.dp)).clickable {
                            clipboard.setText(AnnotatedString(p.desc))
                            Toast.makeText(context, "玩法说明已复制", Toast.LENGTH_SHORT).show()
                        }.padding(14.dp),
                ) {
                    Text(p.key, color = text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(p.desc, color = muted, fontSize = 12.sp)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun RecipesPage(
    dishes: List<Dish>, text: Color, muted: Color, line: Color, tabBar: Color, accent: Color, card: Color,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by remember { mutableStateOf("") }
    var timeIdx by remember { mutableStateOf(0) }
    var catIdx by remember { mutableStateOf(0) }
    var detail by remember { mutableStateOf<Dish?>(null) }
    var shopping by remember { mutableStateOf<List<String>>(emptyList()) }
    var showShopping by remember { mutableStateOf(false) }
    val cats = listOf("全部") + dishes.map { it.cat }.distinct()
    val filtered = dishes.filter { d ->
        (catIdx == 0 || d.cat == cats[catIdx]) &&
        (timeIdx == 0 || d.minutes <= listOf(999, 15, 30, 60)[timeIdx]) &&
        (query.isBlank() || d.name.contains(query) || d.ingredients.any { it.contains(query) })
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 回导航", color = accent, fontWeight = FontWeight.Bold) }
            Text("食谱 · 今天吃什么", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (filtered.isNotEmpty()) detail = filtered.random()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(999.dp),
            ) { Text("随机一桌菜", color = Color.White, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(12.dp))
                .border(1.dp, line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 12.dp),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) Text("搜菜名或食材", color = muted, fontSize = 13.sp)
                inner()
            },
        )
        Spacer(Modifier.height(8.dp))
        PillRow(listOf("全部", "≤15分钟", "≤30分钟", "≤60分钟"), timeIdx, { timeIdx = it }, tabBar, card, text, muted)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cats.size) { i ->
                val on = i == catIdx
                Box(
                    modifier = Modifier
                        .background(if (on) accent else Color.Transparent, RoundedCornerShape(999.dp))
                        .clickable { catIdx = i }.padding(horizontal = 12.dp, vertical = 5.dp),
                ) { Text(cats[i], color = if (on) Color.White else muted, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text("${filtered.size} 道菜", color = muted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (shopping.isNotEmpty()) TextButton(onClick = { showShopping = true }) {
                Text("购物清单（${shopping.size}）", color = accent, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered.size) { i ->
                val d = filtered[i]
                Column(
                    modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(14.dp))
                        .border(1.dp, line, RoundedCornerShape(14.dp)).clickable { detail = d }.padding(14.dp),
                ) {
                    Text(d.name, color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("${d.cat} · ${d.cuisine} · ${d.minutes}分钟 · ${dishDifficulty(d)} · ${dishSpiciness(d)} · ${dishServings(d)}", color = muted, fontSize = 12.sp)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    detail?.let { d ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(d.name, color = text, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("${d.cat} · ${d.cuisine} · ${d.minutes}分钟 · ${dishDifficulty(d)} · ${dishSpiciness(d)} · ${dishServings(d)}（参考）", color = muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("食材", color = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(d.ingredients.joinToString("、"), color = text, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("步骤", color = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    d.steps.forEachIndexed { i, s -> Text("${i + 1}. $s", color = text, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    shopping = (shopping + d.ingredients).distinct()
                    Toast.makeText(context, "已加入购物清单", Toast.LENGTH_SHORT).show()
                }) { Text("加入购物清单", color = accent) }
            },
            dismissButton = {
                TextButton(onClick = { detail = null }) { Text("关闭", color = muted) }
            },
            containerColor = card,
        )
    }
    if (showShopping) {
        AlertDialog(
            onDismissRequest = { showShopping = false },
            title = { Text("购物清单", color = text, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    shopping.forEach { Text("· $it", color = text, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(shopping.joinToString("\n")))
                    Toast.makeText(context, "购物清单已复制", Toast.LENGTH_SHORT).show()
                    showShopping = false
                }) { Text("一键复制", color = accent) }
            },
            dismissButton = {
                TextButton(onClick = { shopping = emptyList(); showShopping = false }) { Text("清空", color = muted) }
            },
            containerColor = card,
        )
    }
}

@Composable
private fun SqPage(
    sq: SqData, text: Color, muted: Color, line: Color, tabBar: Color, accent: Color, card: Color,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var tabIdx by remember { mutableStateOf(0) }
    var filterIdx by remember { mutableStateOf(0) }
    val tab = sq.tabs.getOrNull(tabIdx)
    val filters = tab?.filters ?: emptyList()
    val filter = filters.getOrNull(filterIdx)?.first ?: "all"
    val list = sq.cards.filter { c ->
        c.tab == tabIdx && (filter == "all" || c.tagClasses.contains(filter))
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 回导航", color = accent, fontWeight = FontWeight.Bold) }
            Text("省钱小助手", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("领券 · 出行 · 生活", color = muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        if (tab != null) {
            PillRow(sq.tabs.map { it.name }, tabIdx, { tabIdx = it; filterIdx = 0 }, tabBar, card, text, muted)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filters.size) { i ->
                    val on = i == filterIdx
                    Box(
                        modifier = Modifier
                            .background(if (on) accent else Color.Transparent, RoundedCornerShape(999.dp))
                            .clickable { filterIdx = i }.padding(horizontal = 12.dp, vertical = 5.dp),
                    ) { Text(filters[i].second, color = if (on) Color.White else muted, fontSize = 12.sp) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list.size) { i ->
                val c = list[i]
                Column(
                    modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(14.dp))
                        .border(1.dp, line, RoundedCornerShape(14.dp)).clickable { openUrl(context, c.href) }.padding(14.dp),
                ) {
                    Text(c.title, color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(c.desc, color = muted, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        c.tags.forEach { tag ->
                            Box(Modifier.background(tabBar, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text(tag, color = muted, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("扫码领取 →", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Text("优惠金额与活动以各平台实际页面为准", color = muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 16.dp))
            }
        }
    }
}

@Composable
private fun FavoritesPage(
    backdrop: Backdrop, sites: List<Site>, favorites: Set<String>,
    text: Color, muted: Color, line: Color, accent: Color, card: Color,
    onBack: () -> Unit, onSite: (Site) -> Unit, onSiteDetail: (Site) -> Unit, onToggleFav: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 回导航", color = accent, fontWeight = FontWeight.Bold) }
            Text("我的收藏", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("${sites.size} 个站点", color = muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        if (sites.isEmpty()) {
            Text("还没有收藏，长按站点卡片可以收藏。", color = muted, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sites.size) { i ->
                    val site = sites[i]
                    GlassListCard(
                        backdrop, site, "", "", favorites.contains(site.url),
                        text, muted, accent, card,
                        { onSite(site) }, { onSiteDetail(site) }, { onToggleFav(site.url) },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    themeMode: Int, scale: Float, cats: List<SiteCategory>, text: Color, muted: Color, line: Color, accent: Color, card: Color,
    onTheme: (Int) -> Unit, onScale: (Float) -> Unit, onCheckUpdate: () -> Unit, onClearFav: () -> Unit, onBack: () -> Unit,
) {
    val context = LocalContext.current
    val total = cats.sumOf { it.sites.size }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 回导航", color = accent, fontWeight = FontWeight.Bold) }
            Text("设置", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text("主题", color = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        for ((i, label) in listOf("浅色", "深色", "跟随系统").withIndex()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(12.dp))
                    .border(1.dp, line, RoundedCornerShape(12.dp)).clickable { onTheme(i) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = text, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                if (themeMode == i) Text("✓", color = accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("界面缩放", color = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        for ((v, label) in listOf(0.5f to "50%", 0.75f to "75%", 1.0f to "100%", 1.25f to "125%")) {
            Row(
                modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(12.dp))
                    .border(1.dp, line, RoundedCornerShape(12.dp)).clickable { onScale(v) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = text, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                if (scale == v) Text("✓", color = accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
        Text("底栏不跟随缩放，字号始终正常。", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onCheckUpdate, colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text("检查更新", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onClearFav, colors = ButtonDefaults.buttonColors(containerColor = card), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text("清空收藏", color = text)
        }
        Spacer(Modifier.height(24.dp))
        Text("关于", color = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text("懒得找了 · 免费资源导航\nApp 版本 2.0.2（versionCode 7）\n网站版本 v1.1.51\n共 ${cats.size} 分类 / $total 个站点\n原生 Kotlin + Compose，无 WebView", color = muted, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
    }
}
