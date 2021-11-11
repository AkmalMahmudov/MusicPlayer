package uz.gita.demoplayer.data.local

import android.content.Context

class LocalStorage(context: Context) {

    private val pref = context.getSharedPreferences("LocalStorage", Context.MODE_PRIVATE)

    var isPlaying: Boolean by BooleanPreference(pref, false)
    var lastPlayedPosition: Int by IntPreference(pref, 0)
    var lastPlayedDuration: Int by IntPreference(pref, 0)
}