package com.omercloudstream.owned

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Document

internal suspend fun MainAPI.loadOwnedSiteLinks(
    pageUrl: String,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
): Boolean {
    val response = app.get(pageUrl, referer = mainUrl)
    val firstLevel = collectPlayerCandidates(response.document, response.text)
    var found = false

    for (candidate in firstLevel.distinct()) {
        val fixed = fixUrlNull(candidate) ?: continue
        if (emitDirectLink(fixed, pageUrl, callback)) {
            found = true
            continue
        }

        if (fixed.startsWith(mainUrl)) {
            runCatching {
                val nested = app.get(fixed, referer = pageUrl)
                collectPlayerCandidates(nested.document, nested.text).distinct().forEach { nestedUrl ->
                    val nestedFixed = fixUrlNull(nestedUrl) ?: return@forEach
                    if (emitDirectLink(nestedFixed, fixed, callback)) {
                        found = true
                    } else if (nestedFixed != fixed) {
                        loadExtractor(nestedFixed, fixed, subtitleCallback, callback)
                        found = true
                    }
                }
            }
        }

        loadExtractor(fixed, pageUrl, subtitleCallback, callback)
        found = true
    }

    return found
}

private fun MainAPI.collectPlayerCandidates(document: Document, rawHtml: String): List<String> {
    val urls = mutableListOf<String>()
    document.select("iframe[src], iframe[data-src], video[src], source[src], a[data-video], a[data-iframe]")
        .forEach { element ->
            listOf("src", "data-src", "data-video", "data-iframe")
                .map { element.attr(it) }
                .filter { it.isNotBlank() }
                .forEach(urls::add)
        }

    val normalized = rawHtml
        .replace("\\/", "/")
        .replace("\\u0026", "&")
        .replace("&amp;", "&")

    Regex("""https?://[^\s\"'<>\\]+""").findAll(normalized).forEach { match ->
        val url = match.value.trimEnd(')', ']', '}', ',')
        if (
            url.contains(".m3u8", true) || url.contains(".mp4", true) ||
            url.contains("/embed", true) || url.contains("/player", true) ||
            url.contains("vidmoly", true) || url.contains("stream", true) ||
            url.contains("rplayer", true)
        ) urls.add(url)
    }

    return urls.filterNot {
        it.contains("youtube.com/embed", true) ||
            it.contains("wp-json/oembed", true)
    }
}

private suspend fun MainAPI.emitDirectLink(
    url: String,
    referer: String,
    callback: (ExtractorLink) -> Unit,
): Boolean {
    val clean = url.substringBefore("\\\"").substringBefore('"')
    val type = when {
        clean.contains(".m3u8", true) -> ExtractorLinkType.M3U8
        clean.contains(".mp4", true) -> ExtractorLinkType.VIDEO
        else -> return false
    }
    callback(
        newExtractorLink(name, name, clean, type) {
            this.referer = referer
            this.quality = Qualities.Unknown.value
        }
    )
    return true
}
