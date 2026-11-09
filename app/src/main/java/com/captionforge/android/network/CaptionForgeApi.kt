package com.captionforge.android.network

import com.captionforge.android.model.BackendConfig
import com.captionforge.android.model.ExportRequest
import com.captionforge.android.model.JobStartResponse
import com.captionforge.android.model.JobStatusResponse
import com.captionforge.android.model.Project
import com.captionforge.android.model.ProjectSummary
import com.captionforge.android.model.ProjectUpdatePayload
import com.captionforge.android.model.TrackMotionRequest
import com.captionforge.android.model.TrackMotionResponse
import com.captionforge.android.model.TranscribeRequest
import com.captionforge.android.model.TranslateRequest
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface CaptionForgeApiService {
    @GET("api/config")
    suspend fun getConfig(): BackendConfig

    @GET("api/projects")
    suspend fun getProjects(): List<ProjectSummary>

    @Multipart
    @POST("api/projects")
    suspend fun createProject(@Part file: MultipartBody.Part): Project

    @GET("api/projects/{pid}")
    suspend fun getProject(@Path("pid") pid: String): Project

    @PUT("api/projects/{pid}")
    suspend fun updateProject(
        @Path("pid") pid: String,
        @Body payload: ProjectUpdatePayload
    ): Project

    @DELETE("api/projects/{pid}")
    suspend fun deleteProject(@Path("pid") pid: String)

    @POST("api/projects/{pid}/transcribe")
    suspend fun transcribe(
        @Path("pid") pid: String,
        @Body body: TranscribeRequest
    ): JobStartResponse

    @POST("api/projects/{pid}/translate")
    suspend fun translate(
        @Path("pid") pid: String,
        @Body body: TranslateRequest
    ): JobStartResponse

    @POST("api/projects/{pid}/export")
    suspend fun exportVideo(
        @Path("pid") pid: String,
        @Body body: ExportRequest
    ): JobStartResponse

    @POST("api/projects/{pid}/track")
    suspend fun trackMotion(
        @Path("pid") pid: String,
        @Body body: TrackMotionRequest
    ): TrackMotionResponse

    @Multipart
    @POST("api/projects/{pid}/srt")
    suspend fun importSrt(
        @Path("pid") pid: String,
        @Part file: MultipartBody.Part
    ): ResponseBody

    @GET("api/projects/{pid}/srt")
    suspend fun exportSrt(
        @Path("pid") pid: String,
        @Query("track") trackId: String = ""
    ): ResponseBody

    @Multipart
    @POST("api/fonts")
    suspend fun uploadCustomFont(@Part file: MultipartBody.Part): ResponseBody

    @GET("api/jobs/{jobId}")
    suspend fun getJobStatus(@Path("jobId") jobId: String): JobStatusResponse
}

object BackendClient {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private var currentBaseUrl: String = "http://10.0.2.2:8000/"
    private var cachedApi: CaptionForgeApiService? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getBaseUrl(): String = currentBaseUrl.removeSuffix("/")

    fun setBaseUrl(rawUrl: String) {
        val trimmed = rawUrl.trim().ifEmpty { "http://10.0.2.2:8000" }
        val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        if (normalized != currentBaseUrl || cachedApi == null) {
            currentBaseUrl = normalized
            cachedApi = null
        }
    }

    fun api(): CaptionForgeApiService {
        val existing = cachedApi
        if (existing != null) return existing
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return retrofit.create(CaptionForgeApiService::class.java).also {
            cachedApi = it
        }
    }

    fun getVideoStreamUrl(pid: String): String = "${getBaseUrl()}/api/projects/$pid/video"
    fun getDownloadUrl(pid: String): String = "${getBaseUrl()}/api/projects/$pid/download"
}
