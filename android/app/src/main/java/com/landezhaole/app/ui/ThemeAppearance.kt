package com.landezhaole.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landezhaole.app.AppPalette
import com.landezhaole.app.GlassPanel

// ---- Kotlin translation of theme_appearance_page.dart + glass.dart ----
// ThemeMode mirrors AppThemeMode in the Flutter original: system / light / dark.
// The original deliberately does NOT read the system brightness from context for
// a manually chosen mode; the same rule is kept here: SYSTEM is the only mode
// that follows the system, LIGHT/DARK are absolute.
enum class ThemeMode { SYSTEM, LIGHT, DARK }

fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

fun themeSchemeLabel(scheme: String): String = when (scheme) {
    "neu003" -> "NEU 柔影"
    "glass004" -> "GLASS 雾面"
    "doodle006" -> "DOODLE 手绘"
    "lever009" -> "LEVER 拨杆"
    "y2k032" -> "S11 千禧复兴"
    "crystal" -> "CRYSTAL 水晶液态"
    "glasschat" -> "GlassChat 玻璃"
    else -> "默认（跟随深浅）"
}

// UiZoom mirrors the Flutter UiZoom: lay the content out at virtual size
// (real size / scale) and draw it scaled, so padding, text and spacing all
// scale together instead of only the font size changing.
@Composable
fun UiZoom(scale: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (scale == 1f) {
        androidx.compose.foundation.layout.Box(modifier = modifier) { content() }
        return
    }
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val virtualWidth = (constraints.maxWidth / scale).toInt()
        val virtualHeight = (constraints.maxHeight / scale).toInt()
        val placeables = measurables.map {
            it.measure(
                androidx.compose.ui.unit.Constraints(
                    maxWidth = virtualWidth,
                    maxHeight = virtualHeight
                )
            )
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { placeable ->
                placeable.placeWithLayer(0, 0) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }
}

@Composable
fun ExpandableThemeCard(
    pal: AppPalette,
    title: String,
    currentValue: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    GlassPanel(modifier = Modifier.fillMaxWidth(), pal = pal) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(18.dp, 22.dp, 14.dp, 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
                Spacer(modifier = Modifier.weight(1f))
                Text(currentValue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pal.secondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (expanded) "▴" else "▾", fontSize = 14.sp, color = pal.secondary)
            }
            if (expanded) {
                Column(modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 10.dp)) { content() }
            }
        }
    }
}

@Composable
fun ThemeChoiceRow(pal: AppPalette, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = pal.accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 16.sp, color = pal.foreground)
    }
}

// Kotlin version of ThemeAppearancePage: theme mode + UI scale, in the same
// collapsed-card style as the original (a card shows title + current value,
// tapping it expands the options, picking one collapses it again).
@Composable
fun ThemeAppearanceScreen(
    pal: AppPalette,
    mode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
    scheme: String,
    onSchemeChange: (String) -> Unit,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = pal.subtleFill, contentColor = pal.foreground),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("‹ 返回", fontSize = 14.sp) }
                Spacer(modifier = Modifier.width(12.dp))
                Text("主题与外观", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pal.foreground)
            }
            Spacer(modifier = Modifier.height(16.dp))
            ExpandableThemeCard(pal = pal, title = "系统主题", currentValue = themeModeLabel(mode)) {
                ThemeMode.values().forEach { m ->
                    ThemeChoiceRow(pal = pal, label = themeModeLabel(m), selected = mode == m, onClick = { onModeChange(m) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ExpandableThemeCard(pal = pal, title = "主题风格", currentValue = themeSchemeLabel(scheme)) {
                listOf("default", "neu003", "glass004", "doodle006", "lever009", "y2k032", "crystal", "glasschat").forEach { s ->
                    ThemeChoiceRow(pal = pal, label = themeSchemeLabel(s), selected = scheme == s, onClick = { onSchemeChange(s) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            var draft by remember(uiScale) { mutableStateOf(uiScale) }
            ExpandableThemeCard(pal = pal, title = "界面缩放大小", currentValue = "${(draft * 100).toInt()}%") {
                Slider(
                    value = draft,
                    onValueChange = { draft = it },
                    onValueChangeFinished = { onUiScaleChange(draft) },
                    valueRange = 0.8f..1.3f,
                    colors = SliderDefaults.colors(thumbColor = pal.accent, activeTrackColor = pal.accent)
                )
                Text("拖动调整，松手应用", fontSize = 12.sp, color = pal.secondary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "深浅两档色板与毛玻璃卡片均按你发来的 Flutter 主题翻译：浅色强调蓝、深色强调蓝、页底色与半透明白底，卡片无阴影、无模糊。",
                fontSize = 12.sp, color = pal.secondary
            )
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
