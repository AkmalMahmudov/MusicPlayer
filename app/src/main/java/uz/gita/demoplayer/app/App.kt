package uz.gita.demoplayer.app

import android.app.Application
import timber.log.Timber
import uz.gita.demoplayer.BuildConfig

class App : Application() {
    companion object {
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}