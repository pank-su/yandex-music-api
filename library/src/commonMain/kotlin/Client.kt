import dsl.YandexMusicTagMaker
import exceptions.NotAuthenticatedException
import exceptions.SessionExpiredException
import utils.removeCarets
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.*
import model.account.PromoCodeStatus
import model.account.Status
import model.account.UserSettings
import model.ad.Ad
import model.album.Album
import model.feed.Feed
import model.genre.Genre
import model.landing.*
import model.playlist.TagResult
import model.search.QueryType
import model.search.Search
import model.supplement.Supplement
import model.track.SimilarTracks


expect fun getHttpClientEngine(): HttpClientEngine

@YandexMusicTagMaker
class Client {
    var baseUrl: String = "https://api.music.yandex.net"
    var token: String = ""
    var language = "ru"
    var me: Status? = null


    var httpClientEngine: HttpClientEngine = getHttpClientEngine()

    var loggingSettings: Logging.Config.() -> Unit = {
        logger = Logger.DEFAULT
        this.level = LogLevel.BODY
    }

    var requestSettings: HttpRequestBuilder.() -> Unit = {

    }

    private var httpClient = HttpClient(httpClientEngine) {
        install(Logging) {
            loggingSettings()
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private suspend inline fun <reified T> requestPrimitive(
        vararg components: String,
        method: HttpMethod = HttpMethod.Get,
        body: HashMap<String, String> = hashMapOf()
    ): ResultPrimitive<T> {
        return httpClient.request(baseUrl) {
            requestSettings()
            this.method = method
            url {
                appendPathSegments(components.toList())
            }
            headers {

                append("X-Yandex-Music-Client", "YandexMusicAndroid/24023231")
                append("USER_AGENT", "Yandex-Music-API")
                append("Accept-Language", language)

                if (token != "") {
                    append(HttpHeaders.Authorization, "OAuth $token")
                }
            }
            if (method == HttpMethod.Post) {
                headers {
                    append(HttpHeaders.ContentType, "form-encoded")
                }
                formData {
                    parameters {
                        body.forEach {
                            append(it.key, it.value)
                        }
                    }
                }
            }
            headers {
                remove(HttpHeaders.ContentType)
                remove(HttpHeaders.ContentLength)
            }
        }.body<BasicResponsePrimitive<T>>().result.apply { client = this@Client }
    }


    private suspend inline fun <reified T : Result> request(
        components: List<String>,
        method: HttpMethod = HttpMethod.Get,
        body: HashMap<String, String> = hashMapOf()
    ): T {
        return httpClient.request(baseUrl) {
            requestSettings()
            this.method = method
            url {
                appendPathSegments(components)
            }
            headers {

                append("X-Yandex-Music-Client", "YandexMusicAndroid/24023231")
                append("USER_AGENT", "Yandex-Music-API")
                append("Accept-Language", language)

                if (token != "") {
                    append(HttpHeaders.Authorization, "OAuth $token")
                }
            }
            if (method == HttpMethod.Post) {
                headers {
                    append(HttpHeaders.ContentType, "form-encoded")
                }
                formData {
                    parameters {
                        body.forEach {
                            append(it.key, it.value)
                        }
                    }
                }
            } else if (method == HttpMethod.Get) {
                url {
                    body.forEach {
                        parameters.append(it.key, it.value)
                    }
                }
            }
        }.body<BasicResponse<T>>().result.apply { client = this@Client }
    }

    internal suspend inline fun <reified T : Result> requestForm(
        vararg components: String,
        body: HashMap<String, String> = hashMapOf()
    ): T {
        return httpClient.submitForm(baseUrl, formParameters =
        parameters {
            body.forEach {
                append(it.key, it.value)
            }
        }) {
            requestSettings()
            url {
                appendPathSegments(components.toList())
            }
            headers {
                append("X-Yandex-Music-Client", "YandexMusicAndroid/24023231")
                append("USER_AGENT", "Yandex-Music-API")
                if (token != "") {
                    append(HttpHeaders.Authorization, "OAuth $token")
                }
            }
        }.body<BasicResponse<T>>().result.apply { client = this@Client }
    }


    private suspend inline fun <reified T : Result> request(
        vararg components: String,
        method: HttpMethod = HttpMethod.Get
    ): T {
        return request(components.toList(), method)
    }

    internal suspend inline fun <reified T : Result> requestPost(
        vararg components: String,
        body: HashMap<String, String> = hashMapOf()
    ): T {
        return request(components.toList(), method = HttpMethod.Post, body)
    }


    suspend fun init(init: Client.() -> Unit) {
        this.init()
        try {
            me = status()
        } catch (e: SessionExpiredException) {
            println("Токен некореектный или истёк")
        }
    }

    suspend fun status() = request<Status>("account", "status")
    suspend fun userSettings() = request<UserSettings>("account", "settings")

    suspend fun ads() = request<Ad>("settings")

    suspend fun permissionAlerts() = request<PermissionAlerts>("permission-alerts")

    suspend fun accountExperiments() = requestPrimitive<HashMap<String, String>>("account", "experiments")

    suspend fun consumePromoCode(code: String) = requestForm<PromoCodeStatus>(
        "account",
        "consume-promo-code",
        body = hashMapOf("code" to code, "language" to language)
    )

    suspend fun feed() = request<Feed>("feed")

    suspend fun feedWizardIsPassed() =
        requestPrimitive<HashMap<String, Boolean>>("feed", "wizard", "is-passed").value!!["isWizardPassed"] as Boolean

    suspend fun landing(vararg blocks: BlockType) =
        request<Landing>(
            listOf("landing3"),
            body = hashMapOf(
                "blocks" to blocks.joinToString(",") {
                    Json.encodeToString(it).removeCarets()

                },
                "eitherUserId" to (me?.account?.uid ?: throw NotAuthenticatedException()).toString()
            )
        )

    suspend fun chart(chartOption: ChartOption? = null) =
        request<ChartInfo>(
            "landing3", "chart", if (chartOption != null) {
                Json.encodeToString(chartOption).removeCarets()
            } else ""
        )

    suspend fun newReleases() = request<LandingList>("landing3", "new-releases")

    suspend fun newPlaylists() = request<LandingList>("landing3", "new-playlists")

    suspend fun podcasts() = request<LandingList>("landing3", "podcasts")

    suspend fun genres() = requestPrimitive<List<Genre>>("genres")

    suspend fun tags(tagId: String) = request<TagResult>("tags", tagId, "playlist-ids")

    suspend fun tracksDownloadInfo(trackId: Int) =
        requestPrimitive<List<DownloadInfo>>("tracks", trackId.toString(), "download-info")

    suspend fun trackSupplement(trackId: Int) = request<Supplement>("tracks", trackId.toString(), "supplement")

    suspend fun tracksLyrics(trackId: Int, format: String = "TEXT"): Result =
        TODO("Необходима реализация get_sign_request")

    suspend fun tracksSimilar(trackId: Int) = request<SimilarTracks>("tracks", trackId.toString(), "similar")

    suspend fun playAudio(
        trackId: Int,
        from: String,
        albumId: Int,
        playlistId: String? = null,
        fromCache: Boolean = false,
        playId: String? = null,
        uid: Int? = null,
        timestamp: Instant? = null,
        trackLengthSeconds: Int = 0,
        totalPlayedSeconds: Int = 0,
        endPositionSeconds: Int = 0,
        clientNow: Instant? = null
    ) {
        TODO("Реализация позже")
    }

    suspend fun albumsWithTracks(albumId: Int) = request<Album>("albums", albumId.toString(), "with-tracks")

    suspend fun search(
        query: String,
        isCorrect: Boolean = false,
        type: QueryType = QueryType.All,
        page: Int = 0,
        playlistInBest: Boolean = false
    ) = request<Search>(
        listOf("search"),
        body = hashMapOf(
            "text" to query,
            "nocorrect" to isCorrect.toString().lowercase(),
            "type" to type.name.lowercase(),
            "page" to page.toString(),
            "playlist-in-best" to playlistInBest.toString().lowercase()
        )
    ).apply {
        this.type = type
        this.page = page
    }
}


