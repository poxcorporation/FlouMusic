package com.flou.music.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.flou.music.R

object ThemeManager {
    fun applyTheme(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        when (prefs.getString("theme_mode", "system")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun getColorTheme(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return when (prefs.getString("app_color", "purple")) {
            "blue" -> R.style.Theme_FlouMusic_Blue
            "green" -> R.style.Theme_FlouMusic_Green
            "red" -> R.style.Theme_FlouMusic_Red
            "orange" -> R.style.Theme_FlouMusic_Orange
            "pink" -> R.style.Theme_FlouMusic_Pink
            "teal" -> R.style.Theme_FlouMusic_Teal
            else -> R.style.Theme_FlouMusic_Purple
        }
    }
}