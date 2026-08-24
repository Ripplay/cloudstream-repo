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
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

enum class HtmlMovieSite(
    val providerName: String,
    val baseUrl: String,
    val moviePrefix: String,
) {
    FILM_IZZLE("FilmIzzle", "https://filmizzle.com", "/film/"),
    LIDER_FILM("LiderFilmİzle", "https://liderfilmizle.com", "/"),
    DIZI_FILM("DiziFilmİzle", "https://dizifilmizle.to", "/film/"),
}

class HtmlMovieProvider(private val site: HtmlMovieSite) : MainAPI() {
    override var name = site.providerName
    override var mainUrl = site.baseUrl
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie)
    override val mainPage = mainPageOf(mainUrl to "Filmler")

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = when (site) {
            HtmlMovieSite.FILM_IZZLE -> if (page == 1) mainUrl else "$mainUrl/sayfa/$page"
            HtmlMovieSite.LIDER_FILM -> if (page == 1) mainUrl else "$mainUrl/sayfa/$page"
            HtmlMovieSite.DIZI_FILM -> if (page == 1) mainUrl else "$mainUrl/?page=$page"
        }
        return newHomePageResponse(request.name, parseCards(app.get(url).document))
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val candidates = when (site) {
            HtmlMovieSite.FILM_IZZLE -> listOf("$mainUrl/arama?q=$encoded")
            HtmlMovieSite.LIDER_FILM -> listOf("$mainUrl/?s=$encoded", "$mainUrl/arama?q=$encoded")
            HtmlMovieSite.DIZI_FILM -> listOf("$mainUrl/arama?q=$encoded", "$mainUrl/?q=$encoded")
        }
        for (url in candidates) {
            val results = runCatching { parseCards(app.get(url).document) }.getOrDefault(emptyList())
            if (results.isNotEmpty()) return results
        }
        return emptyList()
    }

    private fun parseCards(document: Document): List<SearchResponse> = document.select("a[href]")
        .mapNotNull { it.toMovieSearchResult() }
        .distinctBy { it.url }

    private fun Element.toMovieSearchResult(): SearchResponse? {
        val href = fixUrlNull(attr("href")) ?: return null
        if (!isMovieUrl(href)) return null
        val image = selectFirst("img") ?: parent()?.selectFirst("img")
        val title = attr("title").ifBlank {
            image?.attr("alt").orEmpty().ifBlank {
                selectFirst("h1,h2,h3,h4,.title,.movie-title")?.text().orEmpty()
            }
        }.replace(Regex("""\s+izle.*$""", RegexOption.IGNORE_CASE), "").trim()
        if (title.isBlank()) return null
        val poster = image?.attr("data-src")?.ifBlank { image.attr("src") }
        return newMovieSearchResponse(title, href, TvType.Movie) {
            posterUrl = fixUrlNull(poster)
        }
    }

    private fun isMovieUrl(url: String): Boolean {
        if (!url.startsWith(mainUrl)) return false
        if (site != HtmlMovieSite.LIDER_FILM) return url.contains(site.moviePrefix)
        val path = url.removePrefix(mainUrl).substringBefore('?').trim('/')
        if (path.isBlank() || path.contains('/')) return false
        return path !in setOf("filmler", "diziler", "iletisim", "hakkimizda", "giris", "kayit", "arama")
    }

    override suspend fun load(url: String): LoadResponse? {
        val response = app.get(url)
        val document = response.document
        val title = document.selectFirst("meta[property=\"og:title\"]")?.attr("content")
            ?.substringBefore(" izle")?.substringBefore(" |")?.substringBefore(" - ")
            ?: document.selectFirst("h1")?.text()?.trim()
            ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=\"og:image\"]")?.attr("content"))
        val plot = document.selectFirst("meta[name=\"description\"]")?.attr("content")
        val year = Regex("""\b(19|20)\d{2}\b""").find(title + " " + plot)?.value?.toIntOrNull()
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            posterUrl = poster
            this.plot = plot
            this.year = year
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean = loadOwnedSiteLinks(data, subtitleCallback, callback)
}
