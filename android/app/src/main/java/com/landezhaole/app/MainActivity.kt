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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landezhaole.app.ui.DoodleUpdateDialog
import com.landezhaole.app.ui.ThemeAppearanceScreen
import com.landezhaole.app.ui.ThemeMode
import com.landezhaole.app.ui.UiZoom
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SubTab(val id: String, val name: String)
data class SiteCard(val t: String, val u: String, val d: String, val b: String, val s: String, val f: String, val i: String)
data class Category(val id: String, val name: String, val desc: String, val subs: List<SubTab>, val cards: List<SiteCard>)
data class Engine(val id: String, val name: String, val url: String?)

// ---- Theme translated from the Flutter palette.dart + glass.dart in the user's zip ----
// Light/dark palettes are copied 1:1 from palette.dart. GlassPanel follows glass.dart:
// superellipse-ish 20dp corner, translucent white fill, deliberately NO shadow and NO
// blur (the original notes: shadows bridge the 12dp card gaps into dark bands, and
// blur over a smooth gradient background changes almost nothing per frame).
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
    val subtleFill: Color
)

val LightPalette = AppPalette(
    isDark = false,
    accent = Color(0xFF1257C9),
    accentSoft = Color(0x141257C9),
    danger = Color(0xFFC0392B),
    surface = Color(0x12000000),
    foreground = Color(0xFF1B2430),
    secondary = Color(0xFF6E7887),
    barBackground = Color(0xE6F2F2F7),
    pageBackground = Color(0xFFCDDCDC),
    subtleFill = Color(0x14000000)
)
val DarkPalette = AppPalette(
    isDark = true,
    accent = Color(0xFF5AA9FF),
    accentSoft = Color(0x1F5AA9FF),
    danger = Color(0xFFFF7B72),
    surface = Color(0x1FFFFFFF),
    foreground = Color(0xFFF5F7FA),
    secondary = Color(0xFFADB7C5),
    barBackground = Color(0xE61C1C1E),
    pageBackground = Color(0xFF434343),
    subtleFill = Color(0x1FFFFFFF)
)

@Composable
fun GlassPanel(modifier: Modifier = Modifier, pal: AppPalette, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (pal.isDark) Color(0x26FFFFFF) else Color(0x8CFFFFFF))
    ) { content() }
}

@Composable
fun PillChip(text: String, selected: Boolean, pal: AppPalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) pal.accentSoft else pal.subtleFill)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) pal.accent else pal.secondary)
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
    Engine("google", "谷歌", "https://www.google.com/search?q=%s")
)

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
fun hostOf(u: String): String {
    return try { URL(u).host.replaceFirst("^www\\.".toRegex(), "") } catch (e: Exception) { "" }
}
fun loadCategories(ctx: Context): List<Category> {
    return try {
        val text = ctx.assets.open("data.json").bufferedReader().readText()
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
fun SiteIcon(card: SiteCard) {
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
                    // Google's 16px globe fallback is not a real logo; skip it to the next source.
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
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(11.dp))
            .background(if (bitmap != null) Color.White else avatarColor(card.t)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
        } else {
            val initials = (if (card.f.isNotBlank()) card.f else card.t).take(2)
            Text(initials, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    HomeScreen(
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

@Composable
fun HomeScreen(pal: AppPalette, dark: Boolean, onToggleDark: () -> Unit, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit, uiScale: Float, onUiScaleChange: (Float) -> Unit) {
    val ctx = LocalContext.current
    val categories = remember { loadCategories(ctx) }
    var query by remember { mutableStateOf("") }
    var engineIdx by remember { mutableStateOf(0) }
    var dockCat by remember { mutableStateOf<String?>(null) }
    var showUpdate by remember { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
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
    val listState = rememberLazyListState()
    LaunchedEffect(showToast) { if (showToast) { delay(1800); showToast = false } }
    val scope = rememberCoroutineScope()
    val total = categories.sumOf { it.cards.size }
    val isSearching = query.isNotBlank() && engineIdx == 0
    val searchResults: List<SiteCard> = if (isSearching) {
        val low = query.trim().lowercase()
        val all = mutableListOf<SiteCard>()
        for (cat in categories) for (c in cat.cards) {
            val hay = (c.t + " " + c.d + " " + c.b + " " + c.u + " " + cat.name).lowercase()
            if (hay.contains(low)) all.add(c)
        }
        fun rank(c: SiteCard): Int = when {
            c.b.equals("NEW", ignoreCase = true) || c.b.contains("新") -> 2
            c.b.equals("HOT", ignoreCase = true) -> 1
            else -> 0
        }
        all.sortedByDescending { rank(it) }
    } else emptyList()
    val shownCategories = when {
        dockCat != null -> categories.filter { it.id == dockCat }
        else -> categories
    }
    Box(modifier = Modifier.fillMaxSize().background(pal.pageBackground)) {
        UiZoom(scale = uiScale) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.ic_app_logo), contentDescription = "懒得找了",
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("懒得找了", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
                        Text("共 $total 个精选站点", fontSize = 12.sp, color = pal.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { showUpdate = true }) {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = "检查更新", tint = pal.foreground)
                    }
                    IconButton(onClick = onToggleDark) {
                        Icon(if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode, contentDescription = "夜间模式", tint = pal.foreground)
                    }
                    Button(
                        onClick = { showTheme = true },
                        colors = ButtonDefaults.buttonColors(containerColor = pal.subtleFill, contentColor = pal.foreground),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("主题", fontSize = 12.sp) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ENGINES.forEachIndexed { idx, e ->
                        PillChip(text = e.name, selected = idx == engineIdx, pal = pal, onClick = { engineIdx = idx })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                GlassPanel(modifier = Modifier.fillMaxWidth(), pal = pal) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("搜本站，或切引擎搜全网", fontSize = 13.sp, color = pal.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "清空", tint = pal.secondary) }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val q = query.trim()
                                if (q.isEmpty()) return@Button
                                val engine = ENGINES[engineIdx]
                                if (engine.url != null) {
                                    val target = String.format(engine.url, URLEncoder.encode(q, "UTF-8"))
                                    openUrl(ctx, target)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) { Icon(Icons.Filled.Search, contentDescription = "搜索") }
                    }
                }
                Text(
                    "站内搜索：标题 / 简介 / 标签匹配，NEW 置顶",
                    fontSize = 11.sp, color = pal.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp).clickable { dockCat = null }) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(pal.subtleFill)
                                .then(if (dockCat == null) Modifier.border(2.dp, pal.accent, RoundedCornerShape(14.dp)) else Modifier),
                            contentAlignment = Alignment.Center
                        ) { Text("全", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pal.foreground) }
                        Text("全部", fontSize = 10.sp, color = pal.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                    categories.take(10).forEach { cat ->
                        val selected = dockCat == cat.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(84.dp).clickable { dockCat = if (selected) null else cat.id }
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(avatarColor(cat.name))
                                    .then(if (selected) Modifier.border(2.dp, pal.accent, RoundedCornerShape(14.dp)) else Modifier),
                                contentAlignment = Alignment.Center
                            ) { Text(cat.name.trim().firstOrNull()?.toString() ?: "?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            Text(cat.name.split("·")[0].trim(), fontSize = 10.sp, color = pal.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (isSearching) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("搜索结果（${searchResults.size}）", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.foreground, modifier = Modifier.weight(1f))
                        Button(onClick = { query = "" }, colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = Color.White), shape = RoundedCornerShape(16.dp)) { Text("清空搜索", fontSize = 12.sp) }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (searchResults.isEmpty()) {
                        Text("没找到相关资源，换个关键词试试", fontSize = 14.sp, color = pal.secondary)
                    }
                }
                items(searchResults.chunked(2)) { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (card in row) {
                            Box(modifier = Modifier.weight(1f)) { SiteCardView(card, clicks[card.u] ?: 0, pal, onClick = { recordClick(card.u); openUrl(ctx, card.u) }) }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                items(shownCategories, key = { it.id }) { cat ->
                    CategorySection(cat, pal, onCardClick = { card -> recordClick(card.u); openUrl(ctx, card.u) }, clicks = clicks)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
        }
        if (showUpdate) {
            DoodleUpdateDialog(onClose = { showUpdate = false }, onToast = { toastMsg = it; showToast = true })
        }
        if (showToast) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2B2B2B)).padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text(toastMsg, color = Color(0xFFFFFDF8), fontSize = 14.sp)
            }
        }
        SmallFloatingActionButton(
            onClick = { scope.launch { listState.scrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = pal.accent, contentColor = Color.White
        ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "回到顶部") }
    }
}

@Composable
fun CategorySection(cat: Category, pal: AppPalette, onCardClick: (SiteCard) -> Unit, clicks: Map<String, Int>) {
    var sub by remember { mutableStateOf("all") }
    val shown = if (sub == "all") cat.cards else cat.cards.filter { it.s == sub }
    GlassPanel(modifier = Modifier.fillMaxWidth(), pal = pal) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pal.accent))
                Spacer(modifier = Modifier.width(8.dp))
                Text(cat.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.foreground, modifier = Modifier.weight(1f))
                Text("${cat.cards.size} 个站点", fontSize = 11.sp, color = pal.secondary)
            }
            if (cat.desc.isNotBlank()) {
                Text(cat.desc, fontSize = 12.sp, color = pal.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip(text = "全部", selected = sub == "all", pal = pal, onClick = { sub = "all" })
                cat.subs.filter { it.id != "all" }.forEach { s ->
                    PillChip(text = s.name, selected = sub == s.id, pal = pal, onClick = { sub = s.id })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            for (row in shown.chunked(2)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (card in row) {
                        Box(modifier = Modifier.weight(1f)) { SiteCardView(card, clicks[card.u] ?: 0, pal, onClick = { onCardClick(card) }) }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun SiteCardView(card: SiteCard, todayClicks: Int, pal: AppPalette, onClick: () -> Unit) {
    val label = if (card.b.isNotBlank()) card.b else "精选"
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (pal.isDark) pal.surface else Color.White)
            .border(1.dp, pal.subtleFill, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SiteIcon(card)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.t, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pal.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.background(RAINBOW, RoundedCornerShape(999.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                Text(card.d, fontSize = 11.sp, color = pal.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (todayClicks > 0) {
                    Text("今日点击 $todayClicks 次", fontSize = 10.sp, color = pal.danger)
                }
            }
        }
    }
}
