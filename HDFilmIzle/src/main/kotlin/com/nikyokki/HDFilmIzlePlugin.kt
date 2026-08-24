package com.nikyokki

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class HDFilmIzlePlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(HDFilmIzle("https://www.hdfilmizle.ink", "HDFilmİzle Ink"))
        registerMainAPI(HDFilmIzle("https://www.hdfilmizle.vip", "HDFilmİzle Vip"))
        registerExtractorAPI(VidRameExtractor())
    }
}
