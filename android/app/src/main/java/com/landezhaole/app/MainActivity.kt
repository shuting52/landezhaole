package com.landezhaole.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.LaunchedEffect
import com.landezhaole.app.ui.DoodleUpdateDialog
import kotlinx.coroutines.delay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder

data class SubTab(val id: String, val name: String)
data class SiteCard(val t: String, val u: String, val d: String, val b: String, val s: String, val f: String)
data class Category(val id: String, val name: String, val desc: String, val subs: List<SubTab>, val cards: List<SiteCard>)
data class Engine(val id: String, val name: String, val url: String?)

val ENGINES = listOf(
    Engine("site", "站内", null),
    Engine("bing", "必应", "https://cn.bing.com/search?q=%s"),
    Engine("baidu", "百度", "https://www.baidu.com/s?wd=%s"),
    Engine("bilibili", "B站", "https://search.bilibili.com/all?keyword=%s"),
    Engine("baidupan", "百度网盘", "https://wangpan.xin/search?keyword=%s"),
    Engine("alipan", "阿里云盘", "https://v2pan.com/search?q=%s"),
    Engine("quarkpan", "夸克网盘", "https://www.kkso.cc/search?kw=%s"),
    Engine("google", "谷歌", "https://www.google.com/search?q=%s")
)

val AVATAR_COLORS = listOf(
    Color(0xFF16A34A), Color(0xFF0EA5E9), Color(0xFF8B5CF6), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFFEC4899), Color(0xFF14B8E6), Color(0xFFF97316)
)
fun avatarColor(t: String): Color {
    var h = 0
    for (c in t) h = (h * 31 + c.code)
    val idx = ((h % AVATAR_COLORS.size) + AVATAR_COLORS.size) % AVATAR_COLORS.size
    return AVATAR_COLORS[idx]
}
val RAINBOW = Brush.horizontalGradient(
    listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFEAB308), Color(0xFF22C55E), Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFFA855F7))
)
fun loadCategories(ctx: Context): List<Category> {
    return try {
        val text = ctx.assets.open("data.json").bufferedReader().readText()
        val root = JSONObject(text)
        val cats = root.getJSONArray("cats")
        val out = mutableListOf<Category>()
        for (i in 0 until cats.length()) {
            val c = cats.getJSONObject(i)
            val subs = mutableListOf<SubTab>()
            val subsArr = c.optJSONArray("subs")
            if (subsArr != null) for (j in 0 until subsArr.length()) {
                val s = subsArr.getJSONObject(j)
                subs.add(SubTab(s.optString("id"), s.optString("name")))
            }
            val cards = mutableListOf<SiteCard>()
            val cardsArr = c.optJSONArray("cards")
            if (cardsArr != null) for (j in 0 until cardsArr.length()) {
                val s = cardsArr.getJSONObject(j)
                cards.add(SiteCard(s.optString("t"), s.optString("u"), s.optString("d"), s.optString("b"), s.optString("s"), s.optString("f")))
            }
            out.add(Category(c.optString("id"), c.optString("name"), c.optString("desc"), subs, cards))
        }
        out
    } catch (e: Exception) {
        emptyList()
    }
}
fun openUrl(ctx: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        ctx.startActivity(intent)
    } catch (e: Exception) {
    }
}
fun todayKey(): String {
    val cal = java.util.Calendar.getInstance()
    return "%04d-%02d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dark by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen(dark = dark, onToggleDark = { dark = !dark })
                }
            }
        }
    }
}

@Composable
fun HomeScreen(dark: Boolean, onToggleDark: () -> Unit) {
    val ctx = LocalContext.current
    val categories = remember { loadCategories(ctx) }
    var query by remember { mutableStateOf("") }
    var engineIdx by remember { mutableStateOf(0) }
    var dockCat by remember { mutableStateOf<String?>(null) }
    var showUpdate by remember { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    val clicks = remember { mutableStateMapOf<String, Int>() }
    val prefs = remember { ctx.getSharedPreferences("landezhaole-clicks", Context.MODE_PRIVATE) }
    // load today's clicks once
    remember {
        try {
            val key = todayKey()
            val raw = prefs.getString(key, "") ?: ""
            if (raw.isNotEmpty()) {
                val obj = JSONObject(raw)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    clicks[k] = obj.optInt(k)
                }
            }
        } catch (e: Exception) {
        }
        true
    }
    fun recordClick(url: String) {
        val n = (clicks[url] ?: 0) + 1
        clicks[url] = n
        try {
            val obj = JSONObject()
            for ((k, v) in clicks) obj.put(k, v)
            prefs.edit().putString(todayKey(), obj.toString()).apply()
        } catch (e: Exception) {
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(showToast) { if (showToast) { delay(1800); showToast = false } }
    val scope = rememberCoroutineScope()
    val total = categories.sumOf { it.cards.size }
    val isSearching = query.isNotBlank() && engineIdx == 0
    val searchResults: List<SiteCard> = if (isSearching) {
        val low = query.trim().lowercase()
        val all = mutableListOf<SiteCard>()
        for (cat in categories) for (c in cat.cards) {
            val hay = (c.t + " " + c.d + " " + c.b + " " + c.u + " " + cat.name).lowercase()
            if (hay.contains(low)) all.add(c)
        }
        fun rank(c: SiteCard): Int = when {
            c.b.equals("NEW", ignoreCase = true) || c.b.contains("新") -> 2
            c.b.equals("HOT", ignoreCase = true) -> 1
            else -> 0
        }
        all.sortedByDescending { rank(it) }
    } else emptyList()
    val shownCategories = when {
        dockCat != null -> categories.filter { it.id == dockCat }
        else -> categories
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF16A34A)),
                        contentAlignment = Alignment.Center
                    ) { Text("懒", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("懒得找了", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("找网站，懒得找了 · $total 个精选站点", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showUpdate = true }) {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = "检查更新")
                    }
                    IconButton(onClick = onToggleDark) {
                        Icon(if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode, contentDescription = "夜间模式")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // engines
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ENGINES.forEachIndexed { idx, e ->
                        val selected = idx == engineIdx
                        Button(
                            onClick = { engineIdx = idx },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Color(0xFF16A34A) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(e.name, fontSize = 12.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // search
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("搜本站资源，或切到其他引擎搜全网…", fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "清空") }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val q = query.trim()
                        if (q.isEmpty()) return@Button
                        val engine = ENGINES[engineIdx]
                        if (engine.url != null) {
                            val target = String.format(engine.url, URLEncoder.encode(q, "UTF-8"))
                            openUrl(ctx, target)
                        }
                    }) { Icon(Icons.Filled.Search, contentDescription = "搜索") }
                }
                Text(
                    "站内搜索：标题 / 简介 / 标签匹配，NEW 置顶 · 搜索时只显示结果列表",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // quick dock: first 10 categories
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { dockCat = null }) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Text("全", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("全部", fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    categories.take(10).forEach { cat ->
                        val selected = dockCat == cat.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { dockCat = if (selected) null else cat.id }
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(avatarColor(cat.name)),
                                contentAlignment = Alignment.Center
                            ) { Text(cat.name.trim().firstOrNull()?.toString() ?: "?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            Text(cat.name.split("·")[0].trim().take(6), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (isSearching) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("搜索结果（${searchResults.size}）", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Button(onClick = { query = "" }) { Text("清空搜索", fontSize = 12.sp) }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (searchResults.isEmpty()) {
                        Text("没找到相关资源，换个关键词试试", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(searchResults.chunked(2)) { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (card in row) {
                            Box(modifier = Modifier.weight(1f)) { SiteCardView(card, clicks[card.u] ?: 0, onClick = { recordClick(card.u); openUrl(ctx, card.u) }) }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                items(shownCategories, key = { it.id }) { cat ->
                    CategorySection(cat, onCardClick = { card -> recordClick(card.u); openUrl(ctx, card.u) }, clicks = clicks)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
        if (showUpdate) {
            DoodleUpdateDialog(onClose = { showUpdate = false }, onToast = { toastMsg = it; showToast = true })
        }
        if (showToast) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2B2B2B)).padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text(toastMsg, color = Color(0xFFFFFDF8), fontSize = 14.sp)
            }
        }
        FloatingActionButton(
            onClick = { scope.launch { listState.scrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "回到顶部") }
    }
}

@Composable
fun CategorySection(cat: Category, onCardClick: (SiteCard) -> Unit, clicks: Map<String, Int>) {
    var sub by remember { mutableStateOf("all") }
    val shown = if (sub == "all") cat.cards else cat.cards.filter { it.s == sub }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF2563EB)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(cat.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${cat.cards.size} 个站点", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (cat.desc.isNotBlank()) {
                Text(cat.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cat.subs.forEach { s ->
                    val selected = sub == s.id
                    Button(
                        onClick = { sub = s.id },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFF16A34A) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(s.name, fontSize = 12.sp) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            for (row in shown.chunked(2)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (card in row) {
                        Box(modifier = Modifier.weight(1f)) { SiteCardView(card, clicks[card.u] ?: 0, onClick = { onCardClick(card) }) }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun SiteCardView(card: SiteCard, todayClicks: Int, onClick: () -> Unit) {
    Box {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val initials = (if (card.f.isNotBlank()) card.f else card.t).take(2)
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)).background(avatarColor(card.t)),
                    contentAlignment = Alignment.Center
                ) { Text(initials, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.t, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(card.d, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (todayClicks > 0) {
                        Text("今日点击 $todayClicks 次", fontSize = 10.sp, color = Color(0xFFEF4444))
                    }
                }
            }
        }
        // corner badge, straddling top border like website
        val label = if (card.b.isNotBlank()) card.b else "精选"
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp).background(RAINBOW, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
        ) { Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
    }
}
