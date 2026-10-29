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
)

data class SiteCategory(
    val id: String,
    val name: String,
    val desc: String,
    val subs: List<SiteSub>,
    val sites: List<Site>,
)

/**
 * Loads the bundled website data (assets/landezhaole-data.json).
 * Same format as the CF Pages project landezhaole:
 * cats[].{id,name,desc,subs[].{id,name},cards[].{t,u,d,b,s}}.
 * Uses org.json, which ships with Android. Returns empty on parse failure.
 */
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
