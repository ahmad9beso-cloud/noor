package app.noor.islamic.ui

import android.content.Context
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import app.noor.islamic.R
import app.noor.islamic.data.MLine
import app.noor.islamic.data.Quran
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Measures how wide a word is in Amiri Quran, using the same shaping engine that draws it. */
class WordMeasurer(context: Context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.amiri_quran)
        textSize = 200f
    }
    private val cache = HashMap<String, Float>()

    /** Width of [text] in em (1.0 = the font size). */
    fun em(text: String): Float = cache.getOrPut(text) { paint.measureText(text) / 200f }
}

private var sharedMeasurer: WordMeasurer? = null

/** One measurer for the whole app, so the font is loaded once and every page reuses the cached widths. */
@Composable
private fun rememberWordMeasurer(): WordMeasurer {
    val ctx = LocalContext.current.applicationContext
    return sharedMeasurer ?: WordMeasurer(ctx).also { sharedMeasurer = it }
}

private class LinePlan(val sizeDp: Float, val justify: Boolean, val gapDp: Float)

/**
 * Decides how one printed line is drawn: every line has the same base size, a line that is too long
 * shrinks to fit, and a shorter line is spread over the full width (or centred when it is much shorter,
 * like the last line of a surah).
 */
private fun planLine(line: MLine, special: Boolean, widthDp: Float, baseDp: Float, m: WordMeasurer): LinePlan {
    val space = m.em(" ")
    var natural = 0f
    for (w in line.words) natural += if (w.marker) 1f else m.em(w.text)
    natural += (line.words.size - 1).coerceAtLeast(0) * space
    val target = if (special) widthDp * 0.78f else widthDp
    val cap = if (special) baseDp * 1.25f else baseDp
    val size = min(cap, target / (natural * 1.012f))
    val centered = line.centered || special
    val used = natural * size / widthDp
    val justify = !(centered && used < 0.78f)
    return LinePlan(size, justify, size * space)
}

/** Lays the words of one line out right-to-left, spreading the free space between them when [justify]. */
@Composable
private fun JustifiedLine(modifier: Modifier, justify: Boolean, gapPx: Float, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val width = constraints.maxWidth
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else (placeables.maxOfOrNull { it.height } ?: 0)
        var total = 0
        for (p in placeables) total += p.width
        val n = placeables.size
        val gap = if (justify && n > 1) max(0f, (width - total).toFloat() / (n - 1)) else gapPx
        val block = total + gap * (n - 1).coerceAtLeast(0)
        val startRight = if (justify) width.toFloat() else (width + block) / 2f
        layout(width, height) {
            var right = startRight
            for (p in placeables) {
                p.place((right - p.width).roundToInt(), (height - p.height) / 2)
                right -= p.width + gap
            }
        }
    }
}

@Composable
private fun WordBox(text: String, sizeSp: TextUnit, lineH: Dp, bg: Color, ink: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .height(lineH)
            .background(bg, RoundedCornerShape(6.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextStyle(fontFamily = Amiri, fontSize = sizeSp, color = ink),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentHeight(Alignment.CenterVertically, unbounded = true),
        )
    }
}

@Composable
private fun AyahMarker(ayah: Int, sizeDp: Float, lineH: Dp, hit: Boolean, onClick: () -> Unit) {
    val c = Noor.colors
    val label = quranDigits(ayah)
    val factor = if (label.length >= 3) 0.34f else if (label.length == 2) 0.44f else 0.5f
    val fontSize = with(LocalDensity.current) { (sizeDp * factor).dp.toSp() }
    Box(
        Modifier
            .height(lineH)
            .width(sizeDp.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size((sizeDp * 0.9f).dp)
                .clip(CircleShape)
                .background(if (hit) c.accent.copy(alpha = 0.35f) else Color.Transparent)
                .border(1.2.dp, c.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = TextStyle(fontFamily = Tajawal, fontWeight = FontWeight.Medium, fontSize = fontSize, color = c.primary),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.wrapContentHeight(Alignment.CenterVertically, unbounded = true),
            )
        }
    }
}

@Composable
private fun SurahBanner(name: String, width: Dp, lineH: Dp, baseDp: Float) {
    val c = Noor.colors
    val shape = RoundedCornerShape(10.dp)
    val fontSize = with(LocalDensity.current) { (baseDp * 1.2f).dp.toSp() }
    Box(
        Modifier
            .width(width)
            .height(lineH)
            .padding(vertical = lineH * 0.09f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(c.accent.copy(alpha = 0.10f))
                .border(1.5.dp, c.accent, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "سُورَةُ $name",
                style = TextStyle(fontFamily = Amiri, fontSize = fontSize, color = c.primary),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.wrapContentHeight(Alignment.CenterVertically, unbounded = true),
            )
        }
    }
}

/**
 * One page of the mushaf. In [paged] mode it fills the screen and spreads its 15 lines over the height;
 * otherwise it has a fixed height so pages can be stacked in a scrolling list.
 * [selected] is sura * 1000 + ayah of the highlighted verse (0 = none).
 */
@Composable
fun MushafPage(
    quran: Quran,
    pageNo: Int,
    paged: Boolean,
    selected: Int,
    onTapAyah: (sura: Int, ayah: Int) -> Unit,
    onBackgroundTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Noor.colors
    val density = LocalDensity.current
    val measurer = rememberWordMeasurer()
    val page = remember(pageNo) { quran.page(pageNo) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBackgroundTap() },
    ) {
        val sidePad = 16.dp
        val width = maxWidth - sidePad * 2
        val widthDp = width.value
        val baseDp = widthDp / 18f
        val headerH = 26.dp
        val footerH = 32.dp
        val minLine = (baseDp * 1.9f).dp
        val lineH: Dp = if (paged) {
            val fit = (maxHeight - topInset - bottomInset - headerH - footerH) / 15f
            if (fit < minLine) minLine else fit
        } else {
            (baseDp * 2.35f).dp
        }
        val special = pageNo <= 2
        val plans = remember(pageNo, widthDp) {
            page.lines.map { if (it.kind == MLine.TEXT) planLine(it, special, widthDp, baseDp, measurer) else null }
        }
        val juz = quran.juzOfPage(pageNo)
        val surahName = quran.surahs[page.firstSura - 1].name

        Column(
            if (paged) Modifier.fillMaxSize().padding(top = topInset, bottom = bottomInset) else Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(headerH)
                    .padding(horizontal = sidePad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(surahName, style = NoorType.small, color = c.muted)
                Spacer(Modifier.weight(1f))
                Text("الجزء " + quranDigits(juz), style = NoorType.small, color = c.muted)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .then(if (paged) Modifier.weight(1f) else Modifier.height(lineH * 15)),
                contentAlignment = Alignment.Center,
            ) {
                Column(Modifier.width(width)) {
                    page.lines.forEachIndexed { i, line ->
                        when (line.kind) {
                            MLine.HEADER -> SurahBanner(quran.surahs[line.sura - 1].name, width, lineH, baseDp)
                            MLine.BASMALA -> Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(lineH),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    quran.bismillah,
                                    style = TextStyle(
                                        fontFamily = Amiri,
                                        fontSize = with(density) { (baseDp * 1.05f).dp.toSp() },
                                        color = c.ink,
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.wrapContentHeight(Alignment.CenterVertically, unbounded = true),
                                )
                            }
                            else -> {
                                val plan = plans[i]
                                if (plan != null) {
                                    val sizeSp = with(density) { plan.sizeDp.dp.toSp() }
                                    val gapPx = with(density) { plan.gapDp.dp.toPx() }
                                    JustifiedLine(Modifier.width(width).height(lineH), plan.justify, gapPx) {
                                        for (word in line.words) {
                                            val hit = selected == line.sura * 1000 + word.ayah
                                            val bg = if (hit) c.accent.copy(alpha = 0.26f) else Color.Transparent
                                            if (word.marker) {
                                                AyahMarker(word.ayah, plan.sizeDp, lineH, hit) { onTapAyah(line.sura, word.ayah) }
                                            } else {
                                                WordBox(word.text, sizeSp, lineH, bg, c.ink) { onTapAyah(line.sura, word.ayah) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(footerH),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    quranDigits(pageNo),
                    style = NoorType.label,
                    color = c.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
fun QuranReader(startPage: Int, onClose: () -> Unit) {
    val app = LocalApp.current
    val prefs = app.prefs
    val quran = app.data.quran ?: return
    val c = Noor.colors

    var page by remember { mutableIntStateOf(startPage.coerceIn(1, Quran.PAGES)) }
    var bars by remember { mutableStateOf(true) }
    var selected by remember { mutableIntStateOf(0) }
    val paged = prefs.readerPaged

    BackHandler(enabled = selected != 0) { selected = 0 }

    val onTapAyah: (Int, Int) -> Unit = { s, a ->
        val key = s * 1000 + a
        selected = if (selected == key) 0 else key
    }
    val onBackgroundTap: () -> Unit = {
        if (selected != 0) selected = 0 else bars = !bars
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.paper),
    ) {
        if (paged) {
            val state = rememberPagerState(initialPage = page - 1) { Quran.PAGES }
            LaunchedEffect(state) {
                snapshotFlow { state.currentPage }.collect {
                    page = it + 1
                    prefs.setLastPage(it + 1)
                }
            }
            HorizontalPager(state = state, modifier = Modifier.fillMaxSize(), key = { it }) { p ->
                MushafPage(quran, p + 1, true, selected, onTapAyah, onBackgroundTap, Modifier.fillMaxSize())
            }
        } else {
            val state = rememberLazyListState(initialFirstVisibleItemIndex = page - 1)
            LaunchedEffect(state) {
                snapshotFlow { state.firstVisibleItemIndex }.collect {
                    page = it + 1
                    prefs.setLastPage(it + 1)
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                state = state,
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
                ),
            ) {
                items(Quran.PAGES, key = { it }) { i ->
                    MushafPage(quran, i + 1, false, selected, onTapAyah, onBackgroundTap)
                }
            }
        }

        AnimatedVisibility(
            visible = bars,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            ReaderTopBar(quran, page, paged, onClose)
        }

        AnimatedVisibility(
            visible = selected != 0,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            AyahBar(quran, selected) { selected = 0 }
        }
    }
}

@Composable
private fun ReaderTopBar(quran: Quran, page: Int, paged: Boolean, onClose: () -> Unit) {
    val prefs = LocalApp.current.prefs
    val c = Noor.colors
    val surah = quran.surahs[quran.page(page).firstSura - 1]
    val start = quran.pageStart(page)
    val marked = prefs.hasBookmark(start.first, start.second)
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surface.copy(alpha = 0.96f))
            .border(1.dp, c.line)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(Ic.ChevronR, onClose)
        Column(Modifier.weight(1f)) {
            Text("سورة ${surah.name}", style = NoorType.h3, color = c.text)
            Text(
                "الجزء ${quranDigits(quran.juzOfPage(page))} · صفحة ${quranDigits(page)}",
                style = NoorType.small,
                color = c.muted,
            )
        }
        IconBtn(
            if (marked) Ic.BookmarkFill else Ic.Bookmark,
            onClick = { prefs.toggleBookmark(start.first, start.second) },
            tint = if (marked) c.accent else c.text,
        )
        IconBtn(if (paged) Ic.Scroll else Ic.Pages, onClick = { prefs.setPaged(!paged) })
    }
}

@Composable
private fun AyahBar(quran: Quran, selected: Int, onClose: () -> Unit) {
    val prefs = LocalApp.current.prefs
    val c = Noor.colors
    val sura = selected / 1000
    val ayah = selected % 1000
    if (sura < 1 || sura > quran.surahs.size) return
    val marked = prefs.hasBookmark(sura, ayah)
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(20.dp))
            .padding(start = 18.dp, end = 6.dp)
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("سورة ${quran.surahs[sura - 1].name}", style = NoorType.h3, color = c.text)
            Text("الآية ${quranDigits(ayah)}", style = NoorType.small, color = c.muted)
        }
        IconBtn(
            if (marked) Ic.BookmarkFill else Ic.Bookmark,
            onClick = { prefs.toggleBookmark(sura, ayah) },
            tint = if (marked) c.accent else c.text,
        )
        IconBtn(Ic.Close, onClick = onClose, tint = c.muted)
    }
}
