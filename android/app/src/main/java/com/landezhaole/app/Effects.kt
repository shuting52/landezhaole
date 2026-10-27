package com.landezhaole.app

import android.graphics.Matrix as AndroidMatrix
import android.graphics.PathMeasure as AndroidPathMeasure
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

// ---- 搜索框旋转光边：SweepGradient 就是 CSS conic-gradient，绕中心旋转，首尾同色保证接缝无断点 ----
fun rotatingGlowBrush(angle: Float): Brush {
    return object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val green = Color(0xFF16A34A).toArgb()
            val clear = Color.Transparent.toArgb()
            val shader = SweepGradient(cx, cy, intArrayOf(green, clear, green, clear, green), null)
            val matrix = AndroidMatrix()
            matrix.setRotate(angle, cx, cy)
            shader.setLocalMatrix(matrix)
            return shader
        }
    }
}

@Composable
fun rememberGlowAngle(): Float {
    val t = rememberInfiniteTransition(label = "glowRotate")
    val angle by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "glowAngle"
    )
    return angle
}

// ---- 摇摆灯泡：对标网站右上角挂牌，红底白字，绕顶部中点 ±18° 摇摆，点按进 ChatGPT 玩法 ----
@Composable
fun SwingBulb(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val swing = rememberInfiniteTransition(label = "bulbSwing")
    val angle by swing.animateFloat(
        initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse),
        label = "bulbAngle"
    )
    Column(
        modifier = modifier
            .graphicsLayer(rotationZ = angle, transformOrigin = TransformOrigin(0.5f, 0f))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.width(2.dp).height(10.dp).background(Color(0xFF9CA3AF)))
        Box(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("ChatGPT玩法", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ---- 启动页描画：对标网站启动页，L/D 描画 + Z 旋转 + 渐变站名 ----
@Composable
fun SplashContent() {
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        draw.animateTo(1f, animationSpec = tween(1500, easing = EaseInOut))
    }
    val spin = rememberInfiniteTransition(label = "splashSpin")
    val zRot by spin.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "zRot"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LetterCanvas(
                progress = draw.value, strokeWidth = 8f,
                brush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF06B6D4)))
            ) { s ->
                Path().apply {
                    moveTo(20f * s, 20f * s)
                    lineTo(20f * s, 87f * s)
                    lineTo(80f * s, 87f * s)
                }
            }
            LetterCanvas(
                progress = draw.value, strokeWidth = 12f,
                brush = Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))
            ) { s ->
                Path().apply {
                    moveTo(28f * s, 20f * s)
                    lineTo(52f * s, 20f * s)
                    arcTo(Rect(19f * s, 20f * s, 85f * s, 86f * s), -90f, 180f, false)
                    lineTo(28f * s, 86f * s)
                    close()
                }
            }
            Box(modifier = Modifier.graphicsLayer(rotationZ = zRot)) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    val s = size.width / 100f
                    val path = Path().apply {
                        moveTo(20f * s, 20f * s)
                        lineTo(80f * s, 20f * s)
                        lineTo(20f * s, 87f * s)
                        lineTo(80f * s, 87f * s)
                    }
                    drawPath(
                        path, Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
                        style = Stroke(width = 11f * s)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "懒得找了",
            style = TextStyle(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF16A34A), Color(0xFF0EA5E9), Color(0xFF8B5CF6), Color(0xFF16A34A))
                ),
                fontSize = 34.sp, fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun LetterCanvas(progress: Float, strokeWidth: Float, brush: Brush, build: (Float) -> Path) {
    Canvas(modifier = Modifier.size(72.dp)) {
        val s = size.width / 100f
        val path = build(s)
        val len = AndroidPathMeasure(path.asAndroidPath(), false).length
        drawPath(
            path = path, brush = brush,
            style = Stroke(
                width = strokeWidth * s,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(len, len), len * (1f - progress))
            )
        )
    }
}
