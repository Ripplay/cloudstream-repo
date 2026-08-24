package com.omercloudstream.owned

import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

class DiziPlusProvider(
    override var name: String,
    override var mainUrl: String,
    private val homePath: String,
    private val seriesPrefix: String,
) : MainAPI() {
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.TvSeries)
    override val mainPage = mainPageOf("$mainUrl$homePath" to "Diziler")

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else request.data.trimEnd('/') + "/page/$page/"
        val items = parseSeriesCards(app.get(url).document)
        return newHomePageResponse(request.name, items)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return parseSeriesCards(app.get("$mainUrl/?s=$encoded").document)
    }

    private fun parseSeriesCards(document: Document): List<SearchResponse> = document
        .select("a[href*='$seriesPrefix']")
        .mapNotNull { it.toSeriesSearchResult() }
        .distinctBy { it.url }

    private fun Element.toSeriesSearchResult(): SearchResponse? {
        val href = fixUrlNull(attr("href")) ?: return null
        if (!href.contains(seriesPrefix)) return null
        val image = selectFirst("img")
        val title = attr("title").ifBlank {
            image?.attr("alt").orEmpty().ifBlank {
                selectFirst("h1,h2,h3,h4,.title,.series-title,.episode-title")?.text().orEmpty()
            }
        }.replace(Regex("""\s+izle.*$""", RegexOption.IGNORE_CASE), "").trim()
        if (title.isBlank()) return null
        val poster = image?.attr("data-src")?.ifBlank { image.attr("src") }
        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            posterUrl = fixUrlNull(poster)
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=\"og:title\"]")?.attr("content")
            ?.substringBefore(" izle")?.substringBefore(" |")
            ?: document.selectFirst("h1")?.text()?.trim()
            ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=\"og:image\"]")?.attr("content"))
        val plot = document.selectFirst("meta[name=\"description\"]")?.attr("content")
        val episodes = document.select("a[href]").mapNotNull { anchor ->
            val href = fixUrlNull(anchor.attr("href")) ?: return@mapNotNull null
            if (!href.contains("bolum", true) || href.contains(seriesPrefix)) return@mapNotNull null
            val label = anchor.attr("title").ifBlank { anchor.text() }.ifBlank { href.substringAfterLast('/') }
            val season = Regex("""(\d+)[.-]?\s*[Ss]ezon""").find(label)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""-(\d+)-sezon""").find(href)?.groupValues?.get(1)?.toIntOrNull()
                ?: 1
            val episode = Regex("""(\d+)[.-]?\s*[Bb]ölüm""").find(label)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""-(\d+)-bolum""").find(href)?.groupValues?.get(1)?.toIntOrNull()
                ?: return@mapNotNull null
            newEpisode(href) {
                name = label.trim().ifBlank { "$episode. Bölüm" }
                this.season = season
                this.episode = episode
            }
        }.distinctBy { it.data }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean = loadOwnedSiteLinks(data, subtitleCallback, callback)
}
