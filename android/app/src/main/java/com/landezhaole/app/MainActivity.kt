package com.landezhaole.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landezhaole.app.ui.RemoteVersion
import com.landezhaole.app.ui.ThemeAppearanceScreen
import com.landezhaole.app.ui.ThemeMode
import com.landezhaole.app.ui.UiZoom
import com.landezhaole.app.ui.UpdateDialog
import com.landezhaole.app.ui.cleanupUpdateLeftovers
import com.landezhaole.app.ui.currentVersionCode
import com.landezhaole.app.ui.fetchCloudCategories
import com.landezhaole.app.ui.fetchRemoteVersion
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SubTab(val id: String, val name: String)
data class SiteCard(val t: String, val u: String, val d: String, val b: String, val s: String, val f: String, val i: String)
data class Category(val id: String, val name: String, val desc: String, val subs: List<SubTab>, val cards: List<SiteCard>)
data class Engine(val id: String, val name: String, val url: String?)

// ---- 网站色板：与网站 :root / [data-theme="dark"] 一致 ----
// accent=绿，danger=红标题，surface=卡片底（网站 --bg），barBackground=区底（网站 --card）
data class AppPalette(
    val isDark: Boolean,
    val accent: Color,
    val accentSoft: Color,
    val danger: Color,
    val surface: Color,
    val foreground: Color,
    val secondary: Color,
    val barBackground: Color,
    val pageBackground: Color,
    val subtleFill: Color,
    val line: Color
)

val LightPalette = AppPalette(
    isDark = false,
    accent = Color(0xFF16A34A),
    accentSoft = Color(0x1416A34A),
    danger = Color(0xFFDC2626),
    surface = Color(0xFFF4F5F7),
    foreground = Color(0xFF1F2937),
    secondary = Color(0xFF6B7280),
    barBackground = Color(0xFFFFFFFF),
    pageBackground = Color(0xFFF4F5F7),
    subtleFill = Color(0x1F7F7F7F),
    line = Color(0xFFE5E7EB)
)
val DarkPalette = AppPalette(
    isDark = true,
    accent = Color(0xFF34D399),
    accentSoft = Color(0x1F34D399),
    danger = Color(0xFFF87171),
    surface = Color(0xFF0B1220),
    foreground = Color(0xFFF3F4F6),
    secondary = Color(0xFF9CA3AF),
    barBackground = Color(0xFF1F2937),
    pageBackground = Color(0xFF0B1220),
    subtleFill = Color(0x1F7F7F7F),
    line = Color(0xFF374151)
)

@Composable
fun GlassPanel(modifier: Modifier = Modifier, pal: AppPalette, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (pal.isDark) Color(0x26FFFFFF) else Color(0x8CFFFFFF))
    ) { content() }
}

// 网站药丸按钮：选中绿底白字加粗，未选中白底灰字细边
@Composable
fun PillChip(text: String, selected: Boolean, pal: AppPalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) pal.accent else pal.barBackground)
            .border(1.dp, if (selected) Color.Transparent else pal.line, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else pal.secondary
        )
    }
}

val ENGINES = listOf(
    Engine("site", "站内", null),
    Engine("bing", "必应", "https://cn.bing.com/search?q=%s"),
    Engine("baidu", "百度", "https://www.baidu.com/s?wd=%s"),
    Engine("bilibili", "B站", "https://search.bilibili.com/all?keyword=%s"),
    Engine("baidupan", "百度网盘", "https://wangpan.xin/search?keyword=%s"),
    Engine("alipan", "阿里云盘", "https://v2pan.com/search?q=%s"),
    Engine("quarkpan", "夸克网盘", "https://www.kkso.cc/search?kw=%s"),
    Engine("google", "谷歌", "https://www.google.com/search?q=%s"),
    Engine("appdl", "应用下载", null)
)

// 网站 AV_COLORS 原样
val AVATAR_COLORS = listOf(
    Color(0xFF16A34A), Color(0xFF0EA5E9), Color(0xFF8B5CF6), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFFEC4899), Color(0xFF14B8E6), Color(0xFFF97316)
)
fun avatarColor(t: String): Color {
    var h = 0
    for (c in t) h = (h * 31 + c.code)
    val idx = ((h % AVATAR_COLORS.size) + AVATAR_COLORS.size) % AVATAR_COLORS.size
    return AVATAR_COLORS[idx]
}
val RAINBOW = Brush.horizontalGradient(
    listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFEAB308), Color(0xFF22C55E), Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFFA855F7))
)

// 网站 AI_API_MAP 原样：AI API 分类的卡片厂商 id 映射到三个子 Tab
val AI_API_MAP = mapOf(
    "official" to setOf("openai", "google", "anthropic", "aliyun", "baidu", "tencent", "volc", "zhipu", "moonshot", "deepseek", "minimax", "step", "xfyun", "huawei", "baichuan", "yi", "kunlun", "jimeng", "github", "perplexity"),
    "aggregate" to setOf("openrouter", "groq", "together", "replicate", "huggingface", "api2d", "gptapi", "closeai", "pandora", "oneapi", "newapi"),
    "cloud" to setOf("cloudflare", "azure", "aws", "gcp")
)

// 卡片所属子分类名（用于搜索：搜“软件库”也能找到该子分类下的站）
fun subNameFor(cat: Category, card: SiteCard): String {
    if (cat.id == "ai_api") {
        for ((subId, vendors) in AI_API_MAP) {
            if (card.s in vendors) return cat.subs.firstOrNull { it.id == subId }?.name ?: ""
        }
        return ""
    }
    return cat.subs.firstOrNull { it.id == card.s }?.name ?: ""
}

fun hostOf(u: String): String {
    return try { URL(u).host.replaceFirst("^www\\.".toRegex(), "") } catch (e: Exception) { "" }
}

fun parseCategories(text: String): List<Category> {
    return try {
        val root = JSONObject(text)
        val cats = root.getJSONArray("cats")
        val out = mutableListOf<Category>()
        for (i in 0 until cats.length()) {
            val c = cats.getJSONObject(i)
            val subs = mutableListOf<SubTab>()
            val subsArr = c.optJSONArray("subs")
            if (subsArr != null) for (j in 0 until subsArr.length()) {
                val s = subsArr.getJSONObject(j)
                subs.add(SubTab(s.optString("id"), s.optString("name")))
            }
            val cards = mutableListOf<SiteCard>()
            val cardsArr = c.optJSONArray("cards")
            if (cardsArr != null) for (j in 0 until cardsArr.length()) {
                val s = cardsArr.getJSONObject(j)
                cards.add(SiteCard(s.optString("t"), s.optString("u"), s.optString("d"), s.optString("b"), s.optString("s"), s.optString("f"), s.optString("i")))
            }
            out.add(Category(c.optString("id"), c.optString("name"), c.optString("desc"), subs, cards))
        }
        out
    } catch (e: Exception) {
        emptyList()
    }
}
fun loadCategories(ctx: Context): List<Category> {
    return try {
        parseCategories(ctx.assets.open("data.json").bufferedReader().readText())
    } catch (e: Exception) {
        emptyList()
    }
}
fun openUrl(ctx: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        ctx.startActivity(intent)
    } catch (e: Exception) {
    }
}
fun todayKey(): String {
    val cal = java.util.Calendar.getInstance()
    return "%04d-%02d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
}
fun fetchBitmap(urlStr: String): Bitmap? {
    return try {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        if (conn.responseCode != 200) return null
        BitmapFactory.decodeStream(conn.inputStream)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun SiteIcon(card: SiteCard, size: Dp = 44.dp) {
    var bitmap by remember(card.u) { mutableStateOf<Bitmap?>(null) }
    var tried by remember(card.u) { mutableStateOf(false) }
    LaunchedEffect(card.u) {
        if (tried) return@LaunchedEffect
        tried = true
        val host = hostOf(card.u)
        val urls = mutableListOf<String>()
        if (card.i.isNotBlank()) urls.add(card.i)
        if (host.isNotBlank()) {
            urls.add("https://www.google.com/s2/favicons?domain=$host&sz=128")
            urls.add("https://$host/apple-touch-icon.png")
            urls.add("https://$host/favicon.ico")
        }
        val found = withContext(Dispatchers.IO) {
            var found: Bitmap? = null
            for (u in urls) {
                val bm = fetchBitmap(u)
                if (bm != null) {
                    if (u.contains("google.com/s2") && bm.width < 32) continue
                    found = bm
                    break
                }
            }
            found
        }
        bitmap = found
    }
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(11.dp))
            .background(if (bitmap != null) Color.White else avatarColor(card.t)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(if (size < 40.dp) 2.dp else 4.dp), contentScale = ContentScale.Fit)
        } else {
            val initials = (if (card.f.isNotBlank()) card.f else card.t).take(2)
            Text(initials, color = Color.White, fontSize = if (size < 40.dp) 10.sp else 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeCtx = LocalContext.current
            val themePrefs = remember { themeCtx.getSharedPreferences("landezhaole-theme", Context.MODE_PRIVATE) }
            var themeMode by remember { mutableStateOf(runCatching { ThemeMode.valueOf(themePrefs.getString("mode", "SYSTEM") ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)) }
            var uiScale by remember { mutableStateOf(themePrefs.getFloat("scale", 1f)) }
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val pal = if (dark) DarkPalette else LightPalette
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme(
                    primary = pal.accent, background = pal.pageBackground,
                    surface = pal.barBackground, onSurface = pal.foreground,
                    surfaceVariant = pal.subtleFill, onSurfaceVariant = pal.secondary
                ) else lightColorScheme(
                    primary = pal.accent, background = pal.pageBackground,
                    surface = pal.barBackground, onSurface = pal.foreground,
                    surfaceVariant = pal.subtleFill, onSurfaceVariant = pal.secondary
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.pageBackground) {
                    AppShell(
                        pal = pal, dark = dark,
                        onToggleDark = {
                            themeMode = if (dark) ThemeMode.LIGHT else ThemeMode.DARK
                            themePrefs.edit().putString("mode", themeMode.name).apply()
                        },
                        themeMode = themeMode,
                        onThemeModeChange = { themeMode = it; themePrefs.edit().putString("mode", it.name).apply() },
                        uiScale = uiScale,
                        onUiScaleChange = { uiScale = it; themePrefs.edit().putFloat("scale", it).apply() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(pal: AppPalette, dark: Boolean, onToggleDark: () -> Unit, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit, uiScale: Float, onUiScaleChange: (Float) -> Unit) {
    val ctx = LocalContext.current
    var categories by remember { mutableStateOf(loadCategories(ctx)) }
    var query by remember { mutableStateOf("") }
    var engineIdx by remember { mutableStateOf(0) }
    // 应用下载引擎状态：切到“应用下载”后点搜索走代理搜应用，结果在 App 内直接下、直接装
    var appResults by remember { mutableStateOf<List<AppEntry>?>(null) }
    var appTotal by remember { mutableStateOf(0) }
    var appLoading by remember { mutableStateOf(false) }
    var appError by remember { mutableStateOf<String?>(null) }
    var appQuery by remember { mutableStateOf("") }
    val appDlStates = remember { mutableStateMapOf<String, DlState>() }
    val appDlJobs = remember { mutableMapOf<String, Job>() }
    var toastMsg by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<RemoteVersion?>(null) }
    var newToast by remember { mutableStateOf<List<SiteCard>?>(null) }
    var newToastChecked by remember { mutableStateOf(false) }
    // Dock 直达某个子分类时用：Dock 点“软件库”会先把这项置为 soft_hub，再跳到资源网站区
    val forcedSub = remember { mutableStateMapOf<String, String>() }
    if (showTheme) {
        ThemeAppearanceScreen(pal = pal, mode = themeMode, onModeChange = onThemeModeChange, uiScale = uiScale, onUiScaleChange = onUiScaleChange, onBack = { showTheme = false })
        return
    }
    val clicks = remember { mutableStateMapOf<String, Int>() }
    val prefs = remember { ctx.getSharedPreferences("landezhaole-clicks", Context.MODE_PRIVATE) }
    remember {
        try {
            val raw = prefs.getString(todayKey(), "") ?: ""
            if (raw.isNotEmpty()) {
                val obj = JSONObject(raw)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    clicks[k] = obj.optInt(k)
                }
            }
        } catch (e: Exception) {
        }
        true
    }
    fun recordClick(url: String) {
        val n = (clicks[url] ?: 0) + 1
        clicks[url] = n
        try {
            val obj = JSONObject()
            for ((k, v) in clicks) obj.put(k, v)
            prefs.edit().putString(todayKey(), obj.toString()).apply()
        } catch (e: Exception) {
        }
    }
    fun seenNewUrls(): Set<String> {
        val raw = ctx.getSharedPreferences("landezhaole-seen-new", Context.MODE_PRIVATE)
            .getString("seen", "") ?: ""
        return raw.split("\n").filter { it.isNotBlank() }.toSet()
    }
    fun markNewSeen(urls: List<String>) {
        val merged = (seenNewUrls() + urls).joinToString("\n")
        ctx.getSharedPreferences("landezhaole-seen-new", Context.MODE_PRIVATE)
            .edit().putString("seen", merged).apply()
    }
    fun dismissNewToast() {
        newToast?.let { markNewSeen(it.map { c -> c.u }) }
        newToast = null
    }
    val listState = rememberLazyListState()
    LaunchedEffect(showToast) { if (showToast) { delay(1800); showToast = false } }
    LaunchedEffect(newToast) {
        if (newToast != null) {
            delay(3500)
            dismissNewToast()
        }
    }
    val scope = rememberCoroutineScope()
    val total = categories.sumOf { it.cards.size }
    val isSearching = query.isNotBlank() && engineIdx == 0
    val searchResults: List<SiteCard> = if (isSearching) {
        val low = query.trim().lowercase()
        val all = mutableListOf<SiteCard>()
        for (cat in categories) for (c in cat.cards) {
            val hay = (c.t + " " + c.d + " " + c.b + " " + c.u + " " + cat.name + " " + subNameFor(cat, c)).lowercase()
            if (hay.contains(low)) all.add(c)
        }
        fun rank(c: SiteCard): Int = when {
            c.b.contains("NEW", ignoreCase = true) || c.b.contains("新") -> 2
            c.b.contains("HOT", ignoreCase = true) -> 1
            else -> 0
        }
        all.sortedByDescending { rank(it) }
    } else emptyList()

    // 启动：清更新残留；版本检查与数据拉取并行（1.1.3 是数据拉完才查版本，raw 慢就收不到提醒）
    LaunchedEffect(Unit) {
        cleanupUpdateLeftovers(ctx)
        val versionJob = async { fetchRemoteVersion() }
        fetchCloudCategories()?.let { categories = it }
        if (!newToastChecked) {
            newToastChecked = true
            val seen = seenNewUrls()
            val news = categories.flatMap { it.cards }
                .filter { it.b.contains("NEW", ignoreCase = true) && it.u !in seen }
            if (news.isNotEmpty()) newToast = news
        }
        val remote = versionJob.await()
        if (remote != null && remote.versionCode > currentVersionCode(ctx)) {
            pendingUpdate = remote
        }
    }

    val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex >= 3 } }
    var headerShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { headerShown = true }
    val headerAlpha by animateFloatAsState(if (headerShown) 1f else 0f, animationSpec = tween(700))
    val density = LocalDensity.current
    val blueDot = if (pal.isDark) WebBlueDark else WebBlueLight
    val greenDarkText = if (pal.isDark) WebGreenDarkTextDark else WebGreenDarkTextLight

    fun jumpToCategory(catId: String) {
        val idx = categories.indexOfFirst { it.id == catId }
        if (idx < 0) return
        query = ""
        appResults = null
        scope.launch { listState.animateScrollToItem(idx + 1) }
    }
    fun manualCheckUpdate() {
        scope.launch {
            val remote = fetchRemoteVersion()
            when {
                remote == null -> { toastMsg = "检查失败，请稍后再试"; showToast = true }
                remote.versionCode > currentVersionCode(ctx) -> pendingUpdate = remote
                else -> { toastMsg = "已是最新版本"; showToast = true }
            }
        }
    }
    fun searchAppsNow(q: String) {
        appQuery = q
        appLoading = true
        appError = null
        scope.launch {
            try {
                val (total, list) = searchApps(q, 0)
                appResults = list
                appTotal = total
            } catch (e: Exception) {
                appError = "搜索失败：" + (e.message ?: "网络异常")
                appResults = emptyList()
            }
            appLoading = false
        }
    }
    fun loadMoreApps() {
        val current = appResults ?: return
        if (appLoading || current.size >= appTotal) return
        appLoading = true
        scope.launch {
            try {
                val (_, list) = searchApps(appQuery, current.size)
                appResults = current + list
            } catch (e: Exception) {
                appError = "加载更多失败：" + (e.message ?: "网络异常")
            }
            appLoading = false
        }
    }
    fun startAppDownload(entry: AppEntry) {
        if (appDlJobs[entry.appId]?.isActive == true) return
        appDlJobs[entry.appId] = scope.launch {
            appDlStates[entry.appId] = DlState.Downloading(0f, 0, entry.size)
            try {
                val file = downloadAppApk(ctx, entry) { w, t ->
                    appDlStates[entry.appId] =
                        DlState.Downloading(if (t > 0) w.toFloat() / t else 0f, w, t)
                }
                appDlStates[entry.appId] = DlState.Done(file)
            } catch (e: CancellationException) {
                appDlStates[entry.appId] = DlState.Idle
                try {
                    appdlFile(ctx, entry, true).delete()
                } catch (_: Exception) {
                }
            } catch (e: Exception) {
                appDlStates[entry.appId] = DlState.Failed(e.message ?: "下载失败")
            }
        }
    }
    val showAppResults = ENGINES[engineIdx].id == "appdl" && appResults != null

    Box(modifier = Modifier.fillMaxSize().background(pal.pageBackground)) {
        WebBlobs(pal = pal, modifier = Modifier.fillMaxSize())
        UiZoom(scale = uiScale) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    Column(
                        modifier = Modifier.graphicsLayer {
                            alpha = headerAlpha
                            translationY = (1 - headerAlpha) * 18 * density.density
                        }
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // 网站顶栏：左品牌，右日期 + 更新 + 夜间模式 + 主题
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "懒得找了 · 免费资源导航", fontSize = 12.sp, color = pal.secondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            val cal = java.util.Calendar.getInstance()
                            val weekdays = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                            Text(
                                "${cal.get(java.util.Calendar.MONTH) + 1}月${cal.get(java.util.Calendar.DAY_OF_MONTH)}日 ${weekdays[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]}",
                                fontSize = 12.sp, color = pal.secondary
                            )
                            IconButton(onClick = { manualCheckUpdate() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.SystemUpdate, contentDescription = "检查更新", tint = pal.foreground, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onToggleDark, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = "夜间模式", tint = pal.foreground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Button(
                                onClick = { showTheme = true },
                                colors = ButtonDefaults.buttonColors(containerColor = pal.subtleFill, contentColor = pal.foreground),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) { Text("主题", fontSize = 12.sp) }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        // 网站左上翻页时钟
                        FlipClock(pal = pal)
                        Spacer(modifier = Modifier.height(10.dp))
                        // 网站页头：居中图标 + 渐变文字 Logo（与网站同步，不用粒子）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "懒得找了",
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "懒得找了.",
                                style = TextStyle(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFF16A34A), Color(0xFF0EA5E9), Color(0xFF8B5CF6))
                                    ),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // 网站数据胶囊
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(pal.barBackground)
                                    .border(1.dp, pal.line, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 18.dp, vertical = 4.dp)
                            ) {
                                Text("共 $total 个精选站点", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = greenDarkText)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        // 网站搜索引擎：药丸（手机端横滑）+ 搜索行
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ENGINES.forEachIndexed { idx, e ->
                                PillChip(text = e.name, selected = idx == engineIdx, pal = pal, onClick = { engineIdx = idx })
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .background(pal.barBackground)
                                .border(1.dp, pal.line, RoundedCornerShape(14.dp))
                        ) {
                            Row(modifier = Modifier.height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                                    BasicTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 15.sp, color = pal.foreground),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner ->
                                            if (query.isEmpty()) {
                                                Text("搜本站，或切引擎搜全网", fontSize = 15.sp, color = pal.secondary)
                                            }
                                            inner()
                                        }
                                    )
                                }
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = ""; appResults = null }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Clear, contentDescription = "清空", tint = pal.secondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Button(
                                    onClick = {
                                        val q = query.trim()
                                        if (q.isEmpty()) return@Button
                                        val engine = ENGINES[engineIdx]
                                        if (engine.id == "appdl") {
                                            searchAppsNow(q)
                                        } else if (engine.url != null) {
                                            val target = String.format(engine.url, URLEncoder.encode(q, "UTF-8"))
                                            openUrl(ctx, target)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = Color.White),
                                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 14.dp, bottomEnd = 14.dp),
                                    modifier = Modifier.fillMaxHeight()
                                ) { Text("搜索", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Text(
                            if (ENGINES[engineIdx].id == "appdl") "应用下载：搜应用名，点下载直接装（不跳浏览器）"
                            else "站内搜索：标题 / 简介 / 标签匹配，NEW 置顶",
                            fontSize = 12.sp, color = pal.secondary,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // 网站快捷 Dock：鱼眼 + 名称提示，点一下跳到对应分类；最后是软件库直达
                        DockFishEye(
                            entries = categories.take(10).map { cat -> DockEntry(cat.name) { jumpToCategory(cat.id) } } +
                                DockEntry("软件库") { forcedSub["resources"] = "soft_hub"; jumpToCategory("resources") },
                            pal = pal
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                if (isSearching) {
                    item {
                        WebSection(pal = pal) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(blueDot).shadow(4.dp, CircleShape))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("站内搜索结果", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pal.danger, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(pal.subtleFill)
                                            .padding(horizontal = 10.dp, vertical = 1.dp)
                                    ) { Text("${searchResults.size} 个", fontSize = 12.sp, color = pal.secondary) }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { query = "" },
                                        colors = ButtonDefaults.buttonColors(containerColor = pal.barBackground, contentColor = pal.secondary),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 3.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) { Text("清空搜索", fontSize = 12.sp) }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (searchResults.isEmpty()) {
                                    Text("没找到相关资源，换个关键词试试", fontSize = 14.sp, color = pal.secondary)
                                } else {
                                    for (row in searchResults.chunked(4)) {
                                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            for (card in row) {
                                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                    WebCard(card, clicks[card.u] ?: 0, pal, onClick = { recordClick(card.u); openUrl(ctx, card.u) })
                                                }
                                            }
                                            repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else if (showAppResults) {
                    item {
                        AppResultsSection(
                            results = appResults!!, total = appTotal, loading = appLoading,
                            error = appError, hasMore = appResults!!.size < appTotal,
                            onLoadMore = { loadMoreApps() },
                            onClear = { query = ""; appResults = null },
                            pal = pal, dlStates = appDlStates,
                            onDownload = { startAppDownload(it) },
                            onCancel = { appDlJobs[it.appId]?.cancel() },
                            onInstall = { _, file -> installAppApk(ctx, file) }
                        )
                    }
                } else {
                    items(categories, key = { it.id }) { cat ->
                        var sub by remember(cat.id) { mutableStateOf(forcedSub[cat.id] ?: "all") }
                        LaunchedEffect(forcedSub[cat.id]) {
                            val f = forcedSub[cat.id]
                            if (f != null) { sub = f; forcedSub.remove(cat.id) }
                        }
                        val shown = when {
                            sub == "all" -> cat.cards
                            cat.id == "ai_api" -> cat.cards.filter { (AI_API_MAP[sub] ?: emptySet()).contains(it.s) }
                            else -> cat.cards.filter { it.s == sub }
                        }
                        WebSection(pal = pal) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(blueDot).shadow(4.dp, CircleShape))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(cat.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pal.danger, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(pal.subtleFill)
                                            .padding(horizontal = 10.dp, vertical = 1.dp)
                                    ) { Text("${cat.cards.size} 个站点", fontSize = 12.sp, color = pal.secondary) }
                                }
                                if (cat.desc.isNotBlank()) {
                                    Text(cat.desc, fontSize = 12.sp, color = pal.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    cat.subs.forEach { s ->
                                        PillChip(text = s.name, selected = sub == s.id, pal = pal, onClick = { sub = s.id })
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                for (row in shown.chunked(4)) {
                                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        for (card in row) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                WebCard(card, clicks[card.u] ?: 0, pal, onClick = { recordClick(card.u); openUrl(ctx, card.u) })
                                            }
                                        }
                                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("懒得找了", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pal.accent)
                        Text("本站为免费资源导航，所有外链归原站所有 · 链接失效欢迎反馈", fontSize = 12.sp, color = pal.secondary)
                        Text("共 $total 个精选站点 · 数据来自云端，启动时自动同步", fontSize = 12.sp, color = pal.secondary)
                    }
                    Spacer(modifier = Modifier.height(96.dp))
                }
            }
        }
        if (pendingUpdate != null) {
            UpdateDialog(version = pendingUpdate!!, pal = pal, onDismiss = { pendingUpdate = null })
        }
        if (newToast != null) {
            val news = newToast!!
            Box(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.barBackground)
                    .border(1.dp, Color(0xFF5EEAD4), RoundedCornerShape(12.dp))
                    .clickable {
                        openUrl(ctx, news[0].u)
                        dismissNewToast()
                    }
            ) {
                Row {
                    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(Color(0xFF5EEAD4)))
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF14B8A6), Color(0xFF0EA5E9)))),
                            contentAlignment = Alignment.Center
                        ) { Text("新", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("发现 ${news.size} 个新站点", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
                            Text(
                                news.joinToString("、") { it.t }, fontSize = 12.sp, color = pal.secondary,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { dismissNewToast() }, modifier = Modifier.size(28.dp)) {
                            Text("×", fontSize = 16.sp, color = pal.secondary)
                        }
                    }
                }
            }
        }
        if (showToast) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2B2B2B)).padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text(toastMsg, color = Color(0xFFFFFDF8), fontSize = 14.sp)
            }
        }
        if (showTop) {
            FloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 106.dp).size(48.dp),
                containerColor = pal.accent, contentColor = Color.White
            ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "回到顶部") }
        }
    }
}
