package com.landezhaole.glasstest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

// 懒得找了 · 液态玻璃测试版 v1.1.0
// 液态玻璃：Kyant0/AndroidLiquidGlass Backdrop 2.0.1，rememberLayerBackdrop +
//   layerBackdrop（背景层）+ drawBackdrop（玻璃组件），vibrancy + blur + lens。
// 数据：CF Pages 项目 landezhaole 的 data.json 原样内置，21 分类 / 2603 站点 / 174 NEW。
// 主题：landezhaole.pages.dev 当前的 Libraries 浅深两套，翻译成 Kotlin 颜色，
//   浅：底 #fdfdfd / 文 #0d0d0d / 次 #6c6c6c / 强调 #0073e5
//   深：底 #121212 / 文 #ffffff / 次 #cacaca / 强调 #55cfff
// 原生 Kotlin + Compose，没有 WebView，没有 HTML。设置用 SharedPreferences 持久化。

private object Libraries {
    val lightBg = Color(0xFFFDFDFD)
    val lightText = Color(0xFF0D0D0D)
    val lightMuted = Color(0xFF6C6C6C)
    val lightAccent = Color(0xFF0073E5)
    val darkBg = Color(0xFF121212)
    val darkText = Color(0xFFFFFFFF)
    val darkMuted = Color(0xFFCACACA)
    val darkAccent = Color(0xFF55CFFF)
}

private const val PREFS = "glass_test_prefs"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GlassTestApp() }
    }

    @Composable
    private fun GlassTestApp() {
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
        val cats = remember { loadSiteData(context) }
        val systemDark = isSystemInDarkTheme()

        // ---- 设置（持久化，界面缩放默认 50%，按用户要求）----
        var themeMode by remember { mutableStateOf(prefs.getInt("theme_mode", 0)) } // 0浅 1深 2跟随系统
        var scale by remember { mutableStateOf(prefs.getFloat("scale", 0.5f)) } // 界面缩放，默认 50%
        var bottomStyle by remember { mutableStateOf(prefs.getInt("bottom_style", 0)) } // 0液态玻璃 1磨砂 2纯色
        var showDesc by remember { mutableStateOf(prefs.getBoolean("show_desc", true)) }
        var showBadge by remember { mutableStateOf(prefs.getBoolean("show_badge", true)) }
        var sortByName by remember { mutableStateOf(prefs.getBoolean("sort_name", false)) }
        var favorites by remember {
            mutableStateOf(prefs.getStringSet("fav_urls", emptySet())?.toSet() ?: emptySet())
        }

        fun saveTheme(v: Int) { themeMode = v; prefs.edit().putInt("theme_mode", v).apply() }
        fun saveScale(v: Float) { scale = v; prefs.edit().putFloat("scale", v).apply() }
        fun saveBottom(v: Int) { bottomStyle = v; prefs.edit().putInt("bottom_style", v).apply() }
        fun saveShowDesc(v: Boolean) { showDesc = v; prefs.edit().putBoolean("show_desc", v).apply() }
        fun saveShowBadge(v: Boolean) { showBadge = v; prefs.edit().putBoolean("show_badge", v).apply() }
        fun saveSort(v: Boolean) { sortByName = v; prefs.edit().putBoolean("sort_name", v).apply() }
        fun toggleFav(url: String) {
            favorites = if (favorites.contains(url)) favorites - url else favorites + url
            prefs.edit().putStringSet("fav_urls", favorites).apply()
        }
        fun clearFavs() { favorites = emptySet(); prefs.edit().putStringSet("fav_urls", emptySet()).apply() }

        val isDark = when (themeMode) { 1 -> true; 2 -> systemDark; else -> false }
        val bg = if (isDark) Libraries.darkBg else Libraries.lightBg
        val text = if (isDark) Libraries.darkText else Libraries.lightText
        val muted = if (isDark) Libraries.darkMuted else Libraries.lightMuted
        val accent = if (isDark) Libraries.darkAccent else Libraries.lightAccent

        // ---- 浏览状态 ----
        var page by remember { mutableStateOf(0) } // 0首页 1收藏 2设置
        var query by remember { mutableStateOf("") }
        var catIndex by remember { mutableStateOf(-1) }
        var subId by remember { mutableStateOf("all") }
        var badgeFilter by remember { mutableStateOf("") } // ""全部 NEW HOT
        var detail by remember { mutableStateOf<Pair<String, Site>?>(null) }

        val totalSites = remember(cats) { cats.sumOf { it.sites.size } }
        val totalNew = remember(cats) { cats.sumOf { c -> c.sites.count { it.badge == "NEW" } } }
        val totalHot = remember(cats) { cats.sumOf { c -> c.sites.count { it.badge == "HOT" } } }

        val shown: List<Pair<String, Site>> = remember(cats, query, catIndex, subId, badgeFilter, page, favorites, sortByName) {
            val q = query.trim()
            val result = ArrayList<Pair<String, Site>>()
            val pool = when {
                page == 1 -> cats // 收藏：先全量再按收藏过滤
                q.isNotEmpty() -> cats
                catIndex >= 0 && catIndex < cats.size -> listOf(cats[catIndex])
                else -> cats
            }
            for (cat in pool) {
                for (site in cat.sites) {
                    if (page == 1 && !favorites.contains(site.url)) continue
                    if (q.isEmpty() && page != 1) {
                        if (badgeFilter.isNotEmpty() && site.badge != badgeFilter) continue
                        if (catIndex >= 0 && subId != "all" && site.sub != subId) continue
                    }
                    if (q.isNotEmpty()) {
                        val hit = site.title.contains(q, ignoreCase = true) ||
                            site.desc.contains(q, ignoreCase = true) ||
                            site.url.contains(q, ignoreCase = true)
                        if (!hit) continue
                    }
                    result.add(cat.name to site)
                }
            }
            if (sortByName) result.sortBy { it.second.title }
            result
        }

        val backdrop = rememberLayerBackdrop()
        val clipboard = LocalClipboardManager.current
        val baseDensity = LocalDensity.current
        // 界面缩放：按比例缩放 dp 与 sp，50% 时一屏能看更多内容，玻璃模糊也同步缩放。
        CompositionLocalProvider(
            LocalDensity provides Density(baseDensity.density * scale, baseDensity.fontScale * scale)
        ) {
            Box(Modifier.fillMaxSize()) {
                // 背景层：必须带 layerBackdrop，玻璃才能采样它
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    Box(Modifier.fillMaxSize().background(bg))
                    Blob(Color(0xFF0073E5), 0.05f, 0.02f, 320.dp)
                    Blob(Color(0xFF55CFFF), 0.62f, 0.18f, 280.dp)
                    Blob(Color(0xFFF59E0B), 0.18f, 0.55f, 300.dp)
                    Blob(Color(0xFF7C4DFF), 0.60f, 0.72f, 260.dp)
                }

                if (page == 2) {
                    SettingsPage(
                        backdrop = backdrop, text = text, muted = muted, accent = accent,
                        themeMode = themeMode, onTheme = ::saveTheme,
                        scale = scale, onScale = ::saveScale,
                        bottomStyle = bottomStyle, onBottom = ::saveBottom,
                        showDesc = showDesc, onShowDesc = ::saveShowDesc,
                        showBadge = showBadge, onShowBadge = ::saveShowBadge,
                        sortByName = sortByName, onSort = ::saveSort,
                        favCount = favorites.size, onClearFavs = ::clearFavs,
                        totalCats = cats.size, totalSites = totalSites, totalNew = totalNew,
                        onBack = { page = 0 },
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item { Spacer(Modifier.height(48.dp)) }
                        item {
                            GlassCard(backdrop, Modifier.fillMaxWidth()) {
                                Txt("懒得找了 · 玻璃测试", size = 22, bold = true, color = text)
                                Spacer(Modifier.height(4.dp))
                                Txt("Backdrop 2.0.1 真液态玻璃 · 数据来自 CF 项目 landezhaole", color = muted)
                                Spacer(Modifier.height(8.dp))
                                Txt(
                                    "${cats.size} 个分类 · $totalSites 个站点 · $totalNew 个 NEW · $totalHot 个 HOT · ${favorites.size} 个收藏 · v1.1.0",
                                    color = muted,
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GlassButton(backdrop, "界面 ${ (scale * 100).toInt() }%", accent, solid = true) { page = 2 }
                                    GlassButton(backdrop, if (isDark) "深色 ✓" else "浅色 ✓", accent) {
                                        saveTheme(if (isDark) 0 else 1)
                                    }
                                }
                            }
                        }
                        item {
                            // 搜索框：玻璃胶囊，搜站点名 / 介绍 / 链接，搜索时跨全部分类
                            Box(
                                Modifier.fillMaxWidth()
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { Capsule() },
                                        effects = {
                                            vibrancy()
                                            blur(4f.dp.toPx())
                                            lens(12f.dp.toPx(), 24f.dp.toPx())
                                        },
                                        highlight = { Highlight.Plain },
                                    )
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) {
                                        BasicTextField(
                                            value = query,
                                            onValueChange = { query = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = TextStyle(color = text, fontSize = 15.sp),
                                            decorationBox = { inner ->
                                                Box {
                                                    inner()
                                                    if (query.isEmpty()) {
                                                        BasicText(
                                                            "搜站点名 / 介绍 / 链接…",
                                                            style = TextStyle(color = muted, fontSize = 15.sp),
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                    }
                                    if (query.isNotEmpty()) {
                                        Txt("✕", size = 16, bold = true, color = accent)
                                        Spacer(Modifier.width(4.dp))
                                        Box(Modifier.clickable { query = "" }) {
                                            Txt("清空", size = 14, color = accent)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            // 分类横向玻璃胶囊
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    GlassButton(backdrop, if (catIndex == -1) "全部 ✓" else "全部", accent, solid = catIndex == -1) {
                                        catIndex = -1; subId = "all"; page = 0
                                    }
                                }
                                items(cats.size) { i ->
                                    val short = cats[i].name.substringBefore(" ·")
                                    GlassButton(backdrop, if (catIndex == i) "$short ✓" else short, accent, solid = catIndex == i) {
                                        catIndex = i; subId = "all"; page = 0
                                    }
                                }
                            }
                        }
                        if (catIndex >= 0 && cats[catIndex].subs.isNotEmpty()) {
                            item {
                                // 子类筛选：数据里的 subs，全部 + 各子类
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(cats[catIndex].subs.size) { j ->
                                        val sub = cats[catIndex].subs[j]
                                        GlassButton(backdrop, if (subId == sub.id) "${sub.name} ✓" else sub.name, accent, solid = subId == sub.id) {
                                            subId = sub.id
                                        }
                                    }
                                }
                            }
                            item {
                                if (cats[catIndex].desc.isNotBlank()) Txt(cats[catIndex].desc, color = muted)
                            }
                        }
                        item {
                            // 角标筛选
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassButton(backdrop, if (badgeFilter.isEmpty()) "角标：全部 ✓" else "角标：全部", accent, solid = badgeFilter.isEmpty()) { badgeFilter = "" }
                                GlassButton(backdrop, if (badgeFilter == "NEW") "只看 NEW ✓" else "只看 NEW", accent, solid = badgeFilter == "NEW") { badgeFilter = "NEW" }
                                GlassButton(backdrop, if (badgeFilter == "HOT") "只看 HOT ✓" else "只看 HOT", accent, solid = badgeFilter == "HOT") { badgeFilter = "HOT" }
                                GlassButton(backdrop, if (sortByName) "按名称 ✓" else "默认顺序", accent, solid = sortByName) { saveSort(!sortByName) }
                            }
                        }
                        item {
                            Txt(
                                when {
                                    page == 1 -> "收藏 · ${shown.size} 条，点卡片看详情"
                                    query.isNotEmpty() -> "搜索“$query” · ${shown.size} 条（全部结果，不截断）"
                                    catIndex >= 0 -> "${cats[catIndex].name} · ${shown.size} 条"
                                    else -> "全部 · ${shown.size} 条（不截断，滑到底就是全部）"
                                },
                                color = muted,
                            )
                        }
                        items(shown.size) { i ->
                            val (catName, site) = shown[i]
                            val isFav = favorites.contains(site.url)
                            GlassCard(backdrop, Modifier.fillMaxWidth(), onClick = { detail = catName to site }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(36.dp).clip(CircleShape)
                                            .background(accent.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Txt(site.title.take(1).ifBlank { "站" }, size = 16, bold = true, color = accent)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Txt(site.title, size = 16, bold = true, color = text, maxLines = 1)
                                        if (showDesc && site.desc.isNotBlank()) Txt(site.desc, color = muted, maxLines = 2)
                                    }
                                    if (showBadge && site.badge.isNotBlank()) {
                                        Spacer(Modifier.width(8.dp))
                                        Txt(site.badge, size = 12, bold = true, color = accent)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.clickable { toggleFav(site.url) }.padding(4.dp)) {
                                        Txt(if (isFav) "★" else "☆", size = 20, bold = true, color = accent)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Txt(site.url, size = 12, color = accent, maxLines = 1)
                                Txt(catName, size = 12, color = muted, maxLines = 1)
                            }
                        }
                        item { Spacer(Modifier.height(120.dp)) }
                    }
                }

                // 底部栏：整宽玻璃底栏（效果样式在设置里切），四个入口
                BottomBar(
                    backdrop = backdrop, accent = accent, text = text, muted = muted,
                    isDark = isDark,
                    style = bottomStyle, page = page,
                    onHome = { page = 0; catIndex = -1; subId = "all" },
                    onFav = { page = 1 },
                    onNew = { page = 0; badgeFilter = if (badgeFilter == "NEW") "" else "NEW" },
                    onSettings = { page = 2 },
                    newActive = badgeFilter == "NEW" && page == 0,
                )

                // 站点详情弹窗：打开 / 复制链接 / 分享 / 收藏
                detail?.let { (catName, site) ->
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                            .clickable { detail = null },
                        contentAlignment = Alignment.Center,
                    ) {
                        GlassCard(backdrop, Modifier.fillMaxWidth().padding(horizontal = 24.dp), onClick = {}) {
                            Txt(site.title, size = 20, bold = true, color = text)
                            Spacer(Modifier.height(6.dp))
                            if (site.desc.isNotBlank()) Txt(site.desc, color = muted)
                            Spacer(Modifier.height(8.dp))
                            Txt(site.url, size = 13, color = accent)
                            Txt(catName, size = 12, color = muted)
                            if (site.badge.isNotBlank()) Txt("角标：${site.badge}", size = 12, color = accent)
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassButton(backdrop, "打开", accent, solid = true) {
                                    if (site.url.isNotBlank()) {
                                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(site.url))) } catch (_: Exception) {}
                                    }
                                    detail = null
                                }
                                GlassButton(backdrop, "复制链接", accent) {
                                    clipboard.setText(AnnotatedString(site.url))
                                    detail = null
                                }
                                GlassButton(backdrop, "分享", accent) {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${site.title} ${site.url}")
                                    }
                                    try { context.startActivity(Intent.createChooser(send, "分享站点")) } catch (_: Exception) {}
                                    detail = null
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassButton(backdrop, if (favorites.contains(site.url)) "已收藏 ★" else "收藏 ☆", accent, solid = favorites.contains(site.url)) {
                                    toggleFav(site.url)
                                }
                                GlassButton(backdrop, "关闭", accent) { detail = null }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsPage(
        backdrop: LayerBackdrop, text: Color, muted: Color, accent: Color,
        themeMode: Int, onTheme: (Int) -> Unit,
        scale: Float, onScale: (Float) -> Unit,
        bottomStyle: Int, onBottom: (Int) -> Unit,
        showDesc: Boolean, onShowDesc: (Boolean) -> Unit,
        showBadge: Boolean, onShowBadge: (Boolean) -> Unit,
        sortByName: Boolean, onSort: (Boolean) -> Unit,
        favCount: Int, onClearFavs: () -> Unit,
        totalCats: Int, totalSites: Int, totalNew: Int,
        onBack: () -> Unit,
    ) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(48.dp)) }
            item {
                GlassCard(backdrop, Modifier.fillMaxWidth()) {
                    Txt("设置", size = 22, bold = true, color = text)
                    Spacer(Modifier.height(4.dp))
                    Txt("改完立即生效，退出再进也保留", color = muted)
                    Spacer(Modifier.height(12.dp))
                    GlassButton(backdrop, "← 回首页", accent, solid = true) { onBack() }
                }
            }
            item {
                GlassCard(backdrop, Modifier.fillMaxWidth()) {
                    Txt("主题", size = 16, bold = true, color = text)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(backdrop, if (themeMode == 0) "浅色 ✓" else "浅色", accent, solid = themeMode == 0) { onTheme(0) }
                        GlassButton(backdrop, if (themeMode == 1) "深色 ✓" else "深色", accent, solid = themeMode == 1) { onTheme(1) }
                        GlassButton(backdrop, if (themeMode == 2) "跟随系统 ✓" else "跟随系统", accent, solid = themeMode == 2) { onTheme(2) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Txt("浅深两套都取自 landezhaole.pages.dev 的 Libraries 主题", color = muted)
                }
            }
            item {
                GlassCard(backdrop, Modifier.fillMaxWidth()) {
                    Txt("界面缩放", size = 16, bold = true, color = text)
                    Spacer(Modifier.height(4.dp))
                    Txt("当前 ${(scale * 100).toInt()}% · 字、卡片、间距都按这个比例缩放", color = muted)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f).forEach { v ->
                            GlassButton(backdrop, if (scale == v) "${(v * 100).toInt()}% ✓" else "${(v * 100).toInt()}%", accent, solid = scale == v) { onScale(v) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Txt("50% 时一屏能看更多站点和分类；觉得小了随时切回 100%", color = muted)
                }
            }
            item {
                GlassCard(backdrop, Modifier.fillMaxWidth()) {
                    Txt("底栏效果", size = 16, bold = true, color = text)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(backdrop, if (bottomStyle == 0) "液态玻璃 ✓" else "液态玻璃", accent, solid = bottomStyle == 0) { onBottom(0) }
                        GlassButton(backdrop, if (bottomStyle == 1) "磨砂 ✓" else "磨砂", accent, solid = bottomStyle == 1) { onBottom(1) }
                        GlassButton(backdrop, if (bottomStyle == 2) "纯色 ✓" else "纯色", accent, solid = bottomStyle == 2) { onBottom(2) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Txt("液态玻璃 = 折射 + 模糊；磨砂 = 只模糊不折射；纯色 = 不用玻璃，直接用主题底色", color = muted)
                }
            }
            item {
                GlassCard(backdrop, Modifier.fillMaxWidth()) {
                    Txt("显示与排序", size = 16, bold = true, color = text)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleRow("显示介绍", showDesc, accent, text) { onShowDesc(it) }
                        ToggleRow("显示角标", showBadge, accent, text) { onShowBadge(it) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(backdrop, if (!sortByName) "默认顺序 ✓" else "默认顺序", accent, solid = !sortByName) { onSort(false) }
                        GlassButton(backdrop, if (sortByName) "按名称排序 ✓" else "按名称排序", accent, solid = sortByName) { onSort(true) }
                    }
                }
            }
            item {
                GlassCard(backdrop, Modifier.fillMaxWidth()) {
                    Txt("收藏", size = 16, bold = true, color = text)
                    Spacer(Modifier.height(4.dp))
                    Txt("当前 $favCount 个收藏，存在本机，卸载会丢", color = muted)
                    Spacer(Modifier.height(8.dp))
                    GlassButton(backdrop, "清空收藏", accent) { onClearFavs() }
                }
            }
            item {
                GlassCard(backdrop, Modifier.fillMaxWidth()) {
                    Txt("关于", size = 16, bold = true, color = text)
                    Spacer(Modifier.height(4.dp))
                    Txt("懒得找了 · 玻璃测试 v1.1.0（versionCode 2）", color = muted)
                    Txt("包名 com.landezhaole.glasstest · 可与主 App 共存", color = muted)
                    Txt("Backdrop 2.0.1 · 数据 $totalCats 分类 / $totalSites 站点 / $totalNew NEW", color = muted)
                    Txt("原生 Kotlin + Compose，无 WebView", color = muted)
                }
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }

    @Composable
    private fun ToggleRow(label: String, value: Boolean, accent: Color, text: Color, onChange: (Boolean) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(label, size = 14, color = text)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(52.dp, 28.dp).clip(RoundedRectangle(14.dp))
                    .background(if (value) accent else Color.Gray.copy(alpha = 0.4f))
                    .clickable { onChange(!value) },
                contentAlignment = if (value) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White))
            }
        }
    }

    @Composable
    private fun BottomBar(
        backdrop: LayerBackdrop, accent: Color, text: Color, muted: Color,
        isDark: Boolean,
        style: Int, page: Int,
        onHome: () -> Unit, onFav: () -> Unit, onNew: () -> Unit, onSettings: () -> Unit,
        newActive: Boolean,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            val barModifier = when (style) {
                2 -> Modifier.fillMaxWidth().background(
                    (if (isDark) Libraries.darkBg else Libraries.lightBg).copy(alpha = 0.95f)
                )
                else -> Modifier.fillMaxWidth().drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(0.dp) },
                    effects = {
                        vibrancy()
                        blur(if (style == 0) 4f.dp.toPx() else 8f.dp.toPx())
                        if (style == 0) lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                )
            }
            Box(barModifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BottomItem("首页", page == 0, accent, muted) { onHome() }
                    BottomItem(if (newActive) "NEW ✓" else "NEW", newActive, accent, muted) { onNew() }
                    BottomItem("收藏", page == 1, accent, muted) { onFav() }
                    BottomItem("设置", page == 2, accent, muted) { onSettings() }
                }
            }
        }
    }

    @Composable
    private fun BottomItem(label: String, active: Boolean, accent: Color, muted: Color, onClick: () -> Unit) {
        Box(
            Modifier.clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Txt(label, size = 15, bold = active, color = if (active) accent else muted)
        }
    }

    @Composable
    private fun Blob(color: Color, fx: Float, fy: Float, sizeDp: androidx.compose.ui.unit.Dp) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Box(
                Modifier
                    .offset(x = (fx * 360).dp, y = (fy * 640).dp)
                    .size(sizeDp)
                    .background(Brush.radialGradient(listOf(color, Color.Transparent)), CircleShape)
            )
        }
    }

    @Composable
    private fun Txt(
        text: String, size: Int = 14, bold: Boolean = false, color: Color,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        BasicText(
            text,
            style = TextStyle(
                color = color, fontSize = size.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }

    @Composable
    private fun GlassCard(
        backdrop: LayerBackdrop, modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null, content: @Composable () -> Unit,
    ) {
        Box(
            modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                )
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(16.dp),
        ) { Column { content() } }
    }

    @Composable
    private fun GlassButton(
        backdrop: LayerBackdrop, text: String, accent: Color,
        solid: Boolean = false, onClick: () -> Unit,
    ) {
        Box(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(2f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = {
                        if (solid) drawRect(accent.copy(alpha = 0.85f))
                    },
                )
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Txt(text, size = 14, bold = solid, color = if (solid) Color.White else accent)
        }
    }
}
