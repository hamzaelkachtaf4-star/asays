package com.naviify.app.data.repository

import com.naviify.app.core.network.SubsonicApiException
import com.naviify.app.core.network.SubsonicService
import com.naviify.app.data.lyrics.LrclibLyricsClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class MediaRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: MediaRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = MediaRepository(
            retrofit.create(SubsonicService::class.java),
            LrclibLyricsClient(OkHttpClient()),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `search maps search3 response to domain results`() = runBlocking {
        enqueue("search3.json")

        val results = repository.search("radio")

        assertEquals(1, results.artists.size)
        assertEquals("Radiohead", results.artists.first().name)
        assertEquals(1, results.tracks.size)
        assertEquals("Paranoid Android", results.tracks.first().title)
    }

    @Test
    fun `album list maps albumList2 response`() = runBlocking {
        enqueue("albumlist2.json")

        val albums = repository.getAlbums(AlbumListType.NEWEST, size = 10)

        assertEquals(2, albums.size)
        assertEquals("Random Access Memories", albums.first().name)
        assertEquals(2013, albums.first().year)
    }

    @Test
    fun `failed status surfaces as SubsonicApiException`() = runBlocking {
        enqueue("error.json")

        val exception = assertThrows(SubsonicApiException::class.java) {
            runBlocking { repository.getPlaylists() }
        }

        assertEquals(40, exception.code)
    }

    @Test
    fun `getLyrics returns synced lines`() = runBlocking {
        enqueue("lyrics_structured.json")

        val lyrics = repository.getLyrics("tr-1", "Radiohead", "Airbag")

        assertEquals(2, lyrics.syncedLines.size)
        assertEquals(12_000L, lyrics.syncedLines.first().startMs)
        assertEquals("First line", lyrics.syncedLines.first().text)
    }

    @Test
    fun `getPlaylist with virtual-library returns virtual library playlist`() = runBlocking {
        enqueue("randomsongs.json")

        val playlist = repository.getPlaylist(MediaRepository.VIRTUAL_LIBRARY_PLAYLIST_ID)

        assertEquals(MediaRepository.VIRTUAL_LIBRARY_PLAYLIST_ID, playlist.id)
        assertEquals(MediaRepository.VIRTUAL_LIBRARY_NAME, playlist.name)
        assertEquals(1, playlist.tracks.size)
        assertEquals("Paranoid Android", playlist.tracks.first().title)
    }

    @Test
    fun `getPlaylists prepends virtual library playlist`() = runBlocking {
        enqueue("playlist.json")

        val playlists = repository.getPlaylists()

        assertEquals(2, playlists.size)
        assertEquals(MediaRepository.VIRTUAL_LIBRARY_PLAYLIST_ID, playlists[0].id)
        assertEquals(MediaRepository.VIRTUAL_LIBRARY_NAME, playlists[0].name)
        assertEquals("pl1", playlists[1].id)
    }

    @Test
    fun `getPlaylists does not inject virtual library if server already has My own playlist`() = runBlocking {
        val json = """
            {
              "subsonic-response": {
                "status": "ok",
                "version": "1.16.1",
                "type": "navidrome",
                "serverVersion": "0.52.5",
                "openSubsonic": true,
                "playlists": {
                  "playlist": [
                    {
                      "id": "pl-server-myown",
                      "name": "My own",
                      "comment": "All songs",
                      "owner": "user",
                      "public": true,
                      "songCount": 958,
                      "duration": 180000
                    }
                  ]
                }
              }
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val playlists = repository.getPlaylists()

        assertEquals(1, playlists.size)
        assertEquals("pl-server-myown", playlists[0].id)
        assertEquals("My own", playlists[0].name)
        assertEquals(958, playlists[0].songCount)
    }

    @Test
    fun `addToPlaylist uses updatePlaylist with songIdToAdd and does not overwrite playlist`() = runBlocking {
        enqueue("ping.json")

        repository.addToPlaylist("pl-42", "tr-99")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        val url = request.requestUrl!!
        assertEquals("/rest/updatePlaylist.view", url.encodedPath)
        assertEquals("pl-42", url.queryParameter("playlistId"))
        assertEquals("tr-99", url.queryParameter("songIdToAdd"))
    }

    @Test
    fun `addToPlaylist with multiple tracks appends all songIdToAdd`() = runBlocking {
        enqueue("ping.json")

        repository.addToPlaylist("pl-42", listOf("tr-1", "tr-2"))

        val request = server.takeRequest()
        val url = request.requestUrl!!
        assertEquals("/rest/updatePlaylist.view", url.encodedPath)
        assertEquals(listOf("tr-1", "tr-2"), url.queryParameterValues("songIdToAdd"))
    }

    @Test
    fun `removeFromPlaylist uses updatePlaylist with songIndexToRemove`() = runBlocking {
        enqueue("ping.json")

        repository.removeFromPlaylist("pl-42", 3)

        val request = server.takeRequest()
        val url = request.requestUrl!!
        assertEquals("/rest/updatePlaylist.view", url.encodedPath)
        assertEquals("pl-42", url.queryParameter("playlistId"))
        assertEquals("3", url.queryParameter("songIndexToRemove"))
    }

    private fun enqueue(fileName: String) {
        val body = requireNotNull(javaClass.classLoader!!.getResource(fileName)).readText()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }
}
