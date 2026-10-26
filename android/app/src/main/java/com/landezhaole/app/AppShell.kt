package com.landezhaole.app

import android.content.Intent
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Switch
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.PathEffect
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import com.landezhaole.app.ui.RemoteVersion
import com.landezhaole.app.ui.ThemeAppearanceScreen
import com.landezhaole.app.ui.ThemeMode
import com.landezhaole.app.ui.UpdateDialog
import com.landezhaole.app.ui.currentVersionCode
import com.landezhaole.app.ui.fetchRemoteVersion
import org.json.JSONObject

// 美女栏视频源：待接入。把 mp4 直链填进来即可播放（连续开关控制循环，切换视频切下一个）。
val VIDEO_URLS: List<String> = emptyList()

@Composable
fun AppShell(
    pal: AppPalette,
    dark: Boolean,
    onToggleDark: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize().background(pal.pageBackground)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
            when (tab) {
                0 -> HomeScreen(pal = pal, uiScale = uiScale)
                1 -> SaveScreen(pal = pal)
                2 -> VideoScreen(pal = pal, onBackHome = { tab = 0 })
                3 -> MineScreen(
                    pal = pal, dark = dark, onToggleDark = onToggleDark,
                    themeMode = themeMode, onThemeModeChange = onThemeModeChange,
                    uiScale = uiScale, onUiScaleChange = onUiScaleChange
                )
            }
        }
        NavigationBar(
            containerColor = pal.barBackground,
            contentColor = pal.foreground
        ) {
            NavigationBarItem(
                selected = tab == 0,
                onClick = { tab = 0 },
                icon = { Icon(Icons.Filled.Home, contentDescription = "首页") },
                label = { Text("首页") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = pal.accent,
                    selectedTextColor = pal.accent,
                    indicatorColor = pal.accentSoft
                )
            )
            NavigationBarItem(
                selected = tab == 1,
                onClick = { tab = 1 },
                icon = { Icon(Icons.Filled.Savings, contentDescription = "省钱") },
                label = { Text("省钱") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = pal.accent,
                    selectedTextColor = pal.accent,
                    indicatorColor = pal.accentSoft
                )
            )
            NavigationBarItem(
                selected = tab == 2,
                onClick = { tab = 2 },
                icon = { Icon(Icons.Filled.Movie, contentDescription = "美女") },
                label = { Text("美女") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = pal.accent,
                    selectedTextColor = pal.accent,
                    indicatorColor = pal.accentSoft
                )
            )
            NavigationBarItem(
                selected = tab == 3,
                onClick = { tab = 3 },
                icon = { Icon(Icons.Filled.Person, contentDescription = "我的") },
                label = { Text("我的") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = pal.accent,
                    selectedTextColor = pal.accent,
                    indicatorColor = pal.accentSoft
                )
            )
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
    Column(modifier = Modifier.fillMaxSize().background(pal.pageBackground)) {
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
                    modifier = Modifier.clickable { tab = i; filter = "all" },
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
                    // 网站 .tab-line：选中 tab 下方一条同宽白带，把 tab 和筛选卡连成一个文件夹形
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
    Column(modifier = Modifier.fillMaxSize().background(pal.pageBackground)) {
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
fun MineScreen(
    pal: AppPalette,
    dark: Boolean,
    onToggleDark: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTheme by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<RemoteVersion?>(null) }
    var qrTitle by remember { mutableStateOf<String?>(null) }
    var qrAsset by remember { mutableStateOf("") }
    var qrHint by remember { mutableStateOf("") }
    var showAbout by remember { mutableStateOf(false) }
    if (showTheme) {
        ThemeAppearanceScreen(pal = pal, mode = themeMode, onModeChange = onThemeModeChange, uiScale = uiScale, onUiScaleChange = onUiScaleChange, onBack = { showTheme = false })
        return
    }
    fun checkUpdate() {
        scope.launch {
            val remote = fetchRemoteVersion()
            when {
                remote == null -> Toast.makeText(ctx, "检查失败，请稍后再试", Toast.LENGTH_SHORT).show()
                remote.versionCode > currentVersionCode(ctx) -> pendingUpdate = remote
                else -> Toast.makeText(ctx, "已是最新版本", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val verNameText = try {
        val v = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        if (v.isNullOrEmpty()) null else "当前 $v"
    } catch (e: Exception) {
        null
    }
    val modeName = when (themeMode) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
    }
    Column(modifier = Modifier.fillMaxSize().background(pal.pageBackground)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp)
                .background(Color(0xFF5B67A6)),
            contentAlignment = Alignment.Center
        ) {
            Text("我的", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.barBackground)
                    .border(1.dp, pal.line, RoundedCornerShape(12.dp))
            ) {
                Column {
                    MineRow(
                        icon = Icons.Filled.Person,
                        iconTint = Color(0xFF7C6CF0),
                        label = "关注我们",
                        pal = pal,
                        onClick = { openUrl(ctx, "https://landezhaole.pages.dev/") }
                    )
                    Divider(color = pal.line)
                    MineRow(
                        icon = Icons.Filled.Share,
                        iconTint = Color(0xFFF5A623),
                        label = "分享给朋友",
                        pal = pal,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "懒得找了 · 免费资源导航 https://landezhaole.pages.dev/")
                                }
                                ctx.startActivity(Intent.createChooser(intent, "分享给朋友"))
                            } catch (e: Exception) {
                            }
                        }
                    )
                    Divider(color = pal.line)
                    MineRow(
                        icon = Icons.Filled.Group,
                        iconTint = Color(0xFF5B67F0),
                        label = "添加粉丝群",
                        pal = pal,
                        onClick = { openUrl(ctx, "https://qm.qq.com/q/vsaFRiPuO4") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("支持与关于", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pal.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.barBackground)
                    .border(1.dp, pal.line, RoundedCornerShape(12.dp))
            ) {
                Column {
                    MineRow(
                        icon = Icons.Filled.Chat,
                        iconTint = Color(0xFF07C160),
                        label = "微信打赏",
                        pal = pal,
                        onClick = { qrTitle = "微信打赏"; qrAsset = "qr-wechat.jpg"; qrHint = "微信扫一扫，支持一下" }
                    )
                    Divider(color = pal.line)
                    MineRow(
                        icon = Icons.Filled.AccountCircle,
                        iconTint = Color(0xFF12B7F5),
                        label = "QQ好友",
                        pal = pal,
                        onClick = { qrTitle = "QQ好友"; qrAsset = "qr-qq.jpg"; qrHint = "QQ扫一扫，加我好友" }
                    )
                    Divider(color = pal.line)
                    MineRow(
                        icon = Icons.Filled.Payment,
                        iconTint = Color(0xFF1677FF),
                        label = "支付宝打赏",
                        pal = pal,
                        onClick = { qrTitle = "支付宝打赏"; qrAsset = "qr-alipay.jpg"; qrHint = "支付宝扫一扫，支持一下" }
                    )
                    Divider(color = pal.line)
                    MineRow(
                        icon = Icons.Filled.Info,
                        iconTint = pal.secondary,
                        label = "关于网站",
                        pal = pal,
                        onClick = { showAbout = true }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("设置", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pal.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.barBackground)
                    .border(1.dp, pal.line, RoundedCornerShape(12.dp))
            ) {
                Column {
                    MineRow(
                        icon = Icons.Filled.SystemUpdate,
                        iconTint = pal.accent,
                        label = "检查更新",
                        trailing = verNameText,
                        pal = pal,
                        onClick = { checkUpdate() }
                    )
                    Divider(color = pal.line)
                    MineSwitchRow(
                        icon = if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        iconTint = Color(0xFF5B67A6),
                        label = "深色模式",
                        checked = dark,
                        pal = pal,
                        onCheckedChange = { onToggleDark() }
                    )
                    Divider(color = pal.line)
                    MineRow(
                        icon = Icons.Filled.Palette,
                        iconTint = Color(0xFFEC4899),
                        label = "主题",
                        trailing = modeName,
                        pal = pal,
                        onClick = { showTheme = true }
                    )
                }
            }
        }
    }
    if (pendingUpdate != null) {
        UpdateDialog(version = pendingUpdate!!, pal = pal, onDismiss = { pendingUpdate = null })
    }
    if (qrTitle != null) {
        QrDialog(title = qrTitle!!, asset = qrAsset, hint = qrHint, onDismiss = { qrTitle = null })
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

fun loadAssetImageBitmap(ctx: Context, name: String): ImageBitmap? {
    return try {
        ctx.assets.open(name).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun QrDialog(title: String, asset: String, hint: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val bitmap = remember(asset) { loadAssetImageBitmap(ctx, asset) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = title, modifier = Modifier.size(240.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(hint, fontSize = 14.sp, color = Color.Gray)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val counts = remember {
        try {
            val raw = ctx.assets.open("data.json").bufferedReader().use { it.readText() }
            val obj = JSONObject(raw)
            val cats = obj.getJSONArray("cats")
            var total = 0
            for (i in 0 until cats.length()) total += cats.getJSONObject(i).getJSONArray("cards").length()
            Pair(cats.length(), total)
        } catch (e: Exception) {
            Pair(21, 2436)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于网站") },
        text = {
            Text("懒得找了 · 免费资源导航\n已收录 ${counts.first} 大分类 · ${counts.second} 个精选站点，一搜即达。\n本站为免费资源导航，所有外链归原站所有 · 链接失效欢迎反馈。")
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

@Composable
fun MineSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    checked: Boolean,
    pal: AppPalette,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, fontSize = 16.sp, color = pal.foreground, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onCheckedChange() })
    }
}
