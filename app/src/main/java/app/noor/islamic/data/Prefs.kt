package app.noor.islamic.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.noor.islamic.logic.Method

class Place(val name: String, val lat: Double, val lon: Double)

/** A saved reading mark. [ayah] == 0 means the whole surah. */
class Bookmark(val sura: Int, val ayah: Int, val time: Long)

/**
 * All user settings. Every value is Compose state, so screens update by themselves,
 * and every change is written straight to SharedPreferences.
 */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("noor", Context.MODE_PRIVATE)

    /** 0 = follow the system, 1 = light, 2 = dark. */
    var themeMode by mutableIntStateOf(sp.getInt("theme", 0))
        private set

    /** Use the Arabic-Indic digits (٠١٢٣) in the interface. Quran verse numbers always use them. */
    var easternDigits by mutableStateOf(sp.getBoolean("east", false))
        private set

    /** 0 = follow the system, 1 = 12 hour, 2 = 24 hour. */
    var clockFormat by mutableIntStateOf(sp.getInt("clock", 0))
        private set

    /** true = page flipping, false = continuous scrolling. */
    var readerPaged by mutableStateOf(sp.getBoolean("paged", true))
        private set

    /** Last page the user was reading (0 = never opened the Quran). */
    var lastPage by mutableIntStateOf(sp.getInt("lastPage", 0))
        private set

    var method by mutableIntStateOf(sp.getInt("method", 0))
        private set

    var hanafi by mutableStateOf(sp.getBoolean("hanafi", false))
        private set

    /** Days added to the Umm al-Qura Hijri date to follow local moon sighting (-2..2). */
    var hijriAdjust by mutableIntStateOf(sp.getInt("hijriAdj", 0))
        private set

    /** Bit mask of the prayers that show a notification (see Prayer.bit). */
    var notifyMask by mutableIntStateOf(sp.getInt("notify", 0))
        private set

    var place by mutableStateOf(loadPlace())
        private set

    var bookmarks by mutableStateOf(loadBookmarks())
        private set

    val methodEnum: Method get() = Method.entries.getOrElse(method) { Method.MWL }

    fun setTheme(v: Int) {
        themeMode = v
        sp.edit().putInt("theme", v).apply()
    }

    fun setEastern(v: Boolean) {
        easternDigits = v
        sp.edit().putBoolean("east", v).apply()
    }

    fun setClockFormat(v: Int) {
        clockFormat = v
        sp.edit().putInt("clock", v).apply()
    }

    fun setPaged(v: Boolean) {
        readerPaged = v
        sp.edit().putBoolean("paged", v).apply()
    }

    fun setLastPage(v: Int) {
        if (v == lastPage) return
        lastPage = v
        sp.edit().putInt("lastPage", v).apply()
    }

    fun setMethod(v: Int) {
        method = v
        sp.edit().putInt("method", v).apply()
    }

    fun setHanafi(v: Boolean) {
        hanafi = v
        sp.edit().putBoolean("hanafi", v).apply()
    }

    fun setHijriAdjust(v: Int) {
        hijriAdjust = v.coerceIn(-2, 2)
        sp.edit().putInt("hijriAdj", hijriAdjust).apply()
    }

    fun setNotifyMask(v: Int) {
        notifyMask = v
        sp.edit().putInt("notify", v).apply()
    }

    fun setPlace(p: Place?) {
        place = p
        val e = sp.edit()
        if (p == null) {
            e.remove("placeName").remove("placeLat").remove("placeLon")
        } else {
            e.putString("placeName", p.name).putString("placeLat", p.lat.toString()).putString("placeLon", p.lon.toString())
        }
        e.apply()
    }

    fun hasBookmark(sura: Int, ayah: Int): Boolean = bookmarks.any { it.sura == sura && it.ayah == ayah }

    fun toggleBookmark(sura: Int, ayah: Int) {
        val list = if (hasBookmark(sura, ayah)) {
            bookmarks.filterNot { it.sura == sura && it.ayah == ayah }
        } else {
            listOf(Bookmark(sura, ayah, System.currentTimeMillis())) + bookmarks
        }
        bookmarks = list
        sp.edit().putString("bookmarks", list.joinToString(",") { "${it.sura}:${it.ayah}:${it.time}" }).apply()
    }

    private fun loadPlace(): Place? {
        val lat = sp.getString("placeLat", null)?.toDoubleOrNull() ?: return null
        val lon = sp.getString("placeLon", null)?.toDoubleOrNull() ?: return null
        return Place(sp.getString("placeName", "") ?: "", lat, lon)
    }

    private fun loadBookmarks(): List<Bookmark> {
        val raw = sp.getString("bookmarks", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(',').mapNotNull { item ->
            val f = item.split(':')
            if (f.size != 3) return@mapNotNull null
            val s = f[0].toIntOrNull() ?: return@mapNotNull null
            val a = f[1].toIntOrNull() ?: return@mapNotNull null
            val t = f[2].toLongOrNull() ?: 0L
            Bookmark(s, a, t)
        }
    }
}
