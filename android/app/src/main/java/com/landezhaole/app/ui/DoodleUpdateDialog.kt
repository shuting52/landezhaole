package com.landezhaole.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// 手绘涂鸦风更新弹窗：原 HTML 演示的 Compose 翻译版
// 四态：downloading -> installing -> done，downloading -> cancelled -> downloading
// 注意：进度目前是演示模拟（与原 HTML 一致），真正的 APK 下载安装需接 Release 直链后再替换 tick() 里的进度来源。
const val DOODLE_VERSION = "1.0.1"
const val DOODLE_TOTAL_MB = 15.7f

private val DoodleBg = Color(0xFFFDF8)
private val DoodleInk = Color(0xFF2B2B2B)
private val DoodleYellow = Color(0xFFFFD93D)
private val DoodleRed = Color(0xFFFF6B6B)
private val DoodleBlue = Color(0xFF4D96FF)
private val DoodleGreen = Color(0xFF6BCB77)
private val DoodleShape = RoundedCornerShape(topStart = 28.dp, topEnd = 36.dp, bottomStart = 26.dp, bottomEnd = 38.dp)

enum class DoodleState { DOWNLOADING, INSTALLING, DONE, CANCELLED }

@Composable
fun DoodleUpdateDialog(onClose: () -> Unit, onToast: (String) -> Unit) {
    var state by remember { mutableStateOf(DoodleState.DOWNLOADING) }
    var progress by remember { mutableStateOf(0f) }
    var speed by remember { mutableStateOf(0f) }

    // 下载计时：每 100ms 前进一次，速度随机 + 平滑
    LaunchedEffect(state) {
        if (state != DoodleState.DOWNLOADING) return@LaunchedEffect
        while (state == DoodleState.DOWNLOADING) {
            delay(100)
            val sp = 8f * (0.6f + Random.nextFloat() * 0.8f)
            speed = speed * 0.7f + sp * 0.3f
            val next = progress + sp * 0.1f / DOODLE_TOTAL_MB * 100f
            if (next >= 100f) {
                progress = 100f
                state = DoodleState.INSTALLING
            } else {
                progress = next
            }
        }
    }
    // 安装：2.4 秒后完成
    LaunchedEffect(state) {
        if (state == DoodleState.INSTALLING) {
            delay(2400)
            state = DoodleState.DONE
            onToast("已更新到最新版本")
        }
    }
    fun reset() {
        progress = 0f
        speed = 0f
        state = DoodleState.DOWNLOADING
    }

    Dialog(onDismissRequest = { if (state == DoodleState.DOWNLOADING) { state = DoodleState.CANCELLED; onToast("已取消下载") } else onClose() }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.rotate(-0.6f)) {
                Column(
                    modifier = Modifier
                        .width(330.dp)
                        .shadow(5.dp, DoodleShape)
                        .background(DoodleBg, DoodleShape)
                        .border(3.dp, DoodleInk, DoodleShape)
                        .padding(26.dp, 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = DoodleRed, modifier = Modifier.size(26.dp).rotate(-8f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("版本更新", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DoodleInk, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape).border(2.5.dp, DoodleInk, CircleShape).background(DoodleBg)
                                .clickable {
                                    if (state == DoodleState.DOWNLOADING) { state = DoodleState.CANCELLED; onToast("已取消下载") } else onClose()
                                },
                            contentAlignment = Alignment.Center
                        ) { Text("×", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DoodleInk) }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val subtitle = when (state) {
                        DoodleState.DOWNLOADING -> buildAnnotatedString {
                            append("正在下载 ")
                            withStyle(SpanStyle(background = DoodleYellow)) { append(DOODLE_VERSION) }
                            append(" 懒得找了版")
                        }
                        DoodleState.INSTALLING -> buildAnnotatedString { append("正在安装… 别乱动！") }
                        DoodleState.DONE -> buildAnnotatedString {
                            append("画好啦！")
                            withStyle(SpanStyle(background = DoodleYellow)) { append("更新完成") }
                        }
                        DoodleState.CANCELLED -> buildAnnotatedString { append("画了一半不画了") }
                    }
                    Text(subtitle, fontSize = 15.sp, color = Color(0xFF555555), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    val meta = when (state) {
                        DoodleState.DOWNLOADING -> "%.1fMB / %.1fMB · %.1f MB/s".format(progress / 100f * DOODLE_TOTAL_MB, DOODLE_TOTAL_MB, speed)
                        DoodleState.INSTALLING -> "%.1fMB / %.1fMB · 校验完成".format(DOODLE_TOTAL_MB, DOODLE_TOTAL_MB)
                        DoodleState.DONE -> "$DOODLE_VERSION 已安装 · 去找站吧"
                        DoodleState.CANCELLED -> "%.1fMB / %.1fMB · 已停止".format(progress / 100f * DOODLE_TOTAL_MB, DOODLE_TOTAL_MB)
                    }
                    Text(meta, fontSize = 12.5.sp, color = Color(0xFF999999), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(20.dp))

                    if (state == DoodleState.DONE) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape).background(DoodleGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Check, contentDescription = null, tint = DoodleGreen, modifier = Modifier.size(56.dp)) }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 蜡笔涂抹进度条
                    val busyAlpha = if (state == DoodleState.INSTALLING) {
                        val t = rememberInfiniteTransition(label = "busy")
                        val a by t.animateFloat(initialValue = 1f, targetValue = 0.7f, animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
                        a
                    } else 1f
                    Box(
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                            .alpha(busyAlpha)
                            .drawBehind {
                                val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                drawRoundRect(color = Color(0xFF8A8A8A), style = Stroke(width = 2.5.dp.toPx(), pathEffect = dash))
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .padding(4.dp)
                    ) {
                        val barColor = when (state) {
                            DoodleState.DOWNLOADING, DoodleState.CANCELLED -> DoodleRed
                            DoodleState.INSTALLING -> DoodleBlue
                            DoodleState.DONE -> DoodleGreen
                        }
                        val frac = if (state == DoodleState.INSTALLING || state == DoodleState.DONE) 0.98f else max(0f, min(0.98f, progress / 100f))
                        Box(modifier = Modifier.fillMaxWidth(frac).height(22.dp).clip(RoundedCornerShape(9.dp)).background(barColor)) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val w = size.width
                                val h = size.height
                                var x = -h
                                while (x < w) {
                                    drawLine(color = Color.White.copy(alpha = 0.3f), start = Offset(x, h), end = Offset(x + h, 0f), strokeWidth = 6f)
                                    x += 16f
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.rotate(-1.2f), verticalAlignment = Alignment.Bottom) {
                        Text("${progress.toInt()}%", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = DoodleInk)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            when (state) {
                                DoodleState.DOWNLOADING -> "蜡笔涂抹中…"
                                DoodleState.INSTALLING -> "正在粘贴…"
                                DoodleState.DONE -> "大功告成！"
                                DoodleState.CANCELLED -> "纸还留着…"
                            },
                            fontSize = 14.sp, color = Color(0xFF888888)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    val btnText = when (state) {
                        DoodleState.DOWNLOADING -> "不更了"
                        DoodleState.CANCELLED -> "继续画完"
                        DoodleState.INSTALLING -> "安装中…"
                        DoodleState.DONE -> "好耶，去找站"
                    }
                    val btnColor = when (state) {
                        DoodleState.DONE -> DoodleGreen
                        DoodleState.INSTALLING -> DoodleBg
                        else -> DoodleYellow
                    }
                    Button(
                        onClick = {
                            when (state) {
                                DoodleState.DOWNLOADING -> { state = DoodleState.CANCELLED; onToast("已取消下载") }
                                DoodleState.CANCELLED -> { reset(); onToast("继续下载") }
                                DoodleState.DONE -> { onClose(); onToast("已更新到 $DOODLE_VERSION") }
                                DoodleState.INSTALLING -> {}
                            }
                        },
                        enabled = state != DoodleState.INSTALLING,
                        colors = ButtonDefaults.buttonColors(containerColor = btnColor, contentColor = DoodleInk, disabledContainerColor = DoodleBg, disabledContentColor = DoodleInk),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 26.dp),
                        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)).border(3.dp, DoodleInk, RoundedCornerShape(20.dp))
                    ) { Text(btnText, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp)) }
                }
                // 胶带
                Box(
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-13).dp).rotate(2f)
                        .width(92.dp).height(26.dp).background(DoodleYellow.copy(alpha = 0.75f))
                        .border(1.dp, DoodleInk.copy(alpha = 0.25f))
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            TextButton(onClick = { reset(); onToast("重新开始演示") }) {
                Text("↺ 再来一遍", fontSize = 14.sp, color = Color(0xFF888888), textDecoration = TextDecoration.Underline)
            }
        }
    }
}
