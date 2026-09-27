package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// AlAdhan API DTOs
@JsonClass(generateAdapter = true)
data class AlAdhanTimingsResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: AlAdhanData?
)

@JsonClass(generateAdapter = true)
data class AlAdhanData(
    @Json(name = "timings") val timings: Map<String, String>?,
    @Json(name = "date") val date: AlAdhanDateInfo?,
    @Json(name = "meta") val meta: AlAdhanMeta? = null
)

@JsonClass(generateAdapter = true)
data class AlAdhanMeta(
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "school") val school: String? = null
)

@JsonClass(generateAdapter = true)
data class AlAdhanDateInfo(
    @Json(name = "readable") val readable: String?,
    @Json(name = "hijri") val hijri: AlAdhanHijri?
)

@JsonClass(generateAdapter = true)
data class AlAdhanHijri(
    @Json(name = "date") val date: String?,
    @Json(name = "day") val day: String?,
    @Json(name = "month") val month: AlAdhanHijriMonth?,
    @Json(name = "year") val year: String?
)

@JsonClass(generateAdapter = true)
data class AlAdhanHijriMonth(
    @Json(name = "number") val number: Int?,
    @Json(name = "en") val en: String?,
    @Json(name = "ar") val ar: String?
)

interface AlAdhanApi {
    @GET("v1/timingsByCity/{date}")
    suspend fun getTimingsByCity(
        @Path("date") date: String,
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int,
        @Query("school") school: Int = 1
    ): AlAdhanTimingsResponse

    @GET("v1/timings/{date}")
    suspend fun getTimingsByCoordinates(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int,
        @Query("school") school: Int = 1
    ): AlAdhanTimingsResponse
}

// AlQuran Cloud DTOs
@JsonClass(generateAdapter = true)
data class AlQuranSurahListResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: List<AlQuranSurahDto>?
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahDto(
    @Json(name = "number") val number: Int,
    @Json(name = "name") val name: String,
    @Json(name = "englishName") val englishName: String,
    @Json(name = "englishNameTranslation") val englishNameTranslation: String,
    @Json(name = "numberOfAyahs") val numberOfAyahs: Int,
    @Json(name = "revelationType") val revelationType: String
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahEditionsResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: List<AlQuranSurahEditionData>?
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahEditionData(
    @Json(name = "number") val number: Int,
    @Json(name = "name") val name: String?,
    @Json(name = "englishName") val englishName: String?,
    @Json(name = "englishNameTranslation") val englishNameTranslation: String?,
    @Json(name = "numberOfAyahs") val numberOfAyahs: Int?,
    @Json(name = "ayahs") val ayahs: List<AlQuranAyahDto>?
)

@JsonClass(generateAdapter = true)
data class AlQuranAyahDto(
    @Json(name = "number") val number: Int,
    @Json(name = "text") val text: String,
    @Json(name = "numberInSurah") val numberInSurah: Int,
    @Json(name = "juz") val juz: Int?
)

interface AlQuranApi {
    @GET("v1/surah")
    suspend fun getSurahs(): AlQuranSurahListResponse

    @GET("v1/surah/{surahNumber}/editions/quran-uthmani,ur.jalandhry,en.sahih")
    suspend fun getSurahWithTranslations(
        @Path("surahNumber") surahNumber: Int
    ): AlQuranSurahEditionsResponse
}

// Quran.com API DTOs (Word-by-Word & Authentic Translations)
@JsonClass(generateAdapter = true)
data class QuranDotComVersesResponse(
    @Json(name = "verses") val verses: List<QuranDotComVerseDto>?
)

@JsonClass(generateAdapter = true)
data class QuranDotComVerseDto(
    @Json(name = "id") val id: Long,
    @Json(name = "verse_number") val verseNumber: Int,
    @Json(name = "verse_key") val verseKey: String?,
    @Json(name = "words") val words: List<QuranDotComWordDto>?,
    @Json(name = "translations") val translations: List<QuranDotComTranslationDto>?
)

@JsonClass(generateAdapter = true)
data class QuranDotComWordDto(
    @Json(name = "id") val id: Long,
    @Json(name = "position") val position: Int,
    @Json(name = "char_type_name") val charTypeName: String?,
    @Json(name = "text_uthmani") val textUthmani: String?,
    @Json(name = "text") val text: String?,
    @Json(name = "translation") val translation: QuranDotComWordTranslationDto?
)

@JsonClass(generateAdapter = true)
data class QuranDotComWordTranslationDto(
    @Json(name = "text") val text: String?,
    @Json(name = "language_name") val languageName: String?
)

@JsonClass(generateAdapter = true)
data class QuranDotComTranslationDto(
    @Json(name = "resource_id") val resourceId: Int?,
    @Json(name = "text") val text: String?
)

interface QuranDotComApi {
    @GET("api/v4/verses/by_chapter/{chapterId}")
    suspend fun getVersesByChapter(
        @Path("chapterId") chapterId: Int,
        @Query("language") language: String = "ur",
        @Query("words") words: Boolean = true,
        @Query("translations") translations: String = "234,131",
        @Query("word_fields") wordFields: String = "text_uthmani,translation",
        @Query("per_page") perPage: Int = 300
    ): QuranDotComVersesResponse
}

// Network Client Provider
object NetworkModule {
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    val alAdhanApi: AlAdhanApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AlAdhanApi::class.java)
    }

    val alQuranApi: AlQuranApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.alquran.cloud/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AlQuranApi::class.java)
    }

    val quranDotComApi: QuranDotComApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.quran.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(QuranDotComApi::class.java)
    }
}
