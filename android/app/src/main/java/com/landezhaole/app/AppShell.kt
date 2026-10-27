package com.landezhaole.app

import android.content.Context
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.landezhaole.app.ui.RemoteVersion
import com.landezhaole.app.ui.ThemeMode
import com.landezhaole.app.ui.UiZoom
import com.landezhaole.app.ui.UpdateDialog
import com.landezhaole.app.ui.cleanupUpdateLeftovers
import com.landezhaole.app.ui.currentVersionCode
import com.landezhaole.app.ui.fetchCloudCategories
import com.landezhaole.app.ui.fetchRemoteVersion
import org.json.JSONArray
import org.json.JSONObject

// 美女视频源：AIVideoHub_ 公开视频直链（X 原站播放，无平台水印；连续开关控制循环，切换视频切下一个）。
val VIDEO_URLS: List<String> = listOf(
    "https://video.twimg.com/amplify_video/2114645689422774272/vid/avc1/714x718/d0pWfM3dLa_SjNzQ.mp4?tag=29",
    "https://video.twimg.com/amplify_video/2114557244889235456/vid/avc1/720x1280/YZ8SJq1_-nouPp46.mp4?tag=29",
    "https://video.twimg.com/amplify_video/2114556951015383040/vid/avc1/720x1280/gieHyC-AKUrUU_Du.mp4?tag=29",
    "https://video.twimg.com/amplify_video/2114558765202821120/vid/avc1/720x1264/OYeN6NhsiVtpZShw.mp4?tag=29",
    "https://video.twimg.com/amplify_video/2114555877198016512/vid/avc1/720x1280/HovltJoJN3VwiqFo.mp4?tag=29",
    "https://video.twimg.com/amplify_video/2114165275474173952/vid/avc1/1080x1920/t2xwz2Us7rvGfWbS.mp4?tag=29"
)

// 省钱小助手已为底部 Tab；美女视频仍为独立页
enum class ShopSubPage { None, Save, Video }

// 独立页（不占 Tab）：分类详情 / 热门更多 / 猜你喜欢更多
sealed class ShopOverlay {
    data class Category(val id: String) : ShopOverlay()
    object Hot : ShopOverlay()
    object Guess : ShopOverlay()
}

// 应用下载记录：本次重做之后完成的才记，之前没有的不编造
data class DownloadRecord(val label: String, val time: Long)

fun loadDownloadRecords(ctx: Context): List<DownloadRecord> {
    return try {
        val raw = ctx.getSharedPreferences("landezhaole-downloads", Context.MODE_PRIVATE)
            .getString("records", "") ?: ""
        if (raw.isBlank()) return emptyList()
        val arr = JSONArray(raw)
        val out = mutableListOf<DownloadRecord>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(DownloadRecord(o.optString("n"), o.optLong("t")))
        }
        out.sortedByDescending { it.time }
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveDownloadRecords(ctx: Context, list: List<DownloadRecord>) {
    try {
        val arr = JSONArray()
        for (r in list) {
            val o = JSONObject()
            o.put("n", r.label)
            o.put("t", r.time)
            arr.put(o)
        }
        ctx.getSharedPreferences("landezhaole-downloads", Context.MODE_PRIVATE)
            .edit().putString("records", arr.toString()).apply()
    } catch (e: Exception) {
    }
}

@Composable
fun SubPageWrapper(
    pal: AppPalette,
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().pageBg(pal)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.clickable { onBack() }.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("‹ 返回", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
            }
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
    }
}

// 画廊主题的底部 Tab 栏：悬浮胶囊，形状/边框/阴影/选中 pill 全部跟主题走（默认主题仍用系统 NavigationBar）
@Composable
fun GalleryTabBar(pal: AppPalette, tab: Int, goTab: (Int) -> Unit) {
    val sty = styleOf(pal)
    Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .hardShadow(sty.tabBarShape, sty.tabBarHardShadow, sty.tabBarHardShadowColor)
                .shadow(sty.tabBarElevation, sty.tabBarShape)
                .clip(sty.tabBarShape)
                .background(sty.tabBarBg)
                .border(sty.tabBarBorderWidth, sty.tabBarBorderColor, sty.tabBarShape)
                .padding(6.dp)
        ) {
            listOf("🏠" to "首页", "🗂️" to "分类", "💰" to "省钱小助手", "👤" to "我的").forEachIndexed { i, (emoji, label) ->
                val sel = tab == i
                Box(
                    modifier = Modifier.weight(1f).clip(sty.tabShape)
                        .background(if (sel) sty.tabSelBg else Color.Transparent)
                        .clickable { goTab(i) }.padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 22.sp)
                        Text(label, fontSize = 11.sp, color = if (sel) sty.tabSelText else pal.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.ShopNavItem(
    pal: AppPalette,
    emoji: String,
    label: String,
    selected: Boolean,
    badge: Int?,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Box(contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 22.sp)
                if (badge != null) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-6).dp)
                            .size(16.dp).clip(CircleShape).background(shopBrand(pal)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (badge > 99) "99+" else badge.toString(),
                            color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = shopBrand(pal),
            selectedTextColor = shopBrand(pal),
            indicatorColor = shopBrand(pal).copy(alpha = 0.12f)
        )
    )
}

@Composable
fun AppShell(
    pal: AppPalette,
    dark: Boolean,
    onToggleDark: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeScheme: String,
    onThemeSchemeChange: (String) -> Unit,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var subPage by remember { mutableStateOf(ShopSubPage.None) }
    var overlay by remember { mutableStateOf<ShopOverlay?>(null) }
    var homeEngineJump by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf(emptyList<Category>()) }
    // 今日点击（landezhaole-clicks + todayKey，与旧版一致）
    val clicks = remember { mutableStateMapOf<String, Int>() }
    val clicksPrefs = remember { ctx.getSharedPreferences("landezhaole-clicks", Context.MODE_PRIVATE) }
    remember {
        try {
            val raw = clicksPrefs.getString(todayKey(), "") ?: ""
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
            clicksPrefs.edit().putString(todayKey(), obj.toString()).apply()
        } catch (e: Exception) {
        }
    }
    // 应用下载记录（本次重做之后完成才记）
    var downloads by remember { mutableStateOf(loadDownloadRecords(ctx)) }
    val recordDownload: (String) -> Unit = { label ->
        val next = listOf(DownloadRecord(label, System.currentTimeMillis())) + downloads
        downloads = next
        saveDownloadRecords(ctx, next)
    }
    // toast
    var toastMsg by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    LaunchedEffect(showToast) {
        if (showToast) {
            delay(1800)
            showToast = false
        }
    }
    val showToastMsg: (String) -> Unit = { m ->
        toastMsg = m
        showToast = true
    }
    // NEW 提示
    var newToast by remember { mutableStateOf<List<SiteCard>?>(null) }
    var newToastChecked by remember { mutableStateOf(false) }
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
    // 更新检查
    var pendingUpdate by remember { mutableStateOf<RemoteVersion?>(null) }
    val checkUpdate: () -> Unit = {
        scope.launch {
            val remote = fetchRemoteVersion()
            when {
                remote == null -> Toast.makeText(ctx, "检查失败，请稍后再试", Toast.LENGTH_SHORT).show()
                remote.versionCode > currentVersionCode(ctx) -> pendingUpdate = remote
                else -> Toast.makeText(ctx, "已是最新版本", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // 启动：清更新残留；版本检查与数据拉取并行
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
    val total = categories.sumOf { it.cards.size }
    val todayClicks = clicks.values.sum()
    val versionName = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }
    val onOpenCard: (SiteCard) -> Unit = { c ->
        recordClick(c.u)
        openUrl(ctx, c.u)
    }
    val goTab: (Int) -> Unit = { t ->
        tab = t
        subPage = ShopSubPage.None
        overlay = null
    }

    Box(modifier = Modifier.fillMaxSize().pageBg(pal)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
                UiZoom(scale = uiScale) {
                    when {
                        subPage == ShopSubPage.Save -> SubPageWrapper(pal, "省钱小助手", { subPage = ShopSubPage.None }) {
                            SaveScreen(pal = pal)
                        }
                        subPage == ShopSubPage.Video -> SubPageWrapper(pal, "美女视频", { subPage = ShopSubPage.None }) {
                            VideoScreen(pal = pal, onBackHome = { subPage = ShopSubPage.None })
                        }
                        overlay != null -> {
                            val ov = overlay!!
                            val ovTitle = when (ov) {
                                is ShopOverlay.Category -> categories.firstOrNull { it.id == ov.id }?.name?.substringBefore(" · ") ?: "分类"
                                ShopOverlay.Hot -> "热门站点"
                                ShopOverlay.Guess -> "猜你喜欢"
                            }
                            SubPageWrapper(pal, ovTitle, { overlay = null }) {
                                when (ov) {
                                    is ShopOverlay.Category -> {
                                        val cat = categories.firstOrNull { it.id == ov.id }
                                        if (cat == null) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("分类不存在", fontSize = 14.sp, color = pal.secondary)
                                            }
                                        } else {
                                            ShopCategoryDetail(pal = pal, cat = cat, onOpenCard = onOpenCard)
                                        }
                                    }
                                    ShopOverlay.Hot -> {
                                        val hotAll = remember(categories) {
                                            categories.flatMap { c -> c.cards.map { c.name to it } }
                                                .filter { it.second.b.equals("HOT", ignoreCase = true) || it.second.b.equals("NEW", ignoreCase = true) }
                                        }
                                        ShopMoreList(pal = pal, sites = hotAll, onOpenCard = onOpenCard)
                                    }
                                    ShopOverlay.Guess -> {
                                        val guessAll = remember(categories) {
                                            categories.flatMap { c -> c.cards.map { c.name to it } }
                                                .sortedByDescending { rankHot(it.second) }
                                        }
                                        ShopMoreList(pal = pal, sites = guessAll, onOpenCard = onOpenCard)
                                    }
                                }
                            }
                        }
                        tab == 0 -> ShopHome(
                            pal = pal, dark = dark, categories = categories,
                            onOpenCard = onOpenCard,
                            onOpenCategory = { id ->
                                if (id == null) {
                                    tab = 1
                                } else {
                                    overlay = ShopOverlay.Category(id)
                                }
                            },
                            onOpenHotMore = { overlay = ShopOverlay.Hot },
                            onOpenGuessMore = { overlay = ShopOverlay.Guess },
                            onOpenSave = { subPage = ShopSubPage.Save },
                            engineJump = homeEngineJump,
                            onEngineJumpConsumed = { homeEngineJump = null },
                            onRecordDownload = recordDownload,
                            showToast = showToastMsg
                        )
                        tab == 1 -> ShopCategory(
                            pal = pal, categories = categories,
                            onOpenCategory = { id -> overlay = ShopOverlay.Category(id) }
                        )

                        tab == 2 -> SaveScreen(pal = pal)
                        else -> ShopMine(
                            pal = pal, dark = dark, onToggleDark = onToggleDark,
                            themeMode = themeMode, onThemeModeChange = onThemeModeChange,
                            themeScheme = themeScheme, onThemeSchemeChange = onThemeSchemeChange,
                            uiScale = uiScale, onUiScaleChange = onUiScaleChange,
                            todayClicks = todayClicks,
                            totalSites = total, catCount = categories.size,
                            onOpenSave = { subPage = ShopSubPage.Save },
                            onOpenVideo = { subPage = ShopSubPage.Video },
                            onHomeAppDownload = { homeEngineJump = "appdl"; tab = 0 },
                            onCheckUpdate = checkUpdate, versionName = versionName,
                            showToast = showToastMsg
                        )
                    }
                }
            }
            if (isGallerySchemePal(pal)) {
                GalleryTabBar(pal = pal, tab = tab, goTab = { goTab(it) })
            } else {
                NavigationBar(
                    containerColor = pal.barBackground,
                    contentColor = pal.foreground
                ) {
                    ShopNavItem(pal, "🏠", "首页", tab == 0, null) { goTab(0) }
                    ShopNavItem(pal, "🗂️", "分类", tab == 1, null) { goTab(1) }
                    ShopNavItem(pal, "💰", "省钱小助手", tab == 2, null) { goTab(2) }
                    ShopNavItem(pal, "👤", "我的", tab == 3, null) { goTab(3) }
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
                Row(modifier = Modifier.height(IntrinsicSize.Max)) {
                    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(Color(0xFF5EEAD4)))
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(Color(0xFF14B8A6), Color(0xFF0EA5E9))
                                    )
                                ),
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
                        Box(
                            modifier = Modifier.size(28.dp).clickable { dismissNewToast() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("×", fontSize = 16.sp, color = pal.secondary)
                        }
                    }
                }
            }
        }
        if (showToast) {
            Box(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Color(0xFF2B2B2B))
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text(toastMsg, color = Color(0xFFFFFDF8), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SaveScreen(pal: AppPalette) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var filter by remember { mutableStateOf("all") }
    val shown = COUPON_CARDS.filter { c ->
        c.tab == tab && (filter == "all" || c.tagClasses.contains(filter))
    }
    Column(modifier = Modifier.fillMaxSize().pageBg(pal)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("省钱小助手", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
            Spacer(modifier = Modifier.width(8.dp))
            Text("领券 · 出行 · 生活", fontSize = 13.sp, color = pal.secondary)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            COUPON_TABS.forEachIndexed { i, name ->
                val selected = i == tab
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max).clickable { tab = i; filter = "all" },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(if (selected) 4.dp else 0.dp, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp), clip = false)
                            .background(if (selected) pal.barBackground else Color.Transparent, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .padding(horizontal = 15.dp, vertical = 10.dp)
                    ) {
                        Text(
                            name, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = if (selected) Color(0xFF283593) else pal.foreground
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(12.dp)
                            .background(if (selected) pal.barBackground else Color.Transparent)
                    )
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .shadow(6.dp, RoundedCornerShape(12.dp), clip = false)
                .background(pal.barBackground, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                COUPON_FILTERS[tab].forEach { (id, label) ->
                    val selected = filter == id
                    Box(
                        modifier = Modifier.clickable { filter = id }
                            .padding(vertical = 13.dp)
                            .padding(end = 10.dp)
                    ) {
                        Box {
                            if (selected) {
                                Box(
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .background(Color(0xFFF6DC3E), RoundedCornerShape(3.dp))
                                )
                            }
                            Text(
                                label, fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color(0xFF283593) else pal.foreground
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(shown, key = { it.title + it.url }) { c ->
                CouponCardView(card = c, pal = pal, onClick = { openUrl(ctx, c.url) })
            }
        }
    }
}

@Composable
fun CouponCardView(card: CouponCard, pal: AppPalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(pal.barBackground)
            .border(1.dp, pal.line, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(avatarColor(card.platform)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        card.platform.take(1), color = Color.White,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    card.platform, fontSize = 12.sp, color = pal.secondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                card.title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = pal.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                card.desc, fontSize = 13.sp, color = pal.secondary,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                card.tags.take(3).forEach { t ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(pal.subtleFill)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(t, fontSize = 11.sp, color = pal.secondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    drawLine(
                        color = Color(0xFFF1F2F3),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                }
                Box(
                    modifier = Modifier.size(16.dp).align(Alignment.CenterStart)
                        .offset(x = (-24).dp)
                        .clip(CircleShape).background(pal.pageBackground)
                )
                Box(
                    modifier = Modifier.size(16.dp).align(Alignment.CenterEnd)
                        .offset(x = 24.dp)
                        .clip(CircleShape).background(pal.pageBackground)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "领取优惠券", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF283593), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun VideoScreen(pal: AppPalette, onBackHome: () -> Unit) {
    var continuous by remember { mutableStateOf(true) }
    var videoIndex by remember { mutableStateOf(0) }
    var hint by remember { mutableStateOf("") }
    val url = VIDEO_URLS.getOrNull(videoIndex)
    Column(modifier = Modifier.fillMaxSize().pageBg(pal)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(pal.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(pal.barBackground)
                    .border(1.dp, pal.line, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("热舞视频", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            if (url != null) {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(Uri.parse(url))
                            setOnPreparedListener { mp ->
                                mp.isLooping = continuous
                                start()
                            }
                        }
                    },
                    onRelease = { it.stopPlayback() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("视频源待接入", fontSize = 15.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "把视频链接发来即可接上播放",
                        fontSize = 12.sp, color = Color(0xFFBBBBBB)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VideoButton(
                text = if (continuous) "连续: 开" else "连续: 关",
                pal = pal,
                modifier = Modifier.weight(1f),
                onClick = { continuous = !continuous }
            )
            VideoButton(
                text = "切换视频",
                pal = pal,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (VIDEO_URLS.isEmpty()) {
                        hint = "暂无视频源，请先添加视频链接"
                    } else {
                        videoIndex = (videoIndex + 1) % VIDEO_URLS.size
                        hint = ""
                    }
                }
            )
            VideoButton(
                text = "返回主页",
                pal = pal,
                accent = true,
                modifier = Modifier.weight(1f),
                onClick = onBackHome
            )
        }
        if (hint.isNotBlank()) {
            Text(
                hint, fontSize = 13.sp, color = pal.secondary,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun VideoButton(
    text: String,
    pal: AppPalette,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = pal.barBackground,
            contentColor = if (accent) pal.accent else pal.foreground
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.line)
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QrDialog(title: String, url: String, hint: String, onDismiss: () -> Unit) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) { fetchBitmap(url)?.asImageBitmap() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val bm = bitmap
                if (bm != null) {
                    Image(bitmap = bm, contentDescription = title, modifier = Modifier.size(240.dp))
                } else {
                    Text("二维码加载中…", fontSize = 14.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(hint, fontSize = 14.sp, color = Color.Gray)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } }
    )
}

@Composable
fun AboutDialog(catCount: Int, totalSites: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于网站") },
        text = {
            Text("懒得找了 · 免费资源导航\n已收录 $catCount 大分类 · $totalSites 个精选站点，一搜即达。\n本站为免费资源导航，所有外链归原站所有 · 链接失效欢迎反馈。")
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } }
    )
}

@Composable
fun MineRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    pal: AppPalette,
    onClick: () -> Unit,
    trailing: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, fontSize = 16.sp, color = pal.foreground, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, fontSize = 13.sp, color = pal.secondary)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(">", fontSize = 18.sp, color = pal.secondary)
    }
}
