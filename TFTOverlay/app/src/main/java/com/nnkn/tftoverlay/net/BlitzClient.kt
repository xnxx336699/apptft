package com.nnkn.tftoverlay.net

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nnkn.tftoverlay.data.Comp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

class BlitzClient(private val fullUrl: String) {

    private val client: OkHttpClient by lazy {
        val log = HttpLoggingInterceptor()
        log.level = HttpLoggingInterceptor.Level.BASIC
        OkHttpClient.Builder().addInterceptor(log).build()
    }

    fun fetchComps(): List<Comp> {
        return try {
            val req = Request.Builder().url(fullUrl).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: "[]"
                normalize(JsonParser.parseString(body))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // fallback
            listOf(
                Comp("Duelist Reroll", "Yasuo/Kai'Sa tốc đánh.", listOf("Yasuo","Kai'Sa","Irelia","Fiora","Jax","Lee Sin")),
                Comp("Warden + Invoker", "Ahri phép + dàn chắn.", listOf("Ahri","Syndra","Taric","Shen","Kennen","Rakan"))
            )
        }
    }

    private fun normalize(el: JsonElement): List<Comp> {
        val out = mutableListOf<Comp>()
        when {
            el.isJsonArray -> {
                for (e in el.asJsonArray) {
                    val o = e.asJsonObject
                    val name = o.get("name")?.asString ?: o.get("title")?.asString ?: "Comp"
                    val desc = o.get("description")?.asString ?: o.get("shortDesc")?.asString ?: ""
                    val units =
                        if (o.has("units")) o.getAsJsonArray("units").map { it.asString }
                        else if (o.has("champions")) o.getAsJsonArray("champions").map { it.asString }
                        else emptyList()
                    out.add(Comp(name, desc, units))
                }
            }
            el.isJsonObject -> {
                val root = el.asJsonObject
                if (root.has("comps")) {
                    for (c in root.getAsJsonArray("comps")) {
                        val o = c.asJsonObject
                        val name = o.get("name")?.asString ?: "Comp"
                        val desc = o.get("description")?.asString ?: ""
                        val units = if (o.has("champions")) o.getAsJsonArray("champions").map { it.asString } else emptyList()
                        out.add(Comp(name, desc, units))
                    }
                } else if (root.has("data")) {
                    out.addAll(normalize(root.get("data")))
                }
            }
        }
        return out
    }
}
