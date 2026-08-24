package com.omercloudstream.owned

import com.lagradost.cloudstream3.Actor
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URLEncoder

class DiziYolProvider(
    override var name: String,
    override var mainUrl: String,
) : MainAPI() {
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "movies" to "Filmler",
        "series" to "Diziler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val response = app.get("$mainUrl/api/${request.data}?page=$page&limit=30&lang=tr-TR")
            .parsedSafe<CatalogListResponse>()
        val isSeries = request.data == "series"
        val items = response?.data.orEmpty().mapNotNull { it.toSearchResult(isSeries) }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val movies = app.get("$mainUrl/api/movies?search=$encoded&page=1&limit=30&lang=tr-TR")
            .parsedSafe<CatalogListResponse>()?.data.orEmpty()
            .mapNotNull { it.toSearchResult(false) }
        val series = app.get("$mainUrl/api/series?search=$encoded&page=1&limit=30&lang=tr-TR")
            .parsedSafe<CatalogListResponse>()?.data.orEmpty()
            .mapNotNull { it.toSearchResult(true) }
        return movies + series
    }

    private fun CatalogItem.toSearchResult(isSeries: Boolean): SearchResponse? {
        val itemSlug = slug ?: objectId ?: id?.toString() ?: return null
        val itemTitle = displayTitle() ?: return null
        val url = "$mainUrl/${if (isSeries) "series" else "movie"}/$itemSlug"
        return if (isSeries) {
            newTvSeriesSearchResponse(itemTitle, url, TvType.TvSeries) {
                this.posterUrl = this@toSearchResult.posterUrl
                this.score = Score.from10(voteAverage)
            }
        } else {
            newMovieSearchResponse(itemTitle, url, TvType.Movie) {
                this.posterUrl = this@toSearchResult.posterUrl
                this.score = Score.from10(voteAverage)
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val isSeries = url.contains("/series/")
        val slug = url.substringAfterLast('/').substringBefore('?')
        val kind = if (isSeries) "series" else "movies"
        val item = app.get("$mainUrl/api/$kind/$slug?lang=tr-TR")
            .parsedSafe<CatalogDetailResponse>()?.data ?: return null
        val title = item.displayTitle() ?: return null
        val year = (item.releaseDate ?: item.firstAirDate)?.take(4)?.toIntOrNull()
        val tags = item.genres.mapNotNull { it.name }
        val actors = item.cast.mapNotNull { cast -> cast.name?.let { Actor(it, cast.profileUrl) } }

        if (!isSeries) {
            return newMovieLoadResponse(title, url, TvType.Movie, "movie|$slug") {
                posterUrl = item.posterUrl
                plot = item.overviewTr ?: item.overview ?: item.overviewEn
                this.year = year
                this.tags = tags
                duration = item.runtime
                score = Score.from10(item.voteAverage)
                addActors(actors)
            }
        }

        val seasons = app.get("$mainUrl/api/series/$slug/seasons?lang=tr-TR")
            .parsedSafe<SeasonsResponse>()?.data?.seasons.orEmpty()
        val episodes = seasons.flatMap { season ->
            val seasonNumber = season.seasonNumber ?: return@flatMap emptyList()
            app.get("$mainUrl/api/series/$slug/seasons/$seasonNumber?lang=tr-TR")
                .parsedSafe<EpisodesResponse>()?.data?.episodes.orEmpty()
                .mapNotNull { episode ->
                    val episodeNumber = episode.episodeNumber ?: return@mapNotNull null
                    newEpisode("series|$slug|$seasonNumber|$episodeNumber") {
                        name = episode.name ?: "$episodeNumber. Bölüm"
                        this.season = seasonNumber
                        this.episode = episodeNumber
                        description = episode.overview
                        posterUrl = episode.stillUrl
                    }
                }
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            posterUrl = item.posterUrl
            plot = item.overviewTr ?: item.overview ?: item.overviewEn
            this.year = year
            this.tags = tags
            score = Score.from10(item.voteAverage)
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val parts = data.split('|')
        val endpoint = when (parts.firstOrNull()) {
            "movie" -> "$mainUrl/api/movies/${parts.getOrNull(1)}/stream?lang=tr-TR"
            "series" -> "$mainUrl/api/series/${parts.getOrNull(1)}/seasons/${parts.getOrNull(2)}/episodes/${parts.getOrNull(3)}/stream?lang=tr-TR"
            else -> return false
        }
        val response = app.get(endpoint)
        val stream = response.parsedSafe<StreamResponse>()?.data
        val streamUrl = stream?.streamUrl ?: stream?.embedUrl ?: stream?.m3u8Url ?: stream?.url ?: stream?.src
            ?: Regex("""https?://[^\s\"']+""").find(response.text.replace("\\/", "/"))?.value
            ?: return false

        if (streamUrl.contains(".m3u8", true) || streamUrl.contains(".mp4", true)) {
            val type = if (streamUrl.contains(".m3u8", true)) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            callback(newExtractorLink(name, name, streamUrl, type) {
                referer = mainUrl
                quality = Qualities.Unknown.value
            })
        } else {
            loadExtractor(streamUrl, mainUrl, subtitleCallback, callback)
        }
        return true
    }

    private fun CatalogItem.displayTitle(): String? = titleTr ?: title ?: nameTr ?: name ?: titleEn ?: nameEn
}
