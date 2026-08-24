package com.nikyokki

import Video
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.Actor
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64DecodeArray
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Element
import kotlin.random.Random

class HDFilmIzle(
    private val baseUrl: String,
    private val providerName: String,
) : MainAPI() {
    override var mainUrl = baseUrl
    override var name = providerName
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/tur/aile/" to "Aile Filmleri",
        "${mainUrl}/tur/aksiyon/" to "Aksiyon Filmleri",
        "${mainUrl}/tur/animasyon/" to "Animasyon Filmleri",
        "${mainUrl}/tur/belgesel/" to "Belgesel Filmleri",
        "${mainUrl}/tur/bilim-kurgu/" to "Bilim Kurgu Filmleri",
        "${mainUrl}/tur/dram/" to "Dram Filmleri",
        "${mainUrl}/tur/fantastik/" to "Fantastik Filmleri",
        "${mainUrl}/tur/gerilim/" to "Gerilim Filmleri",
        "${mainUrl}/tur/gizem/" to "Gizem Filmleri",
        "${mainUrl}/tur/komedi/" to "Komedi Filmleri",
        "${mainUrl}/tur/korku/" to "Korku Filmleri",
        "${mainUrl}/tur/macera/" to "Macera Filmleri",
        "${mainUrl}/tur/romantik/" to "Romantik Filmler",
        "${mainUrl}/tur/savas/" to "Savaş Filmleri",
        "${mainUrl}/tur/suc/" to "Suç Filmleri",
        "${mainUrl}/tur/tarih/" to "Tarih Filmleri",
        "${mainUrl}/tur/vahsi-bati/" to "Vahşi Batı Filmleri",
        "${mainUrl}/tur/yerli-film-izle/" to "Yerli Filmler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document

        val home: List<SearchResponse>?

        home = document.select("div#moviesListResult a.poster").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.selectFirst("h2.title")?.text() ?: ""
        val href = fixUrlNull(this.attr("href")) ?: ""
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val score = this.selectFirst("div.poster-imdb")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score = Score.from10(score)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.post(
            "$mainUrl/search/",
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
            referer = mainUrl,
            data = mapOf("query" to query)
        ).document
        val searchResults = mutableListOf<SearchResponse>()

        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        try {
            val videos: List<Video> = objectMapper.readValue(response.body().text())
            videos.forEach { video ->
                val title = video.name
                val href = fixUrlNull(video.slug) ?: return@forEach
                val posterUrl = fixUrlNull(video.thumbUrl) ?: fixUrlNull(video.thumbWebp)

                searchResults.add(
                    newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
                )
            }
        } catch (e: Exception) {
            println("Error parsing JSON: ${e.message}")
        }

        return searchResults
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val orgTitle = document.selectFirst("div.page-title h1")?.text() ?: ""
        val altTitle =
            document.selectFirst("div.page-title")?.selectFirst("small.text-muted.alt-name")?.text()
                ?: ""
        val title =
            if (altTitle.isNotEmpty() && orgTitle != altTitle) "$orgTitle - $altTitle" else orgTitle
        val poster = fixUrlNull(document.selectFirst("picture.poster-auto img")?.attr("data-src"))
        val tags = document.select("div.pb-2.genres a").map { it.text() }
        val year = document.selectFirst("div.page-title")?.selectFirst("small.text-muted")?.text()
            ?.replace("(", "")?.replace(")", "")?.toIntOrNull()
        val description = document.selectFirst("article.text-white > p")?.text()?.trim()
        val rating = document.selectFirst("div.rate.mb-2 span")?.text()
        val actors = document.select("div.stories-wrapper a").map {
            Actor(
                it.selectFirst("div.story-item-title")!!.text(),
                fixUrlNull(it.select("img").attr("data-src"))
            )
        }

        val recommendations = document.select("div#swiper-wrapper-benzer").mapNotNull {
            val recName = it.selectFirst("a")?.attr("title") ?: return@mapNotNull null
            val recHref = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val recPosterUrl = fixUrlNull(it.selectFirst("img")?.attr("data-src"))
                ?: fixUrlNull(it.selectFirst("img")?.attr("src"))

            newMovieSearchResponse(recName, recHref, TvType.Movie) {
                this.posterUrl = recPosterUrl
            }
        }
        val trailer = document.selectFirst("div.nav-link")?.attr("data-trailer")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.score = Score.from10(rating)
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("HDF", "data » ${data}")
        val response = app.get(data)
        val document = response.document
        val nonce = Regex("""video\s*:\s*[\"]([^\"]+)[\"]""")
            .find(response.text)?.groupValues?.getOrNull(1)
            ?: document.selectFirst("#playex[data-nonce]")?.attr("data-nonce")
        val postId = document.selectFirst("[data-post-id]")?.attr("data-post-id")
        val players = document.select("[data-post-id][data-player-name]")
            .map {
                Triple(
                    it.attr("data-post-id"),
                    it.attr("data-player-name"),
                    it.attr("data-part-key"),
                )
            }
            .filter { (id, player, _) -> id.isNotBlank() && player.isNotBlank() }
            .ifEmpty {
                if (!postId.isNullOrBlank()) listOf(Triple(postId, "SetPlay", "")) else emptyList()
            }
            .distinct()

        var found = false
        if (!nonce.isNullOrBlank()) {
            players.forEach { (id, player, partKey) ->
                val ajax = runCatching {
                    app.post(
                        "$mainUrl/wp-admin/admin-ajax.php",
                        referer = data,
                        headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
                        data = mapOf(
                            "action" to "get_video_url",
                            "nonce" to nonce,
                            "post_id" to id,
                            "player_name" to player,
                            "part_key" to partKey,
                        ),
                    ).parsedSafe<VideoAjaxResponse>()
                }.getOrNull()
                val iframe = ajax?.data?.url ?: return@forEach
                found = if (iframe.contains("setplay.", true)) {
                    resolveSetPlay(iframe, data, callback) || found
                } else {
                    loadExtractor(iframe, data, subtitleCallback, callback) || found
                }
            }
        }

        if (!found) {
            document.select("iframe[src], iframe[data-src]").forEach { frame ->
                val iframe = frame.attr("data-src").ifBlank { frame.attr("src") }
                if (iframe.isNotBlank() && !iframe.contains("youtube.com/embed")) {
                    found = loadExtractor(iframe, data, subtitleCallback, callback) || found
                }
            }
        }

        return found
    }

    private suspend fun resolveSetPlay(
        setPlayUrl: String,
        pageReferer: String,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val setPlay = app.get(setPlayUrl, referer = pageReferer)
        val frameArgs = Regex(
            """SPG\.cerceve\([^,]+,\s*[\"]([^\"]+)[\"]\s*,\s*[\"]([^\"]+)[\"]"""
        ).find(setPlay.text) ?: return false
        val encrypted = base64DecodeArray(frameArgs.groupValues[1])
        val key = base64DecodeArray(frameArgs.groupValues[2])
        if (key.isEmpty()) return false
        val fastPlayUrl = encrypted.mapIndexed { index, byte ->
            (byte.toInt() xor key[index % key.size].toInt()).toByte()
        }.toByteArray().toString(Charsets.UTF_8).substringBefore('|')
        if (!fastPlayUrl.startsWith("http")) return false

        val fastPlay = app.get(fastPlayUrl, referer = setPlayUrl)
        val sp = Regex("""[\"]sp[\"]\s*:\s*[\"]([^\"]+)[\"]""")
            .find(fastPlay.text)?.groupValues?.getOrNull(1) ?: return false
        val spTime = Regex("""[\"]spT[\"]\s*:\s*(\d+)""")
            .find(fastPlay.text)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return false
        val stream = Regex("""stream\s*:\s*[\"]([^\"]+)[\"]""")
            .find(fastPlay.text)?.groupValues?.getOrNull(1)
            ?.replace("&amp;", "&") ?: return false
        val origin = fastPlayUrl.substringBefore("/video/")
        val manifest = if (stream.startsWith("http")) stream else "$origin/${stream.trimStart('/')}"
        val randomPart = Random.nextLong(2_176_782_336L).toString(36)
        val proof = "$sp|$spTime|$randomPart"
        var hash = 0x811c9dc5u
        proof.forEach { hash = (hash xor it.code.toUInt()) * 0x01000193u }
        val xSp = "$spTime.$randomPart.${hash.toString(16)}"

        callback(
            newExtractorLink(name, "$name - SetPlay", manifest, ExtractorLinkType.M3U8) {
                this.referer = fastPlayUrl
                this.quality = Qualities.Unknown.value
                this.headers = mapOf("X-Sp" to xSp)
            }
        )
        return true
    }

    private data class VideoAjaxResponse(
        @JsonProperty("success") val success: Boolean = false,
        @JsonProperty("data") val data: VideoAjaxData? = null,
    )

    private data class VideoAjaxData(
        @JsonProperty("url") val url: String? = null,
    )

    private data class SubSource(
        @JsonProperty("file") val file: String? = null,
        @JsonProperty("label") val label: String? = null,
        @JsonProperty("kind") val kind: String? = null
    )

    data class Results(
        @JsonProperty("results") val results: List<String> = arrayListOf()
    )
}
