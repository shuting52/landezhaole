package com.landezhaole.app

import android.graphics.Bitmap
import android.graphics.Canvas
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
import kotlin.random.Random
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

// ---- 网站页头粒子 Logo：canvas 采样“懒得找了.”约 700 粒（手机性能取舍，网站 2000 粒），
// 飞入 1.5s 先快后慢，呼吸浮动，手指 80px 内推开、弹簧回弹，点一下重新飞入，品牌渐变色 ----
private data class Particle(
    val tx: Float, val ty: Float,
    var x: Float, var y: Float,
    var sx: Float, var sy: Float,
    val delay: Float,
    var vx: Float = 0f, var vy: Float = 0f,
    val ph: Float, val ph2: Float,
    val color: Color
)

private fun gradColor(x: Float): Color {
    val t = x / 600f
    fun mix(a: Int, b: Int, k: Float): Int = (a + (b - a) * k).toInt()
    return if (t < 0.5f) {
        val k = t * 2
        Color(mix(22, 14, k), mix(163, 165, k), mix(74, 233, k))
    } else {
        val k = (t - 0.5f) * 2
        Color(mix(14, 139, k), mix(165, 92, k), mix(233, 246, k))
    }
}

private fun sampleTextTargets(): List<Pair<Float, Float>> {
    return try {
        val bmp = Bitmap.createBitmap(600, 130, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 84f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.WHITE
        }
        canvas.drawText("懒得找了.", 300f, 68f, paint)
        val pixels = IntArray(600 * 130)
        bmp.getPixels(pixels, 0, 600, 0, 0, 600, 130)
        val pts = mutableListOf<Pair<Float, Float>>()
        for (y in 0 until 130 step 3) {
            for (x in 0 until 600 step 3) {
                val alpha = (pixels[y * 600 + x] ushr 24) and 0xff
                if (alpha > 128) pts.add(x.toFloat() to y.toFloat())
            }
        }
        pts.shuffled().take(700)
    } catch (e: Exception) {
        emptyList()
    }
}

private fun randomEdgeStart(random: Random): Pair<Float, Float> {
    return when ((random.nextFloat() * 4).toInt()) {
        0 -> -20f to random.nextFloat() * 130f
        1 -> 620f to random.nextFloat() * 130f
        2 -> random.nextFloat() * 600f to -20f
        else -> random.nextFloat() * 600f to 150f
    }
}

@Composable
fun ParticleLogo(modifier: Modifier = Modifier) {
    val targets = remember { sampleTextTargets() }
    var replay by remember { mutableStateOf(0) }
    var particles by remember(targets, replay) {
        mutableStateOf(targets.map { (tx, ty) ->
            val random = Random.Default
            val (sx, sy) = randomEdgeStart(random)
            Particle(
                tx = tx, ty = ty, x = sx, y = sy, sx = sx, sy = sy,
                delay = random.nextFloat() * 0.35f,
                ph = random.nextFloat() * 6.283f, ph2 = random.nextFloat() * 6.283f,
                color = gradColor(tx)
            )
        })
    }
    var repel by remember { mutableStateOf<Offset?>(null) }
    var startNanos by remember(targets, replay) { mutableStateOf<Long?>(null) }
    LaunchedEffect(targets, replay) {
        withFrameNanos { startNanos = it }
        while (true) {
            withFrameNanos { now ->
                val start = startNanos ?: now
                val el = (now - start) / 1_000_000_000f
                val nowMs = now / 1_000_000f
                val rep = repel
                particles = particles.map { pt ->
                    val lt = el - pt.delay
                    if (lt < 0) {
                        pt.x = pt.sx; pt.y = pt.sy
                    } else if (lt < 1.5f) {
                        val pr = lt / 1.5f
                        val ease = if (pr >= 1f) 1f else 1f - Math.pow(2.0, (-10 * pr).toDouble()).toFloat()
                        pt.x = pt.sx + (pt.tx - pt.sx) * ease
                        pt.y = pt.sy + (pt.ty - pt.sy) * ease
                    } else {
                        val bx = pt.tx + sin(nowMs * 0.0012f + pt.ph) * 1.3f
                        val by = pt.ty + sin(nowMs * 0.001f + pt.ph2) * 1.3f
                        pt.vx += (bx - pt.x) * 0.075f
                        pt.vy += (by - pt.y) * 0.075f
                        if (rep != null) {
                            val dx = pt.x - rep.x
                            val dy = pt.y - rep.y
                            val d = kotlin.math.sqrt(dx * dx + dy * dy)
                            val rad = 160f
                            if (d < rad && d > 0.01f) {
                                val f = (1 - d / rad) * 6f
                                pt.vx += dx / d * f
                                pt.vy += dy / d * f
                            }
                        }
                        pt.vx *= 0.86f
                        pt.vy *= 0.86f
                        pt.x += pt.vx
                        pt.y += pt.vy
                    }
                    pt
                }
            }
        }
    }
    Canvas(
        modifier = modifier.pointerInput(replay) {
            awaitEachGesture {
                val down = awaitFirstDown()
                var moved = 0f
                var lastPos = down.position
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull() ?: break
                    if (change.pressed) {
                        moved += (change.position - lastPos).getDistance()
                        lastPos = change.position
                        // 手指位置换算到 600x130 的粒子坐标
                        val scaleX = 600f / size.width
                        val scaleY = 130f / size.height
                        repel = Offset(change.position.x * scaleX, change.position.y * scaleY)
                    } else {
                        repel = null
                        if (moved < 20f) replay++
                        break
                    }
                }
            }
        }
    ) {
        val scaleX = size.width / 600f
        val scaleY = size.height / 130f
        for (pt in particles) {
            drawCircle(
                color = pt.color,
                radius = 2.6f * scaleX,
                center = Offset(pt.x * scaleX, pt.y * scaleY)
            )
        }
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

// ---- 网站快捷 Dock：鱼眼 + 名称提示（手机端手指版），点一下跳到对应分类 ----
// 鱼眼参数与网站一致：最大 1.5 倍，高斯 sigma 按间距 0.85；Dock 自成组合，滚动重组只影响它自己
@Composable
fun DockFishEye(cats: List<Category>, pal: AppPalette, onJump: (String) -> Unit) {
    val dockScroll = rememberScrollState()
    val density = LocalDensity.current
    var touchX by remember { mutableStateOf<Float?>(null) }
    var tipIndex by remember { mutableStateOf<Int?>(null) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (tipIndex != null && tipIndex!! < cats.size) {
            Box(
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(pal.barBackground)
                    .border(1.dp, pal.line, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(cats[tipIndex!!].name, fontSize = 12.sp, color = pal.foreground)
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
                            for (i in cats.indices) {
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
                cats.forEachIndexed { i, cat ->
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
                            .background(avatarColor(cat.name))
                            .clickable { onJump(cat.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cat.name.trim().firstOrNull()?.toString() ?: "?",
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
