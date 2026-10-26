package com.landezhaole.app

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.delay

// ---- 网站色板（light / dark，与网站 :root / [data-theme="dark"] 一致） ----
val WebBlueLight = Color(0xFF2563EB)
val WebBlueDark = Color(0xFF60A5FA)
val WebGreenDarkTextLight = Color(0xFF15803D)
val WebGreenDarkTextDark = Color(0xFF10B981)

// ---- 网站背景三团柔光（网站 .blob，径向渐变模拟 blur 60px 的软边） ----
@Composable
fun WebBlobs(pal: AppPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val blobs = if (pal.isDark) {
            listOf(
                Triple(Offset(-0.10f * w, -0.05f * h), 0.70f * w, Color(0xFF064E3B)),
                Triple(Offset(1.10f * w, 0.22f * h), 0.62f * w, Color(0xFF1E3A8A)),
                Triple(Offset(0.30f * w, 1.05f * h), 0.58f * w, Color(0xFF78350F))
            )
        } else {
            listOf(
                Triple(Offset(-0.10f * w, -0.05f * h), 0.70f * w, Color(0xFFA7F3D0)),
                Triple(Offset(1.10f * w, 0.22f * h), 0.62f * w, Color(0xFFBFDBFE)),
                Triple(Offset(0.30f * w, 1.05f * h), 0.58f * w, Color(0xFFFDE68A))
            )
        }
        for ((center, radius, color) in blobs) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}

// ---- 网站翻页时钟（左上，HH:MM:SS，数字变化时翻页动画 0.6s） ----
@Composable
fun FlipDigit(ch: Char, pal: AppPalette) {
    var displayed by remember { mutableStateOf(ch) }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(ch) {
        if (ch != displayed) {
            rotation.animateTo(90f, animationSpec = tween(300))
            displayed = ch
            rotation.animateTo(0f, animationSpec = tween(300))
        }
    }
    val digitBg = if (pal.isDark) Color(0xFF2A2A2A) else Color.White
    val digitColor = if (pal.isDark) Color(0xFFEEEEEE) else Color(0xFF1F2937)
    val seam = if (pal.isDark) Color(0xFF444444) else Color(0xFFD1D5DB)
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = 42.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(digitBg)
            .border(1.dp, seam, RoundedCornerShape(5.dp))
            .graphicsLayer {
                rotationX = rotation.value
                cameraDistance = 12f * density
            },
        contentAlignment = Alignment.Center
    ) {
        Text(displayed.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = digitColor)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(1.dp)
                .background(seam)
        )
    }
}

@Composable
fun FlipClock(pal: AppPalette) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L - System.currentTimeMillis() % 1000L)
            now = System.currentTimeMillis()
        }
    }
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    fun two(v: Int): String = "%02d".format(v)
    val h = two(cal.get(Calendar.HOUR_OF_DAY))
    val m = two(cal.get(Calendar.MINUTE))
    val s = two(cal.get(Calendar.SECOND))
    val cardBg = pal.barBackground
    val lineColor = pal.line
    val colonColor = if (pal.isDark) Color(0xFFEEEEEE) else Color(0xFF1F2937)
    Row(
        modifier = Modifier
            .shadow(2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(1.dp, lineColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        FlipDigit(h[0], pal)
        FlipDigit(h[1], pal)
        Text(":", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colonColor)
        FlipDigit(m[0], pal)
        FlipDigit(m[1], pal)
        Text(":", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colonColor)
        FlipDigit(s[0], pal)
        FlipDigit(s[1], pal)
    }
}

// ---- 网站卡片（4 列竖版）：图标居中在上、标题居中在下（最多 2 行），
// 角标贴卡片上沿（网站 .corner-tag）；按压时上抬 2dp + 绿边 + 阴影 ----
@Composable
fun WebCard(
    card: SiteCard,
    todayClicks: Int,
    pal: AppPalette,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val label = if (card.b.isNotBlank()) card.b else "精选"
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(if (pressed) 6.dp else 0.dp, RoundedCornerShape(14.dp))
                .offset(y = if (pressed) (-2).dp else 0.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pal.surface)
                .border(1.dp, if (pressed) pal.accent else pal.line, RoundedCornerShape(14.dp))
                .clickable(interactionSource = interaction, indication = null) { onClick() }
        ) {
            // 竖版卡：图标在上、名字在下，名字放两行（四列横版留给名字只剩两三个字，用户已否决）
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SiteIcon(card)
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.TopCenter) {
                    Text(
                        card.t, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = pal.foreground, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                if (todayClicks > 0) {
                    Text("今日点击 $todayClicks 次", fontSize = 10.sp, color = pal.danger)
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = (-8).dp)
                .background(RAINBOW, RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

// ---- 网站快捷 Dock：鱼眼 + 名称提示（手机端手指版） ----
// 鱼眼参数与网站一致：最大 1.5 倍，高斯 sigma 按间距 0.85；Dock 自成组合，滚动重组只影响它自己
// 条目由调用方给：前 10 个分类 + 软件库直达（点一下跳到资源网站并切到软件库子分类）
data class DockEntry(val name: String, val onClick: () -> Unit)

@Composable
fun DockFishEye(entries: List<DockEntry>, pal: AppPalette) {
    val dockScroll = rememberScrollState()
    val density = LocalDensity.current
    var touchX by remember { mutableStateOf<Float?>(null) }
    var tipIndex by remember { mutableStateOf<Int?>(null) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (tipIndex != null && tipIndex!! < entries.size) {
            Box(
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(pal.barBackground)
                    .border(1.dp, pal.line, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(entries[tipIndex!!].name, fontSize = 12.sp, color = pal.foreground)
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
        Box(
            modifier = Modifier.pointerInput(Unit) {
                // 只用 foundation.gestures 的 awaitEachGesture（本工程已验证可编过）：
                // 每轮手势内用 Initial 通道观测位置但不消费，底下的点击仍能收到
                while (true) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun track(pos: Offset) {
                            touchX = pos.x
                            val touchDp = pos.x / density.density + dockScroll.value / density.density
                            var nearest = 0
                            var best = Float.MAX_VALUE
                            for (i in entries.indices) {
                                val d = kotlin.math.abs(touchDp - (i * 50f + 20f))
                                if (d < best) { best = d; nearest = i }
                            }
                            tipIndex = if (best < 60f) nearest else null
                        }
                        track(down.position)
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                track(change.position)
                            } else {
                                touchX = null
                                tipIndex = null
                                break
                            }
                        }
                    }
                }
            }
        ) {
            Row(
                modifier = Modifier.horizontalScroll(dockScroll),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                entries.forEachIndexed { i, entry ->
                    val s = if (touchX == null) 1f else {
                        val touchDp = touchX!! / density.density + dockScroll.value / density.density
                        val d = kotlin.math.abs(touchDp - (i * 50f + 20f))
                        val sigma = maxOf(46f, 50f * 0.85f)
                        (1f + 0.5f * exp(-(d * d) / (2 * sigma * sigma))).toFloat()
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = s
                                scaleY = s
                                translationY = -(s - 1f) * 34f * density.density
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(avatarColor(entry.name))
                            .clickable { entry.onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            entry.name.trim().firstOrNull()?.toString() ?: "?",
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---- 网站分类区：白底圆角 18 + 细边 + 淡影，蓝点 + 红标题 + 数量胶囊 ----
@Composable
fun WebSection(
    modifier: Modifier = Modifier,
    pal: AppPalette,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(pal.barBackground)
            .border(1.dp, pal.line, RoundedCornerShape(18.dp))
    ) {
        content()
    }
}
