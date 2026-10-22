package com.example

import android.app.Application
import com.example.audio.QuranAudioStreamingCacheManager
import com.example.ui.theme.AlDeenFontRegistry

class AlDeenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AlDeenFontRegistry.init(this)
        QuranAudioStreamingCacheManager.init(this)
    }
}
