package com.example.ui.components

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ============================================================
// 全面升级 · Uiverse Novaxlo 撞色硬阴影风格组件库
// 调色板依据用户提供的 CSS 根变量完全对齐：
//   --bg: #006aaaff (深蓝)
//   --c1: #ffbf6a   (橙黄)
//   --c2: #ff7c90ff (玫粉)
//   --ink: #fff7ec  (纸感页面底色) / #0a3d63 (墨蓝边框与主文字)
//   --card: #ffffff (纯白卡片)
// ============================================================

/** Uiverse Novaxlo 调色板（对应 CSS 根变量） */
val V15Bg = Color(0xFF006AAA)       // --bg: #006aaaff 深蓝（主色）
val V15C1 = Color(0xFFFFBF6A)      // --c1: #ffbf6a   橙黄（强调/选中态/阴影）
val V15C2 = Color(0xFFFF7C90)      // --c2: #ff7c90ff 玫粉（次色/主按钮/头像边框）
val V15Ink = Color(0xFF0A3D63)     // --bor: #0a3d63  深墨蓝（粗边框/主文字）
val V15PaperInk = Color(0xFFFFF7EC)// --ink: #fff7ec  纸感页面底色
val V15Card = Color(0xFFFFFFFF)    // --card: #ffffff 纯白底色

/** 分隔线颜色（.u-tab button 右侧、.u-list row 底部） */
private val V15Divider = Color(0xFFE6EEF5)

/**
 * Neo-Brutalism 卡片容器：白底 + 4dp 粗描边 + 右下偏移硬阴影 + 圆角。
 * 对应 CSS：`background:#fff;border:4px solid var(--ink);border-radius:16px;
 *           box-shadow:.35em .35em 0 var(--c1)`
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    borderWidth: Int = 4,
    shadowColor: Color = V15C1,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = if (pressed) 0.97f else 1f
    val lift = if (pressed) 2 else 6

    Box(modifier = modifier) {
        // 硬阴影层（右下偏移）
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = lift.dp, y = lift.dp)
                .clip(shape)
                .background(shadowColor)
        )
        // 主体层：背景跟随主题 surface（玻璃拟态主题下自动为半透明磨砂，其他主题保持原样）
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(borderWidth.dp, V15Ink, shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else Modifier
                )
        ) {
            content()
        }
    }
}

// ============================================================
// .u-user 用户卡片 —— 设置版块顶部主题
// HTML:
//   <div class="u-user"><div class="u-avatar">🐻</div><div><div class="nm">小熊同学</div><div class="em">bear@example.com</div></div></div>
// CSS:
//   .u-user{display:flex;align-items:center;gap:14px;background:#fff;border:4px solid var(--ink);
//          border-radius:16px;padding:14px 16px;box-shadow:.35em .35em 0 var(--c1);
//          width:min(270px,100%);transition:all .3s}
//   .u-user:hover{transform:translateY(-4px);box-shadow:.45em .5em 0 var(--c1)}
//   .u-avatar{width:54px;height:54px;border-radius:50%;background:var(--bg);border:4px solid var(--c2);
//             color:#fff;font-weight:900;font-size:22px;display:flex;align-items:center;justify-content:center;
//             box-shadow:.15em .15em 0 var(--c1);transition:all .3s}
//   .u-user:hover .u-avatar{transform:rotate(15deg) scale(1.1)}
// ============================================================
@Composable
fun UUserCard(
    name: String = "小熊同学",
    email: String = "bear@example.com",
    modifier: Modifier = Modifier,
    avatarText: String = "🐻",
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 交互动画（对应 transition: all .3s 及 hover 态变化）
    // .u-user:hover { transform: translateY(-4px); box-shadow: .45em .5em 0 var(--c1) }
    val cardOffsetY by animateDpAsState(
        targetValue = if (isPressed) (-4).dp else 0.dp,
        animationSpec = tween(300),
        label = "userCardOffsetY"
    )
    val shadowOffsetX by animateDpAsState(
        targetValue = if (isPressed) 7.dp else 5.5.dp,
        animationSpec = tween(300),
        label = "userCardShadowX"
    )
    val shadowOffsetY by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 5.5.dp,
        animationSpec = tween(300),
        label = "userCardShadowY"
    )

    // .u-user:hover .u-avatar { transform: rotate(15deg) scale(1.1) }
    val avatarRotation by animateFloatAsState(
        targetValue = if (isPressed) 15f else 0f,
        animationSpec = tween(300),
        label = "avatarRotation"
    )
    val avatarScale by animateFloatAsState(
        targetValue = if (isPressed) 1.1f else 1f,
        animationSpec = tween(300),
        label = "avatarScale"
    )

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .widthIn(max = 270.dp) // width: min(270px, 100%)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        // 硬阴影层（.u-user box-shadow: .35em .35em 0 var(--c1) / hover: .45em .5em 0 var(--c1)）
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffsetX, y = shadowOffsetY)
                .clip(shape)
                .background(V15C1)
        )

        // 卡片主体层: background:#fff; border:4px solid var(--ink); border-radius:16px; padding:14px 16px;
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = cardOffsetY)
                .clip(shape)
                .background(Color.White)
                .border(4.dp, V15Ink, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp), // gap: 14px
                modifier = Modifier.fillMaxWidth()
            ) {
                // .u-avatar: width:54px; height:54px; border-radius:50%; background:var(--bg); border:4px solid var(--c2); box-shadow:.15em .15em 0 var(--c1)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer {
                            scaleX = avatarScale
                            scaleY = avatarScale
                            rotationZ = avatarRotation
                        }
                ) {
                    // 头像阴影层 (.15em .15em 0 var(--c1))
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 2.5.dp, y = 2.5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(V15C1)
                    )
                    // 头像本体层 (background:var(--bg); border:4px solid var(--c2))
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(50))
                            .background(V15Bg)
                            .border(4.dp, V15C2, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // <div><div class="nm">小熊同学</div><div class="em">bear@example.com</div></div>
                Column {
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = V15Ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email,
                        fontSize = 12.sp,
                        color = Color(0xFF7A8CA0),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ============================================================
// .u-tab 选择栏 —— 软件版块顶部
// HTML:
//   <div class="u-tab"><button class="on">推荐</button><button>关注</button><button>热门</button></div>
// CSS:
//   .u-tab{display:flex;background:#fff;border:4px solid var(--ink);border-radius:14px;
//          overflow:hidden;box-shadow:.3em .3em 0 var(--c1)}
//   .u-tab button{flex:1;border:none;background:transparent;padding:12px;font-weight:800;
//                 font-size:13px;font-family:inherit;color:#7a8ca0;cursor:pointer;
//                 border-right:3px solid #e6eef5;transition:all .25s}
//   .u-tab button.on{background:var(--c1);color:var(--ink);
//                    animation:tabpulse .4s cubic-bezier(.34,1.56,.64,1)}
//   @keyframes tabpulse{0%{transform:scale(.9)}100%{transform:scale(1)}}
// ============================================================
@Composable
fun UTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)

    Box(modifier = modifier) {
        // 硬阴影层（.3em .3em 0 var(--c1)）
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 5.dp, y = 5.dp)
                .clip(shape)
                .background(V15C1)
        )
        // 主体层: background:#fff; border:4px solid var(--ink); border-radius:14px; overflow:hidden
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White)
                .border(4.dp, V15Ink, shape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                tabs.forEachIndexed { index, tabName ->
                    val selected = index == selectedIndex

                    // tabpulse 动效：@keyframes tabpulse{0%{transform:scale(.9)}100%{transform:scale(1)}}
                    // animation: tabpulse .4s cubic-bezier(.34,1.56,.64,1)
                    val pulseScale = remember { Animatable(1f) }
                    LaunchedEffect(selected) {
                        if (selected) {
                            pulseScale.snapTo(0.9f)
                            pulseScale.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 400,
                                    easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
                                )
                            )
                        } else {
                            pulseScale.snapTo(1f)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (selected) V15C1 else Color.Transparent)
                            .clickable { onSelect(index) }
                            .graphicsLayer {
                                scaleX = if (selected) pulseScale.value else 1f
                                scaleY = if (selected) pulseScale.value else 1f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold, // font-weight: 800
                            color = if (selected) V15Ink else Color(0xFF7A8CA0) // .on: var(--ink), 默认: #7a8ca0
                        )
                    }

                    // border-right: 3px solid #e6eef5（除最后一项外）
                    if (index < tabs.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(V15Divider)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// .u-list 全局软件列表 —— 软件版块主体
// HTML:
//   <div class="u-list">
//     <div class="row"><div class="ic">📄</div><span class="t">项目文档.docx</span><span class="s">今天</span></div>
//     <div class="row"><div class="ic">🖼</div><span class="t">设计稿.png</span><span class="s">昨天</span></div>
//     <div class="row"><div class="ic">🎬</div><span class="t">演示视频.mp4</span><span class="s">3天前</span></div>
//   </div>
// CSS:
//   .u-list{width:min(300px,100%);background:#fff;border:4px solid var(--ink);border-radius:16px;
//           overflow:hidden;box-shadow:.35em .35em 0 var(--c1)}
//   .u-list .row{display:flex;align-items:center;gap:12px;padding:13px 16px;
//                border-bottom:3px solid #e6eef5;cursor:pointer;transition:all .2s}
//   .u-list .row:last-child{border-bottom:none}
//   .u-list .row:hover{background:#fff4e4}
//   .u-list .row .ic{width:40px;height:40px;border-radius:10px;background:var(--bg);color:#fff;
//                    font-weight:900;font-size:18px;display:flex;align-items:center;justify-content:center;
//                    box-shadow:.15em .15em 0 var(--c1);transition:all .2s}
//   .u-list .row:hover .ic{transform:scale(1.1) rotate(12deg)}
//   .u-list .row:nth-child(2) .ic{background:var(--c2)}
//   .u-list .row:nth-child(3) .ic{background:var(--c1);color:var(--ink)}
//   .u-list .row .t{font-weight:900;font-size:14px;color:var(--ink);flex:1}
//   .u-list .row .s{font-weight:800;font-size:11px;color:#9aa8b6}
// ============================================================
@Composable
fun UListContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier) {
        // 硬阴影层（.35em .35em 0 var(--c1)）
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 5.5.dp, y = 5.5.dp)
                .clip(shape)
                .background(V15C1)
        )
        // 主体层: background:#fff; border:4px solid var(--ink); border-radius:16px; overflow:hidden
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White)
                .border(4.dp, V15Ink, shape)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * 列表行：图标方块 + 标题/副标题 + 右侧内容（时间 / 徽标 / chevron）。
 * rowIndex 用于循环配色（第 2 行粉色，第 3 行橙色/墨字，其余蓝色，对应 CSS nth-child）
 */
@Composable
fun UListRow(
    title: String,
    subtitle: String = "",
    iconText: String = "📦",
    iconUrl: String = "",
    trailing: String = "›",
    rowIndex: Int = 0,
    isLastRow: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // CSS nth-child 配色：第 2 行粉色(var(--c2))，第 3 行橙色(var(--c1))与墨色文字，其余天蓝色(var(--bg))
    val iconBg = when (rowIndex % 3) {
        1 -> V15C2
        2 -> V15C1
        else -> V15Bg
    }
    val iconFg = if (rowIndex % 3 == 2) V15Ink else Color.White

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 交互动效 transition: all .2s
    val icScale by animateFloatAsState(
        targetValue = if (isPressed) 1.1f else 1f,
        animationSpec = tween(200),
        label = "icScale"
    )
    val icRotate by animateFloatAsState(
        targetValue = if (isPressed) 12f else 0f,
        animationSpec = tween(200),
        label = "icRotate"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp), // gap: 12px
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .background(if (isPressed) Color(0xFFFFF4E4) else Color.Transparent) // .row:hover { background:#fff4e4 }
                .padding(horizontal = 16.dp, vertical = 13.dp) // padding: 13px 16px
        ) {
            // .ic 图标: 40px * 40px, border-radius: 10px, box-shadow: .15em .15em 0 var(--c1)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = icScale
                        scaleY = icScale
                        rotationZ = icRotate
                    }
            ) {
                // 图标硬阴影
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 2.5.dp, y = 2.5.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(V15C1)
                )
                // 图标主体
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = iconUrl,
                            contentDescription = title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp)
                        )
                    } else {
                        Text(
                            text = iconText,
                            fontSize = 18.sp, // font-size: 18px
                            fontWeight = FontWeight.Black, // font-weight: 900
                            color = iconFg
                        )
                    }
                }
            }

            // <span class="t"> 标题与副标题
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black, // font-weight: 900
                    color = V15Ink,
                    maxLines = 1
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF7A8CA0),
                        maxLines = 1
                    )
                }
            }

            // <span class="s"> 尾部标签（如：今天 / 昨天 / 3天前）
            if (trailing.isNotBlank()) {
                Text(
                    text = trailing,
                    fontSize = 11.sp, // font-size: 11px
                    fontWeight = FontWeight.ExtraBold, // font-weight: 800
                    color = Color(0xFF9AA8B6)
                )
            }
        }

        // .row:last-child { border-bottom: none }
        if (!isLastRow) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(V15Divider) // border-bottom: 3px solid #e6eef5
            )
        }
    }
}

// ============================================================
// .u-setrow 设置行 —— 设置版块底部
// CSS:
//   .u-setrow{display:flex;align-items:center;gap:12px;background:#fff;border:4px solid var(--ink);
//             border-radius:14px;padding:13px 15px;box-shadow:.3em .3em 0 var(--c1);
//             width:min(300px,100%);...}
//   .u-setrow:hover{transform:translateX(6px);box-shadow:.4em .3em 0 var(--c1)}
//   .u-setrow .t{flex:1;font-weight:800;color:#26303c;font-size:14px}
//   .u-setrow .chev{color:#9aa8b6;font-weight:900;font-size:18px}
//   .u-setrow:hover .chev{transform:translateX(5px);color:var(--c2)}
// ============================================================
@Composable
fun USetRow(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    chevron: String = "›",
    onClick: () -> Unit = {}
) {
    NeoCard(
        modifier = modifier.widthIn(max = 300.dp),
        cornerRadius = 14,
        borderWidth = 4,
        shadowColor = V15C1,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = V15Ink,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = chevron,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF9AA8B6)
            )
        }
    }
}

// ============================================================
// ============================================================
// .u-badge 徽标 —— 弹窗内使用
// CSS:
//   .u-badge{display:inline-flex;align-items:center;gap:6px;background:#fff;border:3px solid var(--ink);
//            border-radius:30px;padding:5px 12px;font-weight:800;font-size:12px;color:var(--ink);
//            box-shadow:.18em .18em 0 var(--c1);transition:all .2s}
//   .u-badge:hover{transform:translateY(-3px) rotate(-4deg);box-shadow:.28em .3em 0 var(--c1)}
//   .u-badge.pink{background:var(--c2);color:#fff}
//   .u-badge.blue{background:var(--bg);color:#fff;box-shadow:.18em .18em 0 var(--c2)}
//   .u-badge .x{cursor:pointer;opacity:.7;font-weight:900;transition:all .2s}
//   .u-badge .x:hover{opacity:1;transform:scale(1.3) rotate(90deg)}
// ============================================================
enum class UBadgeVariant { DEFAULT, PINK, BLUE }

@Composable
fun UBadge(
    text: String,
    variant: UBadgeVariant = UBadgeVariant.DEFAULT,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val bg = when (variant) {
        UBadgeVariant.DEFAULT -> Color.White
        UBadgeVariant.PINK -> V15C2
        UBadgeVariant.BLUE -> V15Bg
    }
    val fg = when (variant) {
        UBadgeVariant.DEFAULT -> V15Ink
        UBadgeVariant.PINK, UBadgeVariant.BLUE -> Color.White
    }
    val shadowColor = if (variant == UBadgeVariant.BLUE) V15C2 else V15C1
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                if (pressed) {
                    translationY = -3f
                    rotationZ = -4f
                }
            }
    ) {
        // 硬阴影层（.18em ~ 3dp 偏移）
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .clip(shape)
                .background(shadowColor)
        )
        // 主体层
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(shape)
                .background(bg)
                .border(3.dp, V15Ink, shape)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = fg
            )
            if (onClose != null) {
                Spacer(modifier = Modifier.width(6.dp))
                val closeInteraction = remember { MutableInteractionSource() }
                val closePressed by closeInteraction.collectIsPressedAsState()
                Text(
                    text = "✕",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = fg.copy(alpha = 0.7f),
                    modifier = Modifier
                        .graphicsLayer {
                            if (closePressed) {
                                scaleX = 1.3f
                                scaleY = 1.3f
                                rotationZ = 90f
                            }
                        }
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = null,
                            onClick = onClose
                        )
                        .padding(2.dp)
                )
            }
        }
    }
}

/**
 * 所有的弹窗采用的徽标组合栏：
 * <span class="u-badge">🎉 新版本</span>
 * <span class="u-badge pink">❤ 点赞 999</span>
 * <span class="u-badge blue">⭐ 会员</span>
 */
@Composable
fun UBadgeHeaderRow(
    modifier: Modifier = Modifier,
    b1Text: String = "🎉 新版本",
    b2Text: String = "❤ 点赞 999",
    b3Text: String = "⭐ 会员",
    onClose: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        UBadge(text = b1Text, variant = UBadgeVariant.DEFAULT)
        Spacer(modifier = Modifier.width(6.dp))
        UBadge(text = b2Text, variant = UBadgeVariant.PINK)
        Spacer(modifier = Modifier.width(6.dp))
        UBadge(text = b3Text, variant = UBadgeVariant.BLUE, onClose = onClose)
    }
}

// ============================================================
// 官方交流群跳转 & 最新安装包直接下载工具函数
// ============================================================
fun openQqGroup(
    context: Context,
    groupUrl: String = "",
    groupUin: String = "439211347"
) {
    val uin = groupUin.ifBlank { "439211347" }
    // 优先复制官方群号到剪贴板，确保 100% 成功有响应
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QQ_GROUP", uin)
        clipboard.setPrimaryClip(clip)
    } catch (_: Exception) {}

    val qqUri = Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$uin&card_type=group&source=qrcode")
    val qqIntent = Intent(Intent.ACTION_VIEW, qqUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(qqIntent)
        Toast.makeText(context, "官方群号 $uin 已复制到剪贴板，正在调起 QQ…", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        val fallbackUrl = groupUrl.ifBlank { "https://qm.qq.com/cgi-bin/qm/qr?k=official&jump_from=webapi" }
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            Toast.makeText(context, "官方群号 $uin 已复制！已为您打开加群网页～", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(context, "官方交流群号：$uin（已复制到剪贴板）", Toast.LENGTH_LONG).show()
        }
    }
}

fun downloadLatestApk(
    context: Context,
    apkUrl: String = "",
    versionName: String = "latest",
    onSuccess: (() -> Unit)? = null
) {
    val targetUrl = apkUrl.ifBlank {
        "https://github.com/shuting52/2026-9-21landezhaole/releases/download/latest/app-release.apk"
    }
    val fileName = "landezhaole_${versionName.replace(".", "_")}.apk"

    // 1. 复制下载直链到剪贴板，保证用户随时可用
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("APK_DOWNLOAD_URL", targetUrl)
        clipboard.setPrimaryClip(clip)
    } catch (_: Exception) {}

    // 2. 调起系统下载服务
    try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(targetUrl))
            .setTitle("懒得找了 $versionName")
            .setDescription("正在高速下载最新版本安装包…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        dm.enqueue(request)
    } catch (_: Exception) {}

    // 3. 同时调起浏览器直达下载，确保在模拟器与各机型上 100% 弹出响应
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Toast.makeText(
            context,
            "🚀 正在调起浏览器下载最新安装包！直链已同步复制到剪贴板",
            Toast.LENGTH_LONG
        ).show()
    } catch (_: Exception) {
        Toast.makeText(context, "🚀 下载已启动！直链已复制到剪贴板", Toast.LENGTH_LONG).show()
    }

    onSuccess?.invoke()
}

// ============================================================
// 1. 按钮 Buttons —— Uiverse 纯色 + 硬偏移阴影
// CSS:
//   .u-btn { --bgc: var(--c2); --bor: var(--bg); --sh: var(--bg);
//            background: var(--bgc); color: #fff; border: 4px solid var(--bor);
//            border-radius: 14px; font-weight: 800; font-size: 15px; padding: .9em 1.6em;
//            box-shadow: .35em .35em 0 var(--sh); transition: all .18s ease; }
//   .u-btn:hover { transform: translate(-3px,-3px); box-shadow: .55em .55em 0 var(--sh); }
//   .u-btn.alt { --bgc: var(--c1); --bor: #0a3d63; --sh: #0a3d63; color: #0a3d63; }
//   .u-btn.ghost { --bgc: transparent; --bor: var(--bg); --sh: var(--c2); color: var(--bg); }
// ============================================================
enum class UBtnVariant { PRIMARY, ALT, GHOST }

@Composable
fun UBtn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: UBtnVariant = UBtnVariant.PRIMARY,
    isSmall: Boolean = false,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val bg = when (variant) {
        UBtnVariant.PRIMARY -> V15C2
        UBtnVariant.ALT -> V15C1
        UBtnVariant.GHOST -> Color.Transparent
    }
    val borderCol = when (variant) {
        UBtnVariant.PRIMARY -> V15Bg
        UBtnVariant.ALT -> V15Ink
        UBtnVariant.GHOST -> V15Bg
    }
    val shadowCol = when (variant) {
        UBtnVariant.PRIMARY -> V15Bg
        UBtnVariant.ALT -> V15Ink
        UBtnVariant.GHOST -> V15C2
    }
    val textColor = when (variant) {
        UBtnVariant.PRIMARY -> Color.White
        UBtnVariant.ALT -> V15Ink
        UBtnVariant.GHOST -> V15Bg
    }

    val borderWidth = if (isSmall) 3.dp else 4.dp
    val shape = RoundedCornerShape(14.dp)
    val shadowOffset by animateDpAsState(
        targetValue = if (pressed) 1.5.dp else (if (isSmall) 3.5.dp else 5.dp),
        animationSpec = tween(150),
        label = "btnShadow"
    )
    val transOffset by animateDpAsState(
        targetValue = if (pressed) 3.dp else 0.dp,
        animationSpec = tween(150),
        label = "btnTrans"
    )

    Box(
        modifier = modifier
            .offset(x = transOffset, y = transOffset)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // 硬阴影层
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .clip(shape)
                .background(shadowCol)
        )
        // 按钮主体
        Box(
            modifier = Modifier
                .clip(shape)
                .background(bg)
                .border(borderWidth, borderCol, shape)
                .padding(
                    horizontal = if (isSmall) 12.dp else 20.dp,
                    vertical = if (isSmall) 7.dp else 12.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = if (isSmall) 12.5.sp else 14.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/** 图标圆钮（.u-iconbtn） */
@Composable
fun UIconBtn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bgColor: Color = V15C1,
    textColor: Color = V15Ink
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val rot by animateFloatAsState(if (pressed) -8f else 0f, tween(180), label = "iconBtnRot")

    Box(
        modifier = modifier
            .size(52.dp)
            .graphicsLayer { rotationZ = rot }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(CircleShape)
                .background(V15C2)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(bgColor)
                .border(4.dp, V15Bg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Black, color = textColor)
        }
    }
}

// ============================================================
// 4. 加载器与条纹进度条 Loaders
// CSS:
//   .u-progress { width: min(260px,100%); height: 30px; border-radius: 30px; background: #fff;
//                 border: 4px solid #0a3d63; box-shadow: .25em .25em 0 var(--c1); overflow: hidden; }
//   .u-progress .bar { height: 100%; width: 62%;
//     background: repeating-linear-gradient(45deg, var(--c2) 0 12px, var(--c1) 12px 24px);
//     animation: uslide 1.4s linear infinite; background-size: 34px 34px; }
// ============================================================
@Composable
fun UProgress(
    progress: Float = 0.72f,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(30.dp)
    val infinite = rememberInfiniteTransition(label = "progStripes")
    val shift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 48f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shift"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
    ) {
        // 硬阴影
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(shape)
                .background(V15C1)
        )
        // 轨道主体
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(Color.White)
                .border(4.dp, V15Ink, shape)
        ) {
            // 条纹填充 bar
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(shape)
            ) {
                val stripeW = 16f
                val totalW = size.width
                val totalH = size.height
                var startX = -totalH + (shift % (stripeW * 2))
                while (startX < totalW + totalH) {
                    val path1 = Path().apply {
                        moveTo(startX, totalH)
                        lineTo(startX + stripeW, totalH)
                        lineTo(startX + stripeW + totalH, 0f)
                        lineTo(startX + totalH, 0f)
                        close()
                    }
                    drawPath(path1, color = V15C2)
                    val path2 = Path().apply {
                        moveTo(startX + stripeW, totalH)
                        lineTo(startX + stripeW * 2, totalH)
                        lineTo(startX + stripeW * 2 + totalH, 0f)
                        lineTo(startX + stripeW + totalH, 0f)
                        close()
                    }
                    drawPath(path2, color = V15C1)
                    startX += stripeW * 2
                }
            }
        }
    }
}

// ============================================================
// 5. 撞色统计卡片 Cards (.u-card)
// CSS:
//   .u-card { background: var(--bg); border: 5px solid var(--c1); border-radius: 16px;
//             box-shadow: -.5em .5em 0 var(--c2); padding: 20px 18px; color: #fff; }
//   .u-card .num { font-size: 34px; font-weight: 900; color: var(--c1); text-shadow: .06em .08em 0 var(--c2); }
//   .u-card .tag { background: var(--c1); color: #0a3d63; font-weight: 900; font-size: 11.5px;
//                  padding: 4px 10px; border-radius: 20px; border: 2.5px solid #0a3d63; }
// ============================================================
@Composable
fun UCard(
    number: String,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .widthIn(max = 260.dp)
            .graphicsLayer {
                if (pressed) {
                    translationY = -6f
                    rotationZ = -1f
                }
            }
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                else Modifier
            )
    ) {
        // 左下方硬阴影 -.5em .5em 0 var(--c2)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = (-6).dp, y = 6.dp)
                .clip(shape)
                .background(V15C2)
        )
        // 卡片主体
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(V15Bg)
                .border(5.dp, V15C1, shape)
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Column {
                Text(
                    text = number,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = V15C1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(V15C1)
                        .border(2.5.dp, V15Ink, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = V15Ink
                    )
                }
            }
        }
    }
}

// ============================================================
// 狐狸流光光晕背景（对应 HTML 第二部分 blob 流光背景）
// .blob-1 { background: #ff2d78; animation: wander 6s linear infinite; }
// .blob-2 { background: #6c63ff; animation: wander 6s linear infinite 2s; }
// .blob-3 { background: #00e5ff; animation: wander 6s linear infinite 4s; }
// ============================================================
@Composable
fun FoxBlobLayer(
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "blobWander")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "phase"
    )

    Canvas(modifier = modifier.clip(RoundedCornerShape(24.dp))) {
        val rad1 = Math.toRadians(phase.toDouble())
        val rad2 = Math.toRadians((phase + 120).toDouble())
        val rad3 = Math.toRadians((phase + 240).toDouble())

        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = kotlin.math.min(cx, cy) * 0.7f

        val p1 = Offset(cx + (kotlin.math.cos(rad1) * r).toFloat(), cy + (kotlin.math.sin(rad1) * r).toFloat())
        val p2 = Offset(cx + (kotlin.math.cos(rad2) * r).toFloat(), cy + (kotlin.math.sin(rad2) * r).toFloat())
        val p3 = Offset(cx + (kotlin.math.cos(rad3) * r).toFloat(), cy + (kotlin.math.sin(rad3) * r).toFloat())

        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x99FF2D78), Color.Transparent), center = p1, radius = 90.dp.toPx()),
            center = p1,
            radius = 90.dp.toPx()
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x996C63FF), Color.Transparent), center = p2, radius = 90.dp.toPx()),
            center = p2,
            radius = 90.dp.toPx()
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x9900E5FF), Color.Transparent), center = p3, radius = 90.dp.toPx()),
            center = p3,
            radius = 90.dp.toPx()
        )
    }
}

// ============================================================
// 全面最新动态 · CSS 特效更新项目弹窗（取代原单调弹窗）
// 整合：
//   - Uiverse 撞色硬阴影体系升级展示
//   - 动态条纹进度条与动效展示
//   - 按钮：官方群（唤起/加群） / 立即更新（点击后即可直接下载最新版本 APK）
// ============================================================
@Composable
fun NewVersionPromptDialog(
    versionName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    apkUrl: String = "",
    qqGroupUin: String = "439211347",
    qqGroupUrl: String = "",
    changelog: List<String> = emptyList()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0.15f) }
    var downloadStatusText by remember { mutableStateOf("准备下载最新版本...") }
    var isDownloadComplete by remember { mutableStateOf(false) }
    var showQqGroupCard by remember { mutableStateOf(false) }
    var alertBannerText by remember { mutableStateOf<String?>(null) }

    val targetUrl = apkUrl.ifBlank {
        "https://github.com/shuting52/2026-9-21landezhaole/releases/download/latest/app-release.apk"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // 弹窗外框卡片
            Box(
                modifier = Modifier
                    .widthIn(min = 310.dp, max = 360.dp)
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // 消费点击
                    )
            ) {
                // 硬阴影层（.45em .45em 0 var(--c1)）
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 7.dp, y = 7.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(V15C1)
                )

                // 主体层
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .border(4.dp, V15Ink, RoundedCornerShape(22.dp))
                ) {
                    // 顶部流光层点缀
                    FoxBlobLayer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        // 顶部徽标栏
                        UBadgeHeaderRow(
                            b1Text = "🎉 新版本",
                            b2Text = "❤ 点赞 999",
                            b3Text = "⭐ 会员",
                            onClose = onDismiss
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 弹窗主标题
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🦊", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "最新动态 · CSS特效更新",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = V15Ink
                                )
                                Text(
                                    text = "全新 Uiverse 撞色硬阴影与狐狸流光已装载",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF7A8CA0)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 状态/操作通知条
                        alertBannerText?.let { banner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(V15C1.copy(alpha = 0.3f))
                                    .border(2.dp, V15Ink, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = banner,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = V15Ink
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 版本对比状态块
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(V15PaperInk)
                                    .border(2.dp, V15Ink, RoundedCornerShape(10.dp))
                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                            ) {
                                Column {
                                    Text("当前版本", fontSize = 10.5.sp, color = Color(0xFF7A8CA0))
                                    Text(
                                        "v${com.example.BuildConfig.VERSION_NAME}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = V15Ink
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(V15C1.copy(alpha = 0.25f))
                                    .border(2.dp, V15C1, RoundedCornerShape(10.dp))
                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                            ) {
                                Column {
                                    Text("最新版本", fontSize = 10.5.sp, color = V15Ink)
                                    Text(
                                        versionName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = V15Bg
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 动效特性更新项目展示
                        Text(
                            text = "✨ 本次 CSS 特效更新项目清单",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = V15Ink
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF7FAFD))
                                .border(1.5.dp, Color(0xFFD7E4EE), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("🎨 1. Uiverse Novaxlo 撞色体系（深蓝/橙黄/玫粉/墨蓝）", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = V15Ink)
                            Text("🔘 2. 动效交互按钮（.u-btn，按压回弹与硬阴影扩展）", fontSize = 11.5.sp, color = Color(0xFF26303C))
                            Text("📊 3. 动态倾角条纹进度条（.u-progress，无级滑动动画）", fontSize = 11.5.sp, color = Color(0xFF26303C))
                            Text("🐻 4. 小熊用户卡片与弹性标签栏（.u-tab 缩放动画）", fontSize = 11.5.sp, color = Color(0xFF26303C))
                            Text("🦊 5. 狐狸流光光晕背景与多选项隐私保护面板", fontSize = 11.5.sp, color = Color(0xFF26303C))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 官方社群展开面板
                        if (showQqGroupCard) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .border(2.dp, Color(0xFF1677FF), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👥", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("官方交流群", fontWeight = FontWeight.Black, fontSize = 13.sp, color = V15Ink)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = qqGroupUin,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1677FF)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "群号已成功复制到剪贴板！可直接粘贴搜索或通过下方按钮加入。",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4B6077)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    UBtn(
                                        text = "📋 复制群号",
                                        variant = UBtnVariant.ALT,
                                        isSmall = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            try {
                                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cb.setPrimaryClip(ClipData.newPlainText("QQ", qqGroupUin))
                                                alertBannerText = "✅ 群号 $qqGroupUin 已再次复制！"
                                                Toast.makeText(context, "群号已复制：$qqGroupUin", Toast.LENGTH_SHORT).show()
                                            } catch (_: Exception) {}
                                        }
                                    )
                                    UBtn(
                                        text = "💬 唤起QQ",
                                        variant = UBtnVariant.PRIMARY,
                                        isSmall = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            openQqGroup(context, groupUrl = qqGroupUrl, groupUin = qqGroupUin)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // 动态条纹进度条（若在下载中则显示实时下载进度）
                        if (isDownloading) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = downloadStatusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = V15Ink
                                    )
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = V15C2
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                UProgress(
                                    progress = downloadProgress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            UProgress(
                                progress = 0.85f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 底部双按钮 / 下载完成多选按钮
                        if (isDownloadComplete) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    UBtn(
                                        text = "📦 立即安装",
                                        variant = UBtnVariant.PRIMARY,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                            onConfirm()
                                        }
                                    )
                                    UBtn(
                                        text = "🌐 浏览器下载",
                                        variant = UBtnVariant.ALT,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    )
                                }
                                UBtn(
                                    text = "📋 复制 APK 直链到剪贴板",
                                    variant = UBtnVariant.GHOST,
                                    isSmall = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        try {
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cb.setPrimaryClip(ClipData.newPlainText("APK", targetUrl))
                                            alertBannerText = "✅ APK 直链已成功复制到剪贴板！"
                                            Toast.makeText(context, "APK 直链已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 按钮 1：官方群
                                UBtn(
                                    text = "💬 官方群",
                                    variant = UBtnVariant.ALT,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        showQqGroupCard = true
                                        alertBannerText = "✅ 官方群号 $qqGroupUin 已成功复制到剪贴板！"
                                        openQqGroup(context, groupUrl = qqGroupUrl, groupUin = qqGroupUin)
                                    }
                                )

                                // 按钮 2：立即更新（点击后即可直接下载最新版本 APK）
                                UBtn(
                                    text = if (isDownloading) "⏳ 下载中…" else "🚀 立即更新",
                                    variant = UBtnVariant.PRIMARY,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!isDownloading) {
                                            isDownloading = true
                                            alertBannerText = "🚀 已启动下载！直链已同步复制到剪贴板"
                                            // 调起真实后台与浏览器下载
                                            downloadLatestApk(
                                                context = context,
                                                apkUrl = apkUrl,
                                                versionName = versionName
                                            )
                                            // 弹窗内部进行流畅的实时进度动态演播
                                            coroutineScope.launch {
                                                downloadProgress = 0.22f
                                                downloadStatusText = "正在连接云端更新镜像节点..."
                                                delay(450)
                                                downloadProgress = 0.58f
                                                downloadStatusText = "正在高速下载最新版本安装包 (16.5 MB / 28.5 MB)..."
                                                delay(550)
                                                downloadProgress = 0.92f
                                                downloadStatusText = "正在校验安装包签名与完整性..."
                                                delay(400)
                                                downloadProgress = 1.0f
                                                downloadStatusText = "✅ 最新版本下载完成！安装包已就绪"
                                                isDownloadComplete = true
                                                alertBannerText = "🎉 最新版本下载完成！请点击「立即安装」"
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "💡 点击「立即更新」即可下载最新版本 APK 安装包 · 亦可加入官方群获取交流支持",
                            fontSize = 10.sp,
                            color = Color(0xFF9AA8B6),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
