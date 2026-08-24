package com.omercloudstream.owned

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class OwnedSitesPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DiziPlusProvider("TrDiziİzle", "https://www.trdiziizle.tv", "/tr1/", "/diziler/"))
        registerMainAPI(DiziYolProvider("DiziBal", "https://dizibal.com"))
        registerMainAPI(DiziPlusProvider("DiziRella", "https://dizirella.net", "/", "/dizi/"))
        registerMainAPI(DiziYolProvider("DiziBol", "https://dizibol.com"))
        registerMainAPI(HtmlMovieProvider(HtmlMovieSite.FILM_IZZLE))
        registerMainAPI(HtmlMovieProvider(HtmlMovieSite.LIDER_FILM))
        registerMainAPI(HtmlMovieProvider(HtmlMovieSite.DIZI_FILM))
    }
}
