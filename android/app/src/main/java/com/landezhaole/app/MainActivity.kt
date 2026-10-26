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
import androidx.compose.material.icons.filled.ArrowDownward
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
