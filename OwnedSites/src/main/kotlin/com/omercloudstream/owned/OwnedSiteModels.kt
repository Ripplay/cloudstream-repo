package com.omercloudstream.owned

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class CatalogListResponse(
    val success: Boolean = false,
    val data: List<CatalogItem> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CatalogDetailResponse(
    val success: Boolean = false,
    val data: CatalogItem? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CatalogItem(
    @JsonProperty("_id") val objectId: String? = null,
    val id: Long? = null,
    val slug: String? = null,
    val title: String? = null,
    @JsonProperty("title_tr") val titleTr: String? = null,
    @JsonProperty("title_en") val titleEn: String? = null,
    val name: String? = null,
    @JsonProperty("name_tr") val nameTr: String? = null,
    @JsonProperty("name_en") val nameEn: String? = null,
    val overview: String? = null,
    @JsonProperty("overview_tr") val overviewTr: String? = null,
    @JsonProperty("overview_en") val overviewEn: String? = null,
    @JsonProperty("poster_url") val posterUrl: String? = null,
    @JsonProperty("vote_average") val voteAverage: Double? = null,
    @JsonProperty("release_date") val releaseDate: String? = null,
    @JsonProperty("first_air_date") val firstAirDate: String? = null,
    val runtime: Int? = null,
    val genres: List<CatalogGenre> = emptyList(),
    val cast: List<CatalogCast> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CatalogGenre(val name: String? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CatalogCast(
    val name: String? = null,
    @JsonProperty("profile_url") val profileUrl: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeasonsResponse(val data: SeasonsData? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeasonsData(val seasons: List<SeasonData> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeasonData(
    @JsonProperty("season_number") val seasonNumber: Int? = null,
    val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodesResponse(val data: EpisodesData? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodesData(
    @JsonProperty("season_number") val seasonNumber: Int? = null,
    val episodes: List<EpisodeData> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodeData(
    @JsonProperty("episode_number") val episodeNumber: Int? = null,
    val name: String? = null,
    val overview: String? = null,
    @JsonProperty("still_url") val stillUrl: String? = null,
    val src: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StreamResponse(val data: StreamData? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StreamData(
    @JsonProperty("streamUrl") val streamUrl: String? = null,
    @JsonProperty("embedUrl") val embedUrl: String? = null,
    @JsonProperty("m3u8Url") val m3u8Url: String? = null,
    val url: String? = null,
    val src: String? = null,
)
