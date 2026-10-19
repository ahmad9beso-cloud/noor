package app.noor.islamic.data

import android.content.res.AssetManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** One printed word, or an end-of-ayah marker when [marker] is true (then [text] is empty). */
class MWord(val text: String, val ayah: Int, val marker: Boolean)

/** One of the 15 printed lines of a mushaf page. */
class MLine(val kind: Int, val sura: Int, val centered: Boolean, val words: List<MWord>) {
    companion object {
        const val TEXT = 0
        const val HEADER = 1
        const val BASMALA = 2
    }
}

class MPage(val lines: List<MLine>) {
    val firstSura: Int get() = lines.first().sura
}

class SurahInfo(val name: String, val meccan: Boolean, val ayahs: Int, val page: Int)

class JuzInfo(val sura: Int, val ayah: Int, val page: Int)

/** The whole Quran in the Madinah mushaf layout (604 pages), read from the bundled assets. */
class Quran private constructor(
    private val raw: List<String>,
    val surahs: List<SurahInfo>,
    val juz: List<JuzInfo>,
    private val startKeys: IntArray,
    val bismillah: String,
) {
    private val cache = object : LinkedHashMap<Int, MPage>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, MPage>?): Boolean = size > 14
    }

    fun page(n: Int): MPage = synchronized(cache) {
        cache.getOrPut(n) { parse(raw[n - 1]) }
    }

    /** First ayah that begins on [page], as (sura, ayah). */
    fun pageStart(page: Int): Pair<Int, Int> {
        val k = startKeys[page - 1]
        return Pair(k / 1000, k % 1000)
    }

    /** Page on which the given ayah begins. */
    fun pageOf(sura: Int, ayah: Int): Int {
        val key = sura * 1000 + ayah
        var lo = 0
        var hi = startKeys.size - 1
        var ans = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (startKeys[mid] <= key) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return ans + 1
    }

    fun juzOfPage(page: Int): Int {
        var j = 1
        for (i in juz.indices) if (juz[i].page <= page) j = i + 1
        return j
    }

    private fun parse(text: String): MPage {
        val lines = ArrayList<MLine>(15)
        for (ln in text.split('\n')) {
            if (ln.length < 3) continue
            when (ln[0]) {
                'H' -> lines.add(MLine(MLine.HEADER, ln.substring(2).toInt(), true, emptyList()))
                'B' -> lines.add(MLine(MLine.BASMALA, ln.substring(2).toInt(), true, emptyList()))
                else -> {
                    val f = ln.split('|', limit = 5)
                    val sura = f[1].toInt()
                    var ayah = f[2].toInt()
                    val words = ArrayList<MWord>()
                    for (tok in f[4].split(' ')) {
                        if (tok.isEmpty()) continue
                        if (tok == "}") {
                            words.add(MWord("", ayah, true))
                            ayah++
                        } else {
                            words.add(MWord(tok, ayah, false))
                        }
                    }
                    lines.add(MLine(MLine.TEXT, sura, f[3].contains('E'), words))
                }
            }
        }
        return MPage(lines)
    }

    companion object {
        const val PAGES = 604

        fun load(assets: AssetManager): Quran {
            val mushaf = assets.open("mushaf.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val raw = mushaf.split('\u000c')
            val meta = JSONObject(assets.open("meta.json").bufferedReader(Charsets.UTF_8).use { it.readText() })

            val sj = meta.getJSONArray("surahs")
            val surahs = List(sj.length()) { i ->
                val o = sj.getJSONObject(i)
                SurahInfo(o.getString("n"), o.getInt("m") == 1, o.getInt("a"), o.getInt("p"))
            }
            val jj = meta.getJSONArray("juz")
            val juz = List(jj.length()) { i ->
                val o = jj.getJSONObject(i)
                JuzInfo(o.getInt("s"), o.getInt("a"), o.getInt("p"))
            }
            val st = meta.getJSONArray("starts")
            val keys = IntArray(st.length()) { i ->
                val a = st.getJSONArray(i)
                a.getInt(0) * 1000 + a.getInt(1)
            }
            return Quran(raw, surahs, juz, keys, meta.getString("bismillah"))
        }
    }
}

class Dhikr(val text: String, val count: Int, val note: String)

class AdhkarSection(val title: String, val items: List<Dhikr>)

fun loadAdhkar(assets: AssetManager): List<AdhkarSection> {
    val text = assets.open("adhkar.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
    val arr = JSONObject(text).getJSONArray("sections")
    return List(arr.length()) { i ->
        val s = arr.getJSONObject(i)
        val items = s.getJSONArray("i")
        AdhkarSection(
            s.getString("t"),
            List(items.length()) { j ->
                val d = items.getJSONObject(j)
                Dhikr(d.getString("t"), d.optInt("c", 1), d.optString("n", ""))
            },
        )
    }
}

/** Holds the data loaded from assets; filled once on a background thread at start-up. */
class AppData {
    var quran by mutableStateOf<Quran?>(null)
        private set
    var adhkar by mutableStateOf<List<AdhkarSection>?>(null)
        private set

    fun load(assets: AssetManager) {
        if (quran != null) return
        val q = Quran.load(assets)
        val a = loadAdhkar(assets)
        quran = q
        adhkar = a
    }
}
