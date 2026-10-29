package com.landezhaole.glasstest

import android.content.Context
import org.json.JSONObject

data class SiteSub(val id: String, val name: String)

data class Site(
    val title: String,
    val url: String,
    val desc: String,
    val badge: String,
    val sub: String,
    val iconUrl: String,
)

data class SiteCategory(
    val id: String,
    val name: String,
    val desc: String,
    val subs: List<SiteSub>,
    val sites: List<Site>,
)

data class Prompt(val key: String, val desc: String)
data class PromptCategory(val id: String, val name: String, val prompts: List<Prompt>)
data class Dish(
    val name: String, val cat: String, val cuisine: String, val minutes: Int,
    val ingredients: List<String>, val steps: List<String>,
)
data class SqTab(val id: String, val name: String, val filters: List<Pair<String, String>>)
data class SqCard(
    val tab: Int, val href: String, val title: String, val desc: String,
    val tags: List<String>, val tagClasses: List<String>, val logoAlt: String,
)
data class SqData(val tabs: List<SqTab>, val cards: List<SqCard>)

fun loadSiteData(context: Context): List<SiteCategory> {
    return try {
        val text = context.assets.open("landezhaole-data.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val cats = root.getJSONArray("cats")
        buildList {
            for (i in 0 until cats.length()) {
                val cat = cats.getJSONObject(i)
                val subs = buildList {
                    val arr = cat.optJSONArray("subs") ?: return@buildList
                    for (j in 0 until arr.length()) {
                        val s = arr.getJSONObject(j)
                        add(SiteSub(s.optString("id"), s.optString("name")))
                    }
                }
                val sites = buildList {
                    val arr = cat.optJSONArray("cards") ?: return@buildList
                    for (j in 0 until arr.length()) {
                        val c = arr.getJSONObject(j)
                        add(
                            Site(
                                title = c.optString("t"),
                                url = c.optString("u"),
                                desc = c.optString("d"),
                                badge = c.optString("b"),
                                sub = c.optString("s"),
                                iconUrl = c.optString("i"),
                            )
                        )
                    }
                }
                add(
                    SiteCategory(
                        id = cat.optString("id"),
                        name = cat.optString("name"),
                        desc = cat.optString("desc"),
                        subs = subs,
                        sites = sites,
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun loadPrompts(context: Context): List<PromptCategory> {
    return try {
        val text = context.assets.open("chatgpt-prompts.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val cats = JSONObject(text).getJSONArray("categories")
        buildList {
            for (i in 0 until cats.length()) {
                val cat = cats.getJSONObject(i)
                val prompts = buildList {
                    val arr = cat.optJSONArray("prompts") ?: return@buildList
                    for (j in 0 until arr.length()) {
                        val p = arr.getJSONObject(j)
                        add(Prompt(p.optString("key"), p.optString("desc")))
                    }
                }
                add(PromptCategory(cat.optString("id"), cat.optString("name"), prompts))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun loadDishes(context: Context): List<Dish> {
    return try {
        val text = context.assets.open("recipes.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = org.json.JSONArray(text)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                fun strArr(key: String): List<String> {
                    val a = o.optJSONArray(key) ?: return emptyList()
                    return List(a.length()) { a.optString(it) }
                }
                add(
                    Dish(
                        o.optString("n"), o.optString("c"), o.optString("cuisine"),
                        o.optInt("t"), strArr("ing"), strArr("st"),
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun loadSq(context: Context): SqData {
    return try {
        val text = context.assets.open("sq.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val tabs = buildList {
            val arr = root.optJSONArray("tabs") ?: return@buildList
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                val filters = buildList {
                    val fa = t.optJSONArray("filters") ?: return@buildList
                    for (j in 0 until fa.length()) {
                        val f = fa.getJSONArray(j)
                        add(f.optString(0) to f.optString(1))
                    }
                }
                add(SqTab(t.optString("id"), t.optString("name"), filters))
            }
        }
        val cards = buildList {
            val arr = root.optJSONArray("cards") ?: return@buildList
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                fun strArr(key: String): List<String> {
                    val a = c.optJSONArray(key) ?: return emptyList()
                    return List(a.length()) { a.optString(it) }
                }
                add(
                    SqCard(
                        c.optInt("tab"), c.optString("href"), c.optString("title"),
                        c.optString("desc"), strArr("tags"), strArr("tagClasses"),
                        c.optString("logoAlt"),
                    )
                )
            }
        }
        SqData(tabs, cards)
    } catch (_: Exception) {
        SqData(emptyList(), emptyList())
    }
}

/** 网站同款头像色：名字哈希取 8 色之一。 */
fun avColorHex(name: String): Long {
    val colors = listOf(0xFF16A34AL, 0xFF0EA5E9L, 0xFF8B5CF6L, 0xFFF59E0BL, 0xFFEF4444L, 0xFFEC4899L, 0xFF14B8E6L, 0xFFF97316L)
    var h = 0
    for (ch in name) h = (h * 31 + ch.code) 
    val idx = ((h % colors.size) + colors.size) % colors.size
    return colors[idx]
}

fun dishDifficulty(d: Dish): String =
    if (d.minutes <= 15 && d.steps.size <= 4) "简单" else if (d.minutes <= 30) "中等" else "较难"

fun dishSpiciness(d: Dish): String {
    val joined = d.ingredients.joinToString()
    return when {
        listOf("辣椒", "花椒", "郫县", "剁椒", "小米辣", "辣椒油", "尖椒").any { it in joined } -> "辣"
        "胡椒" in joined -> "微辣"
        else -> "不辣"
    }
}

fun dishServings(d: Dish): String =
    if (d.cat.contains("汤") || d.name.contains("汤") || d.name.contains("羹")) "3-4人" else "2-3人"
