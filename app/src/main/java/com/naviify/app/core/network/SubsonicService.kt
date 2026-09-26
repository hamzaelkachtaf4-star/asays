package com.naviify.app.core.network

import com.naviify.app.core.network.dto.SubsonicEnvelope
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * REST endpoints of the Subsonic API. Auth query parameters (`u`, `t`, `s`,
 * `v`, `c`, `f`) are injected by [SubsonicAuthInterceptor] at request time.
 */
interface SubsonicService {

    @GET("rest/ping.view")
    suspend fun ping(): SubsonicEnvelope

    @GET("rest/getArtists.view")
    suspend fun getArtists(): SubsonicEnvelope

    @GET("rest/getArtist.view")
    suspend fun getArtist(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/getArtistInfo2.view")
    suspend fun getArtistInfo2(
        @Query("id") id: String,
        @Query("count") count: Int = 10,
    ): SubsonicEnvelope

    @GET("rest/getArtistInfo.view")
    suspend fun getArtistInfo(
        @Query("id") id: String,
        @Query("count") count: Int = 10,
    ): SubsonicEnvelope

    @GET("rest/getTopSongs.view")
    suspend fun getTopSongs(
        @Query("artist") artist: String,
        @Query("count") count: Int = 20,
    ): SubsonicEnvelope

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 20,
        @Query("albumCount") albumCount: Int = 20,
        @Query("songCount") songCount: Int = 50,
    ): SubsonicEnvelope

    @GET("rest/getPlaylists.view")
    suspend fun getPlaylists(): SubsonicEnvelope

    @GET("rest/getPlaylist.view")
    suspend fun getPlaylist(@Query("id") id: String): SubsonicEnvelope

    /** Album list views used for Home/library grids (types: newest, random, starred, ...). */
    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String,
        @Query("size") size: Int,
        @Query("offset") offset: Int = 0,
    ): SubsonicEnvelope

    /** OpenSubsonic synced lyrics for a song. */
    @GET("rest/getLyricsBySongId.view")
    suspend fun getLyricsBySongId(
        @Query("id") id: String,
        @Query("lang") lang: String? = null,
    ): SubsonicEnvelope

    @GET("rest/getSong.view")
    suspend fun getSong(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/getRandomSongs.view")
    suspend fun getRandomSongs(
        @Query("size") size: Int = 500,
        @Query("genre") genre: String? = null,
    ): SubsonicEnvelope

    @GET("rest/getSimilarSongs2.view")
    suspend fun getSimilarSongs2(
        @Query("id") id: String,
        @Query("count") count: Int = 50,
    ): SubsonicEnvelope

    /** Legacy Subsonic lyrics endpoint (plain text). */
    @GET("rest/getLyrics.view")
    suspend fun getLyrics(
        @Query("artist") artist: String?,
        @Query("title") title: String?,
    ): SubsonicEnvelope

    @POST("rest/createPlaylist.view")
    suspend fun createPlaylist(
        @Query("name") name: String,
        @Query("playlistId") playlistId: String? = null,
        @Query("songId") songIds: List<String> = emptyList(),
    ): SubsonicEnvelope

    @POST("rest/updatePlaylist.view")
    suspend fun updatePlaylist(
        @Query("playlistId") playlistId: String,
        @Query("name") name: String? = null,
        @Query("comment") comment: String? = null,
        @Query("public") public: Boolean? = null,
        @Query("songIdToAdd") songIdsToAdd: List<String>? = null,
        @Query("songIndexToRemove") songIndexesToRemove: List<Int>? = null,
    ): SubsonicEnvelope

    @GET("rest/deletePlaylist.view")
    suspend fun deletePlaylist(
        @Query("id") id: String,
    ): SubsonicEnvelope

    @POST("rest/scrobble.view")
    suspend fun scrobble(
        @Query("id") id: String,
        @Query("time") time: Long? = null,
        @Query("submission") submission: Boolean = true,
    ): SubsonicEnvelope

    @GET("rest/star.view")
    suspend fun star(@Query("id") ids: List<String>): SubsonicEnvelope

    @GET("rest/unstar.view")
    suspend fun unstar(@Query("id") ids: List<String>): SubsonicEnvelope

    // Binary stream and cover-art endpoints are intentionally absent: Media3
    // and Coil fetch them through pre-signed URLs from SubsonicUrlProvider, and
    // routing them through Retrofit would append the JSON `f` parameter.
    @GET("rest/startScan.view")
    suspend fun startScan(@Query("fullScan") fullScan: Boolean = true): SubsonicEnvelope

    @GET("rest/getScanStatus.view")
    suspend fun getScanStatus(): SubsonicEnvelope
}
