package com.landezhaole.glasstest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

// 懒得找了 · 液态玻璃测试版
// 液态玻璃：Kyant0/AndroidLiquidGlass Backdrop 2.0.1，按上传的安卓完整代码写法：
//   rememberLayerBackdrop + layerBackdrop（背景层）+ drawBackdrop（玻璃组件）
// 数据：CF Pages 项目 landezhaole 的 data.json 原样内置，21 分类 / 2603 站点。
// 主题：landezhaole.pages.dev 当前的 Libraries 浅深两套，数值翻译成 Kotlin 颜色，
//   浅：底 #fdfdfd / 卡 #ffffff / 文 #0d0d0d / 次 #6c6c6c / 强调 #0073e5
//   深：底 #121212 / 卡 #181818 / 文 #ffffff / 次 #cacaca / 强调 #55cfff
// 原生 Kotlin + Compose，没有 WebView，没有 HTML。

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GlassTestApp() }
    }

    @Composable
    private fun GlassTestApp() {
        val context = LocalContext.current
        val cats = remember { loadSiteData(context) }
        var isDark by remember { mutableStateOf(false) }
        var query by remember { mutableStateOf("") }
        var catIndex by remember { mutableStateOf(-1) }
        var onlyNew by remember { mutableStateOf(false) }

        val bg = if (isDark) Libraries.darkBg else Libraries.lightBg
        val text = if (isDark) Libraries.darkText else Libraries.lightText
        val muted = if (isDark) Libraries.darkMuted else Libraries.lightMuted
        val accent = if (isDark) Libraries.darkAccent else Libraries.lightAccent

        val totalSites = remember(cats) { cats.sumOf { it.sites.size } }
        val totalNew = remember(cats) {
            cats.sumOf { c -> c.sites.count { it.badge.contains("NEW") } }
        }

        val shown: List<Pair<String, Site>> = remember(cats, query, catIndex, onlyNew) {
            val q = query.trim()
            val result = ArrayList<Pair<String, Site>>()
            val pool = if (q.isNotEmpty()) {
                cats
            } else if (catIndex >= 0 && catIndex < cats.size) {
                listOf(cats[catIndex])
            } else {
                cats
            }
            for (cat in pool) {
                for (site in cat.sites) {
                    if (onlyNew && !site.badge.contains("NEW")) continue
                    if (q.isNotEmpty()) {
                        val hit = site.title.contains(q, ignoreCase = true) ||
                            site.desc.contains(q, ignoreCase = true) ||
                            site.url.contains(q, ignoreCase = true)
                        if (!hit) continue
                    }
                    result.add(cat.name to site)
                    if (result.size >= 300) break
                }
                if (result.size >= 300) break
            }
            result
        }

        val backdrop = rememberLayerBackdrop()
        Box(Modifier.fillMaxSize()) {
            // 背景层：必须带 layerBackdrop，玻璃才能采样它
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                Box(Modifier.fillMaxSize().background(bg))
                Blob(Color(0xFF0073E5), 0.05f, 0.02f, 320.dp)
                Blob(Color(0xFF55CFFF), 0.62f, 0.18f, 280.dp)
                Blob(Color(0xFFF59E0B), 0.18f, 0.55f, 300.dp)
                Blob(Color(0xFF7C4DFF), 0.60f, 0.72f, 260.dp)
            }

            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { Spacer(Modifier.height(48.dp)) }
                item {
                    GlassCard(backdrop, Modifier.fillMaxWidth()) {
                        Txt("懒得找了 · 玻璃测试", size = 22, bold = true, color = text)
                        Spacer(Modifier.height(4.dp))
                        Txt(
                            "Backdrop 2.0.1 真液态玻璃 · 数据来自 CF 项目 landezhaole",
                            color = muted,
                        )
                        Spacer(Modifier.height(8.dp))
                        Txt(
                            "${cats.size} 个分类 · $totalSites 个站点 · $totalNew 个 NEW · 测试版",
                            color = muted,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassButton(
                                backdrop, if (!isDark) "Libraries·浅 ✓" else "Libraries·浅",
                                accent, solid = !isDark,
                            ) { isDark = false }
                            GlassButton(
                                backdrop, if (isDark) "Libraries·深 ✓" else "Libraries·深",
                                accent, solid = isDark,
                            ) { isDark = true }
                        }
                    }
                }
                item {
                    // 搜索框：玻璃胶囊
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
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            GlassButton(
                                backdrop, if (catIndex == -1) "全部 ✓" else "全部",
                                accent, solid = catIndex == -1,
                            ) { catIndex = -1 }
                        }
                        items(cats.size) { i ->
                            val short = cats[i].name.substringBefore(" ·")
                            GlassButton(
                                backdrop, if (catIndex == i) "$short ✓" else short,
                                accent, solid = catIndex == i,
                            ) { catIndex = i }
                        }
                    }
                }
                item {
                    Txt(
                        if (query.isNotEmpty()) "搜索“$query” · ${shown.size} 条（最多显示 300 条）"
                        else if (catIndex >= 0) "${cats[catIndex].name} · ${shown.size} 条"
                        else "全部 · ${shown.size} 条（最多显示 300 条，搜一下更快）",
                        color = muted,
                    )
                }
                items(shown.size) { i ->
                    val (catName, site) = shown[i]
                    GlassCard(
                        backdrop, Modifier.fillMaxWidth(),
                        onClick = {
                            if (site.url.isNotBlank()) {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(site.url))
                                    )
                                } catch (_: Exception) {
                                }
                            }
                        },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Txt(
                                    site.title.take(1).ifBlank { "站" },
                                    size = 16, bold = true, color = accent,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Txt(
                                    site.title, size = 16, bold = true, color = text,
                                    maxLines = 1,
                                )
                                if (site.desc.isNotBlank()) {
                                    Txt(site.desc, color = muted, maxLines = 2)
                                }
                            }
                            if (site.badge.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Txt(site.badge, size = 12, bold = true, color = accent)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Txt(site.url, size = 12, color = accent, maxLines = 1)
                        Txt(catName, size = 12, color = muted, maxLines = 1)
                    }
                }
                item { Spacer(Modifier.height(110.dp)) }
            }

            // 底部玻璃 Dock
            Box(
                Modifier.fillMaxSize().padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    Modifier
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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(backdrop, "全部", accent, solid = !onlyNew) {
                            onlyNew = false
                        }
                        GlassButton(backdrop, if (onlyNew) "只看 NEW ✓" else "只看 NEW", accent, solid = onlyNew) {
                            onlyNew = !onlyNew
                        }
                        GlassButton(backdrop, if (isDark) "切浅色" else "切深色", accent) {
                            isDark = !isDark
                        }
                    }
                }
            }
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
