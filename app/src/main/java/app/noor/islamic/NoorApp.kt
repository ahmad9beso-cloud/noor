package app.noor.islamic

import android.app.Application
import app.noor.islamic.data.AppData
import app.noor.islamic.data.Prefs
import app.noor.islamic.notify.Notifier
import app.noor.islamic.notify.PrayerScheduler

class NoorApp : Application() {
    lateinit var prefs: Prefs
        private set

    val data = AppData()

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        Notifier.createChannel(this)
        PrayerScheduler.scheduleNext(this)
    }
}
