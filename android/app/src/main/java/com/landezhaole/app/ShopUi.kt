package com.landezhaole.app

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landezhaole.app.ui.ThemeAppearanceScreen
import com.landezhaole.app.ui.ThemeMode
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---- 电商风主色（按 HTML 原型，App 仍叫懒得找了） ----
val SHOP_RED = Color(0xFFF53D2D)
val SHOP_GRADIENT = Brush.horizontalGradient(listOf(Color(0xFFF53D2D), Color(0xFFFF7733)))

val CAT_EMOJI = mapOf(
    "cloud_browser" to "☁️", "ai_api" to "🔌", "ai_models" to "🤖", "ai_canvas" to "🎨",
    "ai_prompts" to "💬", "tools_magic" to "✨", "short_drama" to "🎬", "movie" to "🎥",
    "discounts" to "🛒", "resources" to "🧰", "music" to "🎵", "novel" to "📚",
    "game" to "🎮", "tools" to "🔧", "pc_setup" to "💻", "explore" to "🌍",
    "partners" to "🤝", "ai_bot_ecosystem" to "🌐", "coding_tools" to "⌨️",
    "haoyangmao" to "🎁", "ai_video_tools" to "🎞️"
)
val CAT_COLORS = listOf(
    Color(0xFFFFE9E6), Color(0xFFFFF0E8), Color(0xFFE8F4FF), Color(0xFFEAFAF1),
    Color(0xFFF3EAFE), Color(0xFFFEF3E2), Color(0xFFEEF2FF), Color(0xFFEAF9F6),
    Color(0xFFFFF5F5), Color(0xFFF0F7FF)
)

fun catEmoji(catId: String): String = CAT_EMOJI[catId] ?: "📦"

fun rankHot(card: SiteCard): Int = when {
    card.b.equals("NEW", ignoreCase = true) || card.b.contains("新") -> 2
    card.b.equals("HOT", ignoreCase = true) -> 1
    else -> 0
}

fun filterCardsForSub(cat: Category, subId: String): List<SiteCard> = when {
    subId == "all" -> cat.cards
    cat.id == "ai_api" -> cat.cards.filter { (AI_API_MAP[subId] ?: emptySet()).contains(it.s) }
    else -> cat.cards.filter { it.s == subId }
}

fun catNameByUrl(categories: List<Category>): Map<String, String> {
    val m = LinkedHashMap<String, String>()
    for (c in categories) for (card in c.cards) if (!m.containsKey(card.u)) m[card.u] = c.name
    return m
}

// ---- 猜你喜欢 / 分类页共用卡片：图标区 + 名 + 角标 + 分类名 + 去体验 + 星标 ----
@Composable
fun ShopCard(
    card: SiteCard,
    catName: String,
    pal: AppPalette,
    isFav: Boolean,
    onOpen: () -> Unit,
    onToggleFav: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(pal.barBackground)
            .border(1.dp, pal.line, RoundedCornerShape(12.dp))
            .clickable { onOpen() }
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(110.dp).background(pal.subtleFill),
                contentAlignment = Alignment.Center
            ) {
                SiteIcon(card, 56.dp)
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.TopStart) {
                    Text(
                        card.t, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pal.foreground,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (card.b.equals("HOT", ignoreCase = true) || card.b.equals("NEW", ignoreCase = true)) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFFFF6F5))
                                .border(1.dp, Color(0xFFFFC7BF), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(card.b, fontSize = 10.sp, color = SHOP_RED)
                        }
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFFFF6F5))
                            .border(1.dp, Color(0xFFFFC7BF), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("免费", fontSize = 10.sp, color = SHOP_RED)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(catName, fontSize = 11.sp, color = pal.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SHOP_RED)
                        .clickable { onOpen() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("去体验", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
                .clickable { onToggleFav() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isFav) "★" else "☆", fontSize = 16.sp,
                color = if (isFav) SHOP_RED else Color(0xFF999999)
            )
        }
    }
}

@Composable
fun ShopSectionTitle(title: String, small: String?, pal: AppPalette) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pal.foreground, modifier = Modifier.weight(1f))
        if (small != null) {
            Text(small, fontSize = 12.sp, color = pal.secondary)
        }
    }
}

private data class BannerSlide(val title: String, val sub: String, val btn: String, val c1: Color, val c2: Color)

@Composable
fun ShopBanner(total: Int, catCount: Int, couponCount: Int, onAction: (Int) -> Unit) {
    var idx by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            idx = (idx + 1) % 3
        }
    }
    val slides = listOf(
        BannerSlide("去水印神器", "站内搜“去水印”，一搜即达", "开始搜索", Color(0xFFFF512F), Color(0xFFDD2476)),
        BannerSlide("$total 站点全收录", "$catCount 大分类 · 一搜即达", "去逛逛", Color(0xFFF7971E), Color(0xFFFFD200)),
        BannerSlide("省钱小助手", "$couponCount 张领券", "去领券", Color(0xFF11998E), Color(0xFF38EF7D))
    )
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 12.dp)
            .height(150.dp).clip(RoundedCornerShape(12.dp))
    ) {
        slides.forEachIndexed { i, s ->
            val alpha by animateFloatAsState(if (i == idx) 1f else 0f, animationSpec = tween(500))
            Box(
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { this.alpha = alpha }
                    .background(Brush.horizontalGradient(listOf(s.c1, s.c2)))
                    .clickable(enabled = i == idx) { onAction(i) }
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(s.title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(s.sub, fontSize = 12.sp, color = Color.White.copy(alpha = 0.92f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White)
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(s.btn, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SHOP_RED)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slides.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .size(width = if (i == idx) 16.dp else 6.dp, height = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (i == idx) Color.White else Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun ShopHome(
    pal: AppPalette,
    categories: List<Category>,
    favs: Set<String>,
    onToggleFav: (String) -> Boolean,
    onOpenCard: (SiteCard) -> Unit,
    onOpenCategory: (String?) -> Unit,
    onOpenSave: () -> Unit,
    engineJump: String?,
    onEngineJumpConsumed: () -> Unit,
    onRecordDownload: (String) -> Unit,
    showToast: (String) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var engineIdx by remember { mutableStateOf(0) }
    var appResults by remember { mutableStateOf<List<AppEntry>?>(null) }
    var appTotal by remember { mutableStateOf(0) }
    var appLoading by remember { mutableStateOf(false) }
    var appError by remember { mutableStateOf<String?>(null) }
    var appQuery by remember { mutableStateOf("") }
    val appDlStates = remember { mutableStateMapOf<String, DlState>() }
    val appDlJobs = remember { mutableMapOf<String, Job>() }
    LaunchedEffect(engineJump) {
        if (engineJump != null) {
            val i = ENGINES.indexOfFirst { it.id == engineJump }
            if (i >= 0) engineIdx = i
            onEngineJumpConsumed()
        }
    }
    val total = categories.sumOf { it.cards.size }
    val isSearching = query.isNotBlank() && engineIdx == 0
    val searchResults: List<SiteCard> = if (isSearching) {
        val low = query.trim().lowercase()
        val all = mutableListOf<SiteCard>()
        for (cat in categories) for (c in cat.cards) {
            val hay = (c.t + " " + c.d + " " + c.b + " " + c.u + " " + cat.name + " " + subNameFor(cat, c)).lowercase()
            if (hay.contains(low)) all.add(c)
        }
        all.sortedByDescending { rankHot(it) }
    } else emptyList()
    val catByUrl = remember(categories) { catNameByUrl(categories) }
    val hotSites = remember(categories) {
        categories.flatMap { c -> c.cards.map { c to it } }
            .filter { it.second.b.equals("HOT", ignoreCase = true) || it.second.b.equals("NEW", ignoreCase = true) }
            .take(12)
    }
    val guess = remember(categories) {
        categories.flatMap { c -> c.cards.map { c to it } }
            .sortedByDescending { rankHot(it.second) }
            .take(20)
    }
    fun searchAppsNow(q: String) {
        appQuery = q
        appLoading = true
        appError = null
        scope.launch {
            try {
                val (t, list) = searchApps(q, 0)
                appResults = list
                appTotal = t
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
            appDlStates[entry.appId] = DlState.Downloading(0f, 0L, entry.size)
            try {
                val file = downloadAppApk(ctx, entry) { w, t ->
                    appDlStates[entry.appId] =
                        DlState.Downloading(if (t > 0) w.toFloat() / t else 0f, w, t)
                }
                appDlStates[entry.appId] = DlState.Done(file)
                onRecordDownload(entry.label)
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
    fun doSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        val engine = ENGINES[engineIdx]
        if (engine.id == "appdl") {
            searchAppsNow(q)
        } else if (engine.url != null) {
            val target = String.format(engine.url, URLEncoder.encode(q, "UTF-8"))
            openUrl(ctx, target)
        }
    }
    val showAppResults = ENGINES[engineIdx].id == "appdl" && appResults != null

    Column(modifier = Modifier.fillMaxSize().background(pal.pageBackground)) {
        // ---- 渐变顶栏：logo + 搜索框 + 引擎药丸 + 热词 ----
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(SHOP_GRADIENT)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "懒得找了", color = Color.White, fontSize = 17.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f).height(36.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔍", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 13.sp, color = pal.foreground),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (query.isEmpty()) {
                                            Text("搜本站资源", fontSize = 13.sp, color = Color(0xFF999999))
                                        }
                                        inner()
                                    }
                                )
                            }
                            if (query.isNotEmpty()) {
                                Text(
                                    "搜索", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SHOP_RED,
                                    modifier = Modifier.clickable { doSearch() }.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ENGINES.forEachIndexed { idx, e ->
                        val sel = idx == engineIdx
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                .background(if (sel) Color.White else Color.White.copy(alpha = 0.25f))
                                .clickable { engineIdx = idx }
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                e.name, fontSize = 12.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) SHOP_RED else Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("去水印", "AI绘画", "小游戏", "免费短剧", "壁纸", "工具").forEach { w ->
                        Text(
                            w, fontSize = 12.sp, color = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.clickable { query = w; engineIdx = 0 }
                        )
                    }
                }
            }
        }
        if (isSearching) {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "站内搜索结果", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = pal.foreground, modifier = Modifier.weight(1f)
                        )
                        Text("${searchResults.size} 个", fontSize = 12.sp, color = pal.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "清空搜索", fontSize = 12.sp, color = pal.secondary,
                            modifier = Modifier.clickable { query = "" }
                        )
                    }
                }
                if (searchResults.isEmpty()) {
                    item {
                        Text(
                            "没找到相关资源，换个关键词试试", fontSize = 14.sp, color = pal.secondary,
                            modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(searchResults.chunked(2)) { row ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (card in row) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ShopCard(
                                            card, catByUrl[card.u] ?: "", pal, card.u in favs,
                                            { onOpenCard(card) },
                                            {
                                                val nowFav = onToggleFav(card.u)
                                                showToast(if (nowFav) "已收藏 ⭐" else "已取消收藏")
                                            }
                                        )
                                    }
                                }
                                repeat(2 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        } else if (showAppResults) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
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
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                item {
                    ShopBanner(total, categories.size, COUPON_CARDS.size) { i ->
                        when (i) {
                            0 -> { query = "去水印"; engineIdx = 0 }
                            1 -> onOpenCategory(null)
                            else -> onOpenSave()
                        }
                    }
                }
                item {
                    Column {
                        ShopSectionTitle("站点分类", "${categories.size} 个分类", pal)
                        for (row in categories.chunked(5)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                row.forEachIndexed { ri, cat ->
                                    val globalIdx = categories.indexOf(cat)
                                    Column(
                                        modifier = Modifier.weight(1f)
                                            .clickable { onOpenCategory(cat.id) }
                                            .padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                                .background(CAT_COLORS[globalIdx % CAT_COLORS.size]),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(catEmoji(cat.id), fontSize = 24.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            cat.name.substringBefore(" · "), fontSize = 11.sp, color = pal.foreground,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center, lineHeight = 14.sp
                                        )
                                    }
                                }
                                repeat(5 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                item {
                    Column {
                        ShopSectionTitle("热门站点", "HOT / NEW", pal)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            hotSites.forEach { (_, card) ->
                                Column(
                                    modifier = Modifier.width(96.dp).clickable { onOpenCard(card) },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp))
                                            .background(pal.subtleFill),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SiteIcon(card, 48.dp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        card.t, fontSize = 12.sp, color = pal.foreground,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("免费", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SHOP_RED)
                                        if (card.b.equals("HOT", ignoreCase = true) || card.b.equals("NEW", ignoreCase = true)) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier.clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xFFFFF6F5))
                                                    .border(1.dp, Color(0xFFFFC7BF), RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(card.b, fontSize = 10.sp, color = SHOP_RED)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Column {
                        ShopSectionTitle("猜你喜欢", "为你推荐", pal)
                        for (row in guess.chunked(2)) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for ((cat, card) in row) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ShopCard(
                                                card, cat.name, pal, card.u in favs,
                                                { onOpenCard(card) },
                                                {
                                                    val nowFav = onToggleFav(card.u)
                                                    showToast(if (nowFav) "已收藏 ⭐" else "已取消收藏")
                                                }
                                            )
                                        }
                                    }
                                    repeat(2 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
                item {
                    Text(
                        "— 到底啦 —", fontSize = 12.sp, color = Color(0xFFBBBBBB),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ---- 分类页：左 21 分类 + 右子类 chips + 2 列站点卡（前 6 个） ----
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopCategory(
    pal: AppPalette,
    categories: List<Category>,
    favs: Set<String>,
    onToggleFav: (String) -> Boolean,
    onOpenCard: (SiteCard) -> Unit,
    catJump: String?,
    onCatJumpConsumed: () -> Unit,
    showToast: (String) -> Unit
) {
    var selIdx by remember { mutableStateOf(0) }
    var selSub by remember { mutableStateOf("all") }
    LaunchedEffect(catJump) {
        if (catJump != null) {
            val i = categories.indexOfFirst { it.id == catJump }
            if (i >= 0) {
                selIdx = i
                selSub = "all"
            }
            onCatJumpConsumed()
        }
    }
    if (categories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("数据加载中…", fontSize = 14.sp, color = pal.secondary)
        }
        return
    }
    val cat = categories[selIdx.coerceIn(0, categories.size - 1)]
    val subs = if (cat.subs.isEmpty()) listOf(SubTab("all", "全部")) else cat.subs
    val shown = filterCardsForSub(cat, selSub)
    Column(modifier = Modifier.fillMaxSize().background(pal.pageBackground)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(SHOP_GRADIENT)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text("站点分类", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier.width(96.dp).fillMaxHeight().background(pal.barBackground)
                    .verticalScroll(rememberScrollState())
            ) {
                categories.forEachIndexed { i, c ->
                    val sel = i == selIdx
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (sel) Color(0xFFFFF8F7) else Color.Transparent)
                            .clickable { selIdx = i; selSub = "all" }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.width(3.dp).height(48.dp)
                                    .background(if (sel) SHOP_RED else Color.Transparent)
                            )
                            Box(
                                modifier = Modifier.weight(1f).padding(vertical = 14.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    c.name.substringBefore(" · "), fontSize = 13.sp,
                                    color = if (sel) SHOP_RED else pal.secondary,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center, maxLines = 2, lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pal.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subs.forEach { s ->
                        val sel = selSub == s.id
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                .background(if (sel) SHOP_RED else pal.barBackground)
                                .border(1.dp, if (sel) SHOP_RED else pal.line, RoundedCornerShape(16.dp))
                                .clickable { selSub = s.id }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                s.name, fontSize = 12.sp,
                                color = if (sel) Color.White else pal.foreground
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("全部站点", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
                Spacer(modifier = Modifier.height(8.dp))
                if (shown.isEmpty()) {
                    Text("该分类下暂无站点", fontSize = 13.sp, color = pal.secondary)
                } else {
                    for (row in shown.chunked(2)) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (card in row) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ShopCard(
                                            card, cat.name, pal, card.u in favs,
                                            { onOpenCard(card) },
                                            {
                                                val nowFav = onToggleFav(card.u)
                                                showToast(if (nowFav) "已收藏 ⭐" else "已取消收藏")
                                            }
                                        )
                                    }
                                }
                                repeat(2 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---- 我的收藏：星标站点，复选 + 打开 + 删除选中 ----
@Composable
fun ShopFavorites(
    pal: AppPalette,
    categories: List<Category>,
    favs: Set<String>,
    onToggleFav: (String) -> Boolean,
    onOpenCard: (SiteCard) -> Unit,
    showToast: (String) -> Unit
) {
    val favCards = remember(categories, favs) {
        favs.mapNotNull { url ->
            categories.firstNotNullOfOrNull { c ->
                c.cards.firstOrNull { it.u == url }?.let { c.name to it }
            }
        }
    }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    val liveSelected = selected.intersect(favs)
    val allChecked = favCards.isNotEmpty() && liveSelected.size == favCards.size
    Column(modifier = Modifier.fillMaxSize().background(pal.pageBackground)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(pal.barBackground)
                .border(0.dp, Color.Transparent)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("我的收藏", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
        }
        if (favCards.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("还没有收藏，去首页点星标", fontSize = 14.sp, color = pal.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favCards, key = { it.second.u }) { (catName, card) ->
                    val checked = card.u in liveSelected
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(pal.barBackground)
                            .border(1.dp, pal.line, RoundedCornerShape(12.dp))
                            .clickable { onOpenCard(card) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(22.dp).clip(CircleShape)
                                .background(if (checked) SHOP_RED else Color.Transparent)
                                .border(2.dp, if (checked) SHOP_RED else Color(0xFFCCCCCC), CircleShape)
                                .clickable {
                                    selected = if (checked) liveSelected - card.u else liveSelected + card.u
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) {
                                Text("✓", fontSize = 14.sp, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        SiteIcon(card, 48.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                card.t, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pal.foreground,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(catName, fontSize = 11.sp, color = pal.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(28.dp).clickable {
                                onToggleFav(card.u)
                                showToast("已取消收藏")
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("★", fontSize = 18.sp, color = SHOP_RED)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(pal.barBackground)
                .border(1.dp, pal.line)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape)
                    .background(if (allChecked) SHOP_RED else Color.Transparent)
                    .border(2.dp, if (allChecked) SHOP_RED else Color(0xFFCCCCCC), CircleShape)
                    .clickable {
                        selected = if (allChecked) emptySet() else favCards.map { it.second.u }.toSet()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (allChecked) {
                    Text("✓", fontSize = 14.sp, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("全选", fontSize = 13.sp, color = pal.foreground)
            Spacer(modifier = Modifier.weight(1f))
            Text("共收藏 ${favCards.size} 个站点", fontSize = 13.sp, color = pal.foreground)
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(SHOP_RED)
                    .clickable {
                        if (liveSelected.isEmpty()) {
                            showToast("请先勾选要删除的收藏")
                        } else {
                            liveSelected.forEach { onToggleFav(it) }
                            selected = emptySet()
                            showToast("已删除 ${liveSelected.size} 个收藏")
                        }
                    }
                    .padding(horizontal = 26.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("删除选中", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ---- 消息页：全部 / 下载 / 系统，只用真实状态 ----
private data class ShopMsg(
    val icon: String,
    val title: String,
    val desc: String,
    val time: String,
    val isDownload: Boolean,
    val onClick: () -> Unit
)

@Composable
fun ShopMessages(
    pal: AppPalette,
    categories: List<Category>,
    downloads: List<DownloadRecord>,
    versionName: String,
    onCheckUpdate: () -> Unit,
    showToast: (String) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val total = categories.sumOf { it.cards.size }
    val newCards = remember(categories) {
        categories.flatMap { it.cards }.filter { it.b.equals("NEW", ignoreCase = true) }
    }
    val timeFmt = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }
    val systemMsgs = listOf(
        ShopMsg(
            "🆕", "新站点上新",
            if (newCards.isEmpty()) "暂无新站点" else "本周新增 ${newCards.size} 个站点：" + newCards.take(5).joinToString("、") { it.t },
            "", false
        ) { showToast(if (newCards.isEmpty()) "暂无新站点" else "本周新增 ${newCards.size} 个站点") },
        ShopMsg(
            "🔄", "当前版本",
            "当前版本 " + (if (versionName.isBlank()) "1.3.0" else versionName) + " · 点此检查更新",
            "", false
        ) { onCheckUpdate() },
        ShopMsg(
            "☁️", "数据同步",
            "已收录 ${categories.size} 分类 · $total 站点，启动时自动同步",
            "", false
        ) { showToast("数据已是最新") }
    )
    val downloadMsgs = downloads.map { rec ->
        ShopMsg(
            "📦", "应用下载完成", rec.label,
            timeFmt.format(Date(rec.time)), true
        ) { showToast("应用：" + rec.label + " · 下载于 " + timeFmt.format(Date(rec.time))) }
    }
    val shown = when (tab) {
        1 -> downloadMsgs
        2 -> systemMsgs
        else -> downloadMsgs + systemMsgs
    }
    Column(modifier = Modifier.fillMaxSize().background(pal.pageBackground)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(pal.barBackground)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("消息中心", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(pal.barBackground)
                .padding(horizontal = 12.dp)
        ) {
            listOf("全部", "下载", "系统").forEachIndexed { i, name ->
                val sel = tab == i
                Box(
                    modifier = Modifier.clickable { tab = i }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .border(
                            0.dp, Color.Transparent,
                            RoundedCornerShape(0.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            name, fontSize = 14.sp,
                            color = if (sel) SHOP_RED else Color(0xFF888888),
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier.width(24.dp).height(2.dp)
                                .background(if (sel) SHOP_RED else Color.Transparent)
                        )
                    }
                }
            }
        }
        if (shown.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (tab == 1) "暂无下载记录" else "暂无消息",
                    fontSize = 14.sp, color = pal.secondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shown) { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(pal.barBackground)
                            .border(1.dp, pal.line, RoundedCornerShape(12.dp))
                            .clickable { m.onClick() }
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(pal.subtleFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(m.icon, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    m.title, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = pal.foreground, modifier = Modifier.weight(1f)
                                )
                                if (m.time.isNotBlank()) {
                                    Text(m.time, fontSize = 11.sp, color = pal.secondary)
                                } else if (!m.isDownload) {
                                    Text("系统", fontSize = 11.sp, color = pal.secondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                m.desc, fontSize = 12.sp, color = pal.secondary,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---- 我的页：渐变头 + 真实数据格 + 功能格 + 菜单列 ----
@Composable
fun ShopMine(
    pal: AppPalette,
    dark: Boolean,
    onToggleDark: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit,
    todayClicks: Int,
    favCount: Int,
    totalSites: Int,
    catCount: Int,
    onOpenSave: () -> Unit,
    onOpenVideo: () -> Unit,
    onHomeAppDownload: () -> Unit,
    onCheckUpdate: () -> Unit,
    versionName: String,
    showToast: (String) -> Unit
) {
    val ctx = LocalContext.current
    var showTheme by remember { mutableStateOf(false) }
    var qrTitle by remember { mutableStateOf<String?>(null) }
    var qrAsset by remember { mutableStateOf("") }
    var qrHint by remember { mutableStateOf("") }
    var showAbout by remember { mutableStateOf(false) }
    if (showTheme) {
        ThemeAppearanceScreen(
            pal = pal, mode = themeMode, onModeChange = onThemeModeChange,
            uiScale = uiScale, onUiScaleChange = onUiScaleChange, onBack = { showTheme = false }
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().background(pal.pageBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(SHOP_GRADIENT)
                .padding(horizontal = 16.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "懒得找了",
                    modifier = Modifier.size(60.dp).clip(CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("懒得找了", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("免费资源导航", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(pal.barBackground)
                .border(1.dp, pal.line, RoundedCornerShape(12.dp))
                .padding(vertical = 6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                val stats = listOf(
                    Triple("$todayClicks", "今日点击", "今日点击 $todayClicks 次"),
                    Triple("$favCount", "我的收藏", "我的收藏 $favCount 个站点"),
                    Triple("$totalSites", "收录站点", "已收录 $totalSites 个站点"),
                    Triple("$catCount", "分类总数", "已收录 $catCount 大分类")
                )
                stats.forEach { (v, label, tip) ->
                    Column(
                        modifier = Modifier.weight(1f).clickable { showToast(tip) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(v, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(label, fontSize = 11.sp, color = pal.secondary)
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(pal.barBackground)
                .border(1.dp, pal.line, RoundedCornerShape(12.dp))
                .padding(vertical = 6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                val funcs = listOf(
                    Triple("💰", "省钱小助手", onOpenSave),
                    Triple("📲", "应用下载", onHomeAppDownload),
                    Triple("🔄", "检查更新", onCheckUpdate),
                    Triple(if (dark) "☀️" else "🌙", if (dark) "深色·开" else "深色·关", onToggleDark)
                )
                funcs.forEach { (emoji, label, action) ->
                    Column(
                        modifier = Modifier.weight(1f).clickable { action() }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(label, fontSize = 11.sp, color = pal.secondary)
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(pal.barBackground)
                .border(1.dp, pal.line, RoundedCornerShape(12.dp))
        ) {
            Column {
                MineRow(
                    icon = Icons.Filled.Person, iconTint = Color(0xFF7C6CF0), label = "关注我们",
                    pal = pal, onClick = { openUrl(ctx, "https://landezhaole.pages.dev/") }
                )
                Divider(color = pal.line)
                MineRow(
                    icon = Icons.Filled.Share, iconTint = Color(0xFFF5A623), label = "分享给朋友",
                    pal = pal,
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "懒得找了 · 免费资源导航 https://landezhaole.pages.dev/")
                            }
                            ctx.startActivity(Intent.createChooser(intent, "分享给朋友"))
                        } catch (e: Exception) {
                        }
                    }
                )
                Divider(color = pal.line)
                MineRow(
                    icon = Icons.Filled.Group, iconTint = Color(0xFF5B67F0), label = "添加粉丝群",
                    pal = pal, onClick = { openUrl(ctx, "https://qm.qq.com/q/vsaFRiPuO4") }
                )
                Divider(color = pal.line)
                MineRow(
                    icon = Icons.Filled.Movie, iconTint = Color(0xFFEC4899), label = "美女视频",
                    pal = pal, onClick = onOpenVideo
                )
                Divider(color = pal.line)
                MineRow(
                    icon = Icons.Filled.Chat, iconTint = Color(0xFF07C160), label = "微信打赏",
                    pal = pal,
                    onClick = { qrTitle = "微信打赏"; qrAsset = "qr-wechat.jpg"; qrHint = "微信扫一扫，支持一下" }
                )
                Divider(color = pal.line)
                MineRow(
                    icon = Icons.Filled.AccountCircle, iconTint = Color(0xFF12B7F5), label = "QQ好友",
                    pal = pal,
                    onClick = { qrTitle = "QQ好友"; qrAsset = "qr-qq.jpg"; qrHint = "QQ扫一扫，加我好友" }
                )
                Divider(color = pal.line)
                MineRow(
                    icon = Icons.Filled.Payment, iconTint = Color(0xFF1677FF), label = "支付宝打赏",
                    pal = pal,
                    onClick = { qrTitle = "支付宝打赏"; qrAsset = "qr-alipay.jpg"; qrHint = "支付宝扫一扫，支持一下" }
                )
                Divider(color = pal.line)
                MineRow(
                    icon = Icons.Filled.Info, iconTint = pal.secondary, label = "关于网站",
                    pal = pal, onClick = { showAbout = true }
                )
                Divider(color = pal.line)
                MineRow(
                    icon = Icons.Filled.Palette, iconTint = Color(0xFFEC4899), label = "主题",
                    pal = pal, onClick = { showTheme = true }
                )
            }
        }
        Text(
            "当前版本 " + (if (versionName.isBlank()) "1.3.0" else versionName),
            fontSize = 12.sp, color = pal.secondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
    }
    if (qrTitle != null) {
        QrDialog(title = qrTitle!!, asset = qrAsset, hint = qrHint, onDismiss = { qrTitle = null })
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}
