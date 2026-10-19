package app.noor.islamic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.noor.islamic.data.Quran
import app.noor.islamic.data.SurahInfo

@Composable
fun QuranIndexScreen(nav: Nav) {
    val app = LocalApp.current
    val prefs = app.prefs
    val quran = app.data.quran ?: return
    val c = Noor.colors

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = remember(query) {
        val q = normalizeArabic(query)
        val num = query.trim().toIntOrNull()
        quran.surahs.withIndex().filter { (i, s) ->
            q.isEmpty() || normalizeArabic(s.name).contains(q) || (num != null && num == i + 1)
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                NoorTopBar("القرآن الكريم", onBack = null, modifier = Modifier.padding(horizontal = 0.dp))
                if (prefs.lastPage > 0) {
                    ContinueCard(quran, prefs.lastPage) { nav.openQuran(prefs.lastPage) }
                    Spacer(Modifier.height(14.dp))
                }
                if (tab == 0) {
                    SearchField(query, { query = it }, "ابحث عن سورة بالاسم أو الرقم")
                    Spacer(Modifier.height(12.dp))
                }
                SegmentedControl(listOf("السور", "الأجزاء", "العلامات"), tab, { tab = it })
                Spacer(Modifier.height(8.dp))
            }
        }
        when (tab) {
            0 -> {
                if (filtered.isEmpty()) item { EmptyHint("لا توجد سورة بهذا الاسم") }
                items(filtered, key = { it.index }) { iv ->
                    SurahRow(iv.index + 1, iv.value, nav)
                }
            }
            1 -> {
                items(quran.juz.size, key = { it }) { i ->
                    val j = quran.juz[i]
                    val surah = quran.surahs[j.sura - 1]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { nav.openQuran(j.page) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OctagonBadge(digits(i + 1))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الجزء " + digits(i + 1), style = NoorType.h3, color = c.text)
                            Text(
                                "يبدأ من سورة ${surah.name} · الآية ${digits(j.ayah)}",
                                style = NoorType.small,
                                color = c.muted,
                            )
                        }
                        Text("ص " + quranDigits(j.page), style = NoorType.small, color = c.muted)
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = c.line)
                }
            }
            else -> {
                if (prefs.bookmarks.isEmpty()) {
                    item {
                        EmptyHint("لا توجد علامات بعد.\nاضغط على أي آية أثناء القراءة، أو على أيقونة العلامة بجانب السورة.")
                    }
                }
                items(prefs.bookmarks, key = { "${it.sura}:${it.ayah}" }) { b ->
                    val surah = quran.surahs[b.sura - 1]
                    val page = if (b.ayah == 0) surah.page else quran.pageOf(b.sura, b.ayah)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { nav.openQuran(page) }
                            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (b.ayah == 0) "سورة ${surah.name}" else "سورة ${surah.name} · الآية ${digits(b.ayah)}",
                                style = NoorType.h3,
                                color = c.text,
                            )
                            Text("صفحة " + quranDigits(page), style = NoorType.small, color = c.muted)
                        }
                        IconBtn(Ic.BookmarkFill, onClick = { prefs.toggleBookmark(b.sura, b.ayah) }, tint = c.accent)
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = c.line)
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(quran: Quran, page: Int, onClick: () -> Unit) {
    val c = Noor.colors
    val surah = remember(page) { quran.surahs[quran.page(page).firstSura - 1] }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(c.heroStart, c.heroEnd)))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("تابع القراءة", style = NoorType.small, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f))
            Text(
                "سورة ${surah.name}",
                style = NoorType.surahName,
                color = androidx.compose.ui.graphics.Color.White,
            )
            Text(
                "صفحة " + quranDigits(page),
                style = NoorType.small,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
            )
        }
        NIcon(Ic.ChevronL, tint = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
private fun SurahRow(number: Int, s: SurahInfo, nav: Nav) {
    val prefs = LocalApp.current.prefs
    val c = Noor.colors
    val marked = prefs.hasBookmark(number, 0)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { nav.openQuran(s.page) }
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OctagonBadge(digits(number))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, style = NoorType.surahName, color = c.text)
            Text(
                (if (s.meccan) "مكية" else "مدنية") + " · " + digits(s.ayahs) + " آية",
                style = NoorType.small,
                color = c.muted,
                textAlign = TextAlign.Start,
            )
        }
        Text("ص " + quranDigits(s.page), style = NoorType.small, color = c.muted)
        IconBtn(
            if (marked) Ic.BookmarkFill else Ic.Bookmark,
            onClick = { prefs.toggleBookmark(number, 0) },
            tint = if (marked) c.accent else c.muted,
        )
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = c.line)
}
