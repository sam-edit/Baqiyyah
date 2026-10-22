package com.example.domain.model

data class RecentAudioSurah(
    val surahNumber: Int,
    val surahEnglishName: String,
    val surahArabicName: String,
    val surahUrduName: String = "",
    val reciterId: String,
    val reciterName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L
)
