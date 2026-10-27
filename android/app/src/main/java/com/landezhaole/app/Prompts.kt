package com.landezhaole.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ChatGPT 玩法题库：与网站 chatgpt-prompts.json 同源，运行时从云端拉取，不写进包里。
data class PromptItem(val key: String, val desc: String)
data class PromptCategory(val id: String, val name: String, val prompts: List<PromptItem>)
data class PromptsData(val categories: List<PromptCategory>) {
    val total: Int get() = categories.sumOf { it.prompts.size }
}

// 双源：仓库 jsdelivr 镜像为主，线上网站为备；两个都是同一份文件。
suspend fun fetchPrompts(): PromptsData? = withContext(Dispatchers.IO) {
    val urls = listOf(
        "https://cdn.jsdelivr.net/gh/shuting52/landezhaole@main/chatgpt-prompts.json",
        "https://landezhaole.pages.dev/chatgpt-prompts.json"
    )
    for (u in urls) {
        try {
            val conn = URL(u).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val text = conn.inputStream.readBytes().toString(Charsets.UTF_8)
            val arr = JSONObject(text).getJSONArray("categories")
            val cats = mutableListOf<PromptCategory>()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                val ps = c.getJSONArray("prompts")
                val items = mutableListOf<PromptItem>()
                for (j in 0 until ps.length()) {
                    val p = ps.getJSONObject(j)
                    items.add(PromptItem(p.optString("key"), p.optString("desc")))
                }
                cats.add(PromptCategory(c.optString("id"), c.optString("name"), items))
            }
            if (cats.isNotEmpty()) return@withContext PromptsData(cats)
        } catch (e: Exception) {
        }
    }
    null
}
