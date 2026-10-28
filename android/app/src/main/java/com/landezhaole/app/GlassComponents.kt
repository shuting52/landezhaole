package com.landezhaole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex

// ============================================================
// GlassChat 玻璃原生组件库
// 语言：极光底 + 渐变着色 + 内高光 + 渐变描边 + 深色投影。
// 自包含：只用本工程已有的 Compose 依赖，不加 Haze、不加 AGSL、不改构建配置。
// 用法：任何页面直接调用，例如 GlassCard { Text("...") }、GlassButton("确定") { }。
// 注意：本套是固定深色玻璃语言（白字），适合放在极光底或深色底上。
// ============================================================

/** 玻璃主渐变：按钮、选中态、进度条统一用这条蓝紫渐变。 */
val GlassAccentGradient: Brush
    get() = Brush.horizontalGradient(
        listOf(Color(0xFF60A5FA), Color(0xFF1668DC), Color(0xFF7C3AED))
    )

/** 第 1 层：半透明渐变着色（必须是渐变，纯色会变灰蒙版）。 */
private fun Modifier.glassTint(tintAlpha: Float): Modifier = this.background(
    Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = (tintAlpha * 1.15f).coerceAtMost(1f)),
            Color.White.copy(alpha = (tintAlpha * 0.95f).coerceAtMost(1f)),
            Color.White.copy(alpha = (tintAlpha * 1.05f).coerceAtMost(1f)),
        )
    )
)

/** 第 2 层：内高光，顶部亮、中间透明、底部微亮，制造厚度错觉。 */
private fun glassSheen(): Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color.White.copy(alpha = 0.16f),
        0.18f to Color.White.copy(alpha = 0.05f),
        0.55f to Color.White.copy(alpha = 0.00f),
        1.00f to Color.White.copy(alpha = 0.05f),
    )
)

/** 第 3 层：渐变描边，左上角最亮、右下角次亮、中间几乎透明。 */
fun Modifier.glassBorder(
    cornerRadius: Dp,
    width: Dp = 1.dp,
    edgeHighlight: Float = 0.85f,
): Modifier = drawWithContent {
    drawContent()
    val w = width.toPx()
    if (w <= 0f) return@drawWithContent
    val r = (cornerRadius.toPx() - w / 2f).coerceAtLeast(0f)
    drawRoundRect(
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = edgeHighlight.coerceIn(0f, 1f)),
                0.10f to Color.White.copy(alpha = 0.30f),
                0.32f to Color.White.copy(alpha = 0.05f),
                0.60f to Color.White.copy(alpha = 0.03f),
                0.84f to Color.White.copy(alpha = 0.28f),
                1.00f to Color.White.copy(alpha = 0.72f),
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        topLeft = Offset(w / 2f, w / 2f),
        size = Size((size.width - w).coerceAtLeast(0f), (size.height - w).coerceAtLeast(0f)),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = w),
    )
}

/**
 * 玻璃三件套合一：投影（必须在 clip 之前）+ 裁圆角 + 渐变着色 + 内高光 + 渐变描边。
 * 任何自定义玻璃容器都可以用它，不必重复拼这五层。
 */
fun Modifier.glassCardStyle(
    cornerRadius: Dp = 20.dp,
    tintAlpha: Float = 0.10f,
    elevation: Dp = 16.dp,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(
            elevation = elevation, shape = shape, clip = false,
            ambientColor = Color.Black.copy(alpha = 0.30f),
            spotColor = Color.Black.copy(alpha = 0.55f),
        )
        .clip(shape)
        .glassTint(tintAlpha)
        .background(glassSheen())
        .glassBorder(cornerRadius = cornerRadius, width = 1.dp)
}

/** 极光底：暗底 + 四色光斑 + 弧形光带 + 底部压暗，玻璃组件的标配背景。 */
@Composable
fun GlassAuroraBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xFF100A16))
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1B1030), Color(0xFF3A1E52), Color(0xFF6E293C)),
                        start = Offset.Zero,
                        end = Offset(w, h),
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF9B4A).copy(alpha = 0.80f), Color.Transparent),
                        center = Offset(w * 0.14f, h * 0.30f), radius = h * 0.28f,
                    ),
                    radius = h * 0.28f, center = Offset(w * 0.14f, h * 0.30f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3FD2E8).copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(w * 0.98f, h * 0.08f), radius = h * 0.22f,
                    ),
                    radius = h * 0.22f, center = Offset(w * 0.98f, h * 0.08f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF5A8A).copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(w * 0.86f, h * 0.60f), radius = h * 0.24f,
                    ),
                    radius = h * 0.24f, center = Offset(w * 0.86f, h * 0.60f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF43E0A0).copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(w * 0.30f, h * 0.88f), radius = h * 0.22f,
                    ),
                    radius = h * 0.22f, center = Offset(w * 0.30f, h * 0.88f),
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.07f),
                    startAngle = 195f, sweepAngle = 130f, useCenter = false,
                    topLeft = Offset(w * 0.30f, -h * 0.12f), size = Size(w * 1.25f, h * 0.95f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.05f),
                    startAngle = 200f, sweepAngle = 120f, useCenter = false,
                    topLeft = Offset(w * 0.42f, h * 0.02f), size = Size(w * 1.05f, h * 0.78f),
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
                )
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0A0610).copy(alpha = 0.70f)),
                        startY = h * 0.45f, endY = h,
                    )
                )
            }
    )
}

/** 玻璃卡片：最常用的容器，点按可选。 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    tintAlpha: Float = 0.10f,
    elevation: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val base = modifier.glassCardStyle(cornerRadius, tintAlpha, elevation)
    Box(
        modifier = if (onClick != null) base.clickable { onClick() } else base,
        content = content,
    )
}

/** 主按钮：蓝紫渐变底 + 玻璃描边 + 白字，胶囊形。 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp, shape = shape, clip = false,
                ambientColor = Color.Black.copy(alpha = 0.30f),
                spotColor = Color.Black.copy(alpha = 0.55f),
            )
            .clip(shape)
            .background(GlassAccentGradient)
            .glassBorder(cornerRadius = 50.dp, width = 1.dp)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
    }
}

/** 描边按钮：透明玻璃底 + 白字，常用于次要操作。 */
@Composable
fun GlassOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .glassTint(0.10f)
            .background(glassSheen())
            .glassBorder(cornerRadius = 50.dp, width = 1.dp)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = if (enabled) 0.95f else 0.5f),
            fontSize = 15.sp, fontWeight = FontWeight.Medium,
        )
    }
}

/** 小胶囊标签，例如 demo 里的“企业好友”。 */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .glassTint(0.14f)
            .background(glassSheen())
            .glassBorder(cornerRadius = 50.dp, width = 1.dp)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 14.sp, fontWeight = FontWeight.Medium,
        )
    }
}

/** 小角标，例如数量、新消息数。 */
@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    bg: Brush? = null,
    textColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg ?: if (accent) GlassAccentGradient else Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f))
            ))
            .glassBorder(cornerRadius = 50.dp, width = 1.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** 筛选芯片：选中为蓝紫渐变，未选中为透明玻璃。 */
@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(GlassAccentGradient)
                else Modifier.glassTint(0.10f).background(glassSheen())
            )
            .glassBorder(cornerRadius = 50.dp, width = 1.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = if (selected) 1f else 0.7f),
            fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/** 分段切换：一排胶囊放在一个玻璃条里，选中项为蓝紫渐变。 */
@Composable
fun GlassTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .glassTint(0.08f)
            .background(glassSheen())
            .glassBorder(cornerRadius = 50.dp, width = 1.dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .then(if (selected) Modifier.background(GlassAccentGradient) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = if (selected) 1f else 0.65f),
                    fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** 玻璃输入框：透明玻璃底 + 白字 + 占位灰字。 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .glassTint(0.10f)
            .background(glassSheen())
            .glassBorder(cornerRadius = 16.dp, width = 1.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(text = placeholder, color = Color.White.copy(alpha = 0.45f), fontSize = 14.sp)
                }
                innerTextField()
            },
        )
    }
}

/** 玻璃搜索框：放大镜 + 输入 + 清除，搜索页、列表页通用。 */
@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .glassTint(0.10f)
            .background(glassSheen())
            .glassBorder(cornerRadius = 50.dp, width = 1.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search, contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = Color.White.copy(alpha = 0.45f), fontSize = 14.sp)
                    }
                    innerTextField()
                },
            )
        }
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Close, contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp).clickable { onValueChange("") },
            )
        }
        if (onSearch != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "搜索",
                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSearch() },
            )
        }
    }
}

/** 圆形头像占位：渐变底 + 白字，不依赖图片资源。 */
@Composable
fun GlassAvatar(
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    colors: List<Color> = listOf(Color(0xFF8E6CEF), Color(0xFF3B1E6E)),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors))
            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 头像组：几个圆头像左右交叠，右边数字表示还有多少人。 */
@Composable
fun GlassAvatarStack(
    labels: List<String>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 30.dp,
    overlap: Dp = 10.dp,
    extraCount: Int = 0,
) {
    val palette = listOf(
        listOf(Color(0xFF8E6CEF), Color(0xFF3B1E6E)),
        listOf(Color(0xFFFF9A4D), Color(0xFFB33A3A)),
        listOf(Color(0xFF32D0C0), Color(0xFF13596B)),
    )
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(avatarSize)
                .width(avatarSize * labels.size - overlap * (labels.size - 1).coerceAtLeast(0))
        ) {
            labels.forEachIndexed { i, label ->
                GlassAvatar(
                    label = label,
                    colors = palette[i % palette.size],
                    size = avatarSize,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = (avatarSize - overlap) * i)
                        .zIndex((labels.size - i).toFloat()),
                )
            }
        }
        if (extraCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "+$extraCount",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
            )
        }
    }
}

/** 玻璃列表行：图标圆盘 + 标题 + 副标题 + 右侧箭头/角标，站点分类、设置页通用。 */
@Composable
fun GlassListRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    iconColors: List<Color> = listOf(Color(0xFF8E6CEF), Color(0xFF3B1E6E)),
    iconTint: Color = Color.White,
    titleColor: Color = Color.White,
    subtitleColor: Color = Color.White.copy(alpha = 0.6f),
    arrowTint: Color = Color.White.copy(alpha = 0.6f),
    tintAlpha: Float = 0.10f,
    badgeAccent: Boolean = false,
    badgeBg: Brush? = null,
    badgeTextColor: Color = Color.White,
) {
    Row(
        modifier = modifier
            .glassCardStyle(cornerRadius = 20.dp, tintAlpha = tintAlpha, elevation = 16.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(Brush.linearGradient(iconColors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = titleColor,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle, fontSize = 12.sp, color = subtitleColor,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (badge != null) {
            Spacer(modifier = Modifier.width(8.dp))
            GlassBadge(text = badge, accent = badgeAccent, bg = badgeBg, textColor = badgeTextColor)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = null,
            tint = arrowTint, modifier = Modifier.size(22.dp),
        )
    }
}

/** 分区标题：白字粗体 + 右侧可选操作文字。 */
@Composable
fun GlassSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    color: Color = Color.White,
    actionColor: Color = Color.White.copy(alpha = 0.7f),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                color = actionColor, fontSize = 13.sp,
                modifier = Modifier.clickable { onAction() },
            )
        }
    }
}

/** 底栏项：图标 + 文字。 */
data class GlassBarItem(val label: String, val icon: ImageVector)

/** 悬浮胶囊底栏：玻璃条 + 选中项蓝紫药丸，适合取代系统 NavigationBar 的玻璃页。 */
@Composable
fun GlassBottomBar(
    items: List<GlassBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .glassTint(0.12f)
            .background(glassSheen())
            .glassBorder(cornerRadius = 32.dp, width = 1.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            val selected = i == selectedIndex
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .then(if (selected) Modifier.background(GlassAccentGradient) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = item.icon, contentDescription = null,
                    tint = Color.White.copy(alpha = if (selected) 1f else 0.65f),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label,
                    color = Color.White.copy(alpha = if (selected) 1f else 0.65f),
                    fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** 玻璃弹窗：标题 + 正文 + 取消/确认两个按钮。 */
@Composable
fun GlassDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    body: String? = null,
    dismissText: String = "取消",
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(cornerRadius = 28.dp, tintAlpha = 0.14f, elevation = 26.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            ) {
                Text(text = title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (body != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = body, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlassOutlineButton(
                        text = dismissText, onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        text = confirmText, onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** 玻璃开关行：标题 + 副标题 + 右侧开关，设置页通用。 */
@Composable
fun GlassSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .glassCardStyle(cornerRadius = 20.dp, tintAlpha = 0.10f, elevation = 16.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF60A5FA),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.20f),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** 玻璃滑动条行：标题 + 当前值 + 滑动条，缩放、透明度这类设置通用。 */
@Composable
fun GlassSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueText: String? = null,
) {
    Column(
        modifier = modifier
            .glassCardStyle(cornerRadius = 20.dp, tintAlpha = 0.10f, elevation = 16.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (valueText != null) {
                Text(text = valueText, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF60A5FA),
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
            ),
        )
    }
}

/** 玻璃进度条：蓝紫进度 + 半透明白轨道。 */
@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
        color = Color(0xFF60A5FA),
        trackColor = Color.White.copy(alpha = 0.15f),
    )
}

/** 空态文字：居中半透明白字，列表无数据时用。 */
@Composable
fun GlassEmptyText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
    }
}
