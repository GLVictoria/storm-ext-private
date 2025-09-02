package com.stormunblessed

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import java.util.*
import android.content.Context
import com.marplex.cloudflarebypass.CloudflareBypass
import org.jsoup.Jsoup

class AnimeOnlineNinjaProvider(val context: Context) : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Película")) TvType.AnimeMovie
            else TvType.Anime
        }

        fun getDubStatus(title: String): DubStatus {
            return if (title.contains("Latino") || title.contains("Castellano"))
                DubStatus.Dubbed
            else DubStatus.Subbed
        }
    }

    override var mainUrl = "https://ww3.animeonline.ninja"
    override var name = "AnimeOnlineNinja"
    override var lang = "es"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(
        TvType.AnimeMovie,
        TvType.OVA,
        TvType.Anime,
    )

    private val bypasser = CloudflareBypass(context)

    override suspend fun getMainPage(page: Int, request : MainPageRequest): HomePageResponse {
        val urls = listOf(
            Pair("${mainUrl}/peliculas/", "Películas"),
            Pair("${mainUrl}/directorio/", "Animes"),
            Pair("${mainUrl}/emision/", "En emision"),
        )
        val items = ArrayList<HomePageList>()
        val isHorizontal = true
        val doc = bypasser.get(mainUrl).body?.string()?.let { Jsoup.parse(it) }
        items.add(
            HomePageList(
                "Últimos episodios",
                doc?.select("ul.List-episodes li")?.mapNotNull {
                    val title = it.selectFirst("h3.Title")?.text() ?: return@mapNotNull null
                    val poster = it.selectFirst("div.Image figure img")?.attr("src") ?: return@mapNotNull null
                    val url = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                    val epNum = it.selectFirst("span.Episode")?.text()?.replace("Episodio ", "")?.toIntOrNull()
                    newAnimeSearchResponse(title, url) {
                        this.posterUrl = fixUrl(poster)
                        addDubStatus(getDubStatus(title), epNum)
                    }
                } ?: emptyList(), isHorizontal)
        )

        urls.apmap {
            val doc = bypasser.get(url).body?.string()?.let { Jsoup.parse(it) }
            val home = doc?.select("ul.List-Animes li article")?.mapNotNull {
                val title = it.selectFirst("h3.Title")?.text() ?: return@mapNotNull null
                val poster = it.selectFirst("figure img")?.attr("src") ?: return@mapNotNull null
                newAnimeSearchResponse(
                    title,
                    fixUrl(it.selectFirst("a")?.attr("href") ?: return@mapNotNull null)
                ) {
                    this.posterUrl = fixUrl(poster)
                    addDubStatus(getDubStatus(title))
                }
            } ?: emptyList()

            items.add(HomePageList(name, home))
        }
        if (items.size <= 0) throw ErrorLoadingException()
        return HomePageResponse(items)
    }

    data class SearchObject(
        @JsonProperty("id") val id: String,
        @JsonProperty("title") val title: String,
        @JsonProperty("type") val type: String,
        @JsonProperty("last_id") val lastId: String,
        @JsonProperty("slug") val slug: String
    )

    override suspend fun quickSearch(query: String): List<SearchResponse> {
        return search(query)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = bypasser.get("${mainUrl}/directorio/?s=$query").body?.string()?.let { Jsoup.parse(it) }
        val sss = doc?.select("ul.List-Animes li article")?.map { ll ->
            val title = ll.selectFirst("h3")?.text() ?: ""
            val image = ll.selectFirst("figure img")?.attr("src") ?: ""
            val href = ll.selectFirst("a")?.attr("href") ?: ""
            newAnimeSearchResponse(title, href){
                this.posterUrl = image
                addDubStatus(getDubStatus(title))
            }
        } ?: emptyList()
        return sss
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = bypasser.get(url).body?.string()?.let { Jsoup.parse(it) }
        val episodes = ArrayList<Episode>()
        val title = doc?.selectFirst("h1.Title")!!.text()
        val poster = doc.selectFirst("div.AnimeCover div.Image figure img")?.attr("src")!!
        val description = doc.selectFirst("div.Description p")?.text()
        val type = doc.selectFirst("span.Type")?.text() ?: ""
        val status = when (doc.selectFirst("p.AnmStts span")?.text()) {
            "En emision" -> ShowStatus.Ongoing
            "Finalizado" -> ShowStatus.Completed
            else -> null
        }
        val genre = doc.select("nav.Nvgnrs a")
            .map { it?.text()?.trim().toString() }

        doc.select("script").map { script ->
            if (script.data().contains("var episodes = [")) {
                val data = script.data().substringAfter("var episodes = [").substringBefore("];")
                data.split("],").forEach {

                    val epNum = it.removePrefix("[").substringBefore(",")
                    // val epthumbid = it.removePrefix("[").substringAfter(",").substringBefore("]")
                    val animeid = doc.selectFirst("div.Strs.RateIt")?.attr("data-id")
                    //val epthumb = "https://cdn.animeflv.net/screenshots/$animeid/$epNum/th_3.jpg"
                    val link = url.replace("/anime/", "/ver/") + "-$epNum"
                    episodes.add(
                        Episode(
                            link,
                            null,
                            //posterUrl = epthumb,
                            episode = epNum.toIntOrNull()
                        )
                    )
                }
            }
        }
        return newAnimeLoadResponse(title, url, getType(type)) {
            posterUrl = fixUrl(poster)
            addEpisodes(DubStatus.Subbed, episodes.reversed())
            showStatus = status
            plot = description
            tags = genre
        }
    }

    data class MainServers(
            @JsonProperty("SUB")
            val sub: List<Sub>,
    )

    data class Sub(
            val code: String,
    )


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = bypasser.get(data).body?.string()?.let { Jsoup.parse(it) }
        doc?.select("script")?.apmap { script ->
            if (script.data().contains("var videos = {") || script.data()
                    .contains("var anime_id = ") || script.data().contains("server")
            ) {
                val serversRegex = Regex("var videos = (\\{\"SUB\":\\\\[\\{\\.???\\}\\]\\\\}\");")
                val serversplain = serversRegex.find(script.data())?.destructured?.component1() ?: ""
                val json = parseJson<MainServers>(serversplain)
                json.sub.apmap {
                    val code = it.code
                    loadExtractor(code, data, subtitleCallback, callback)
                }
            }
        }
        return true
    }
}