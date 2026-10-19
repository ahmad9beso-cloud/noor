package app.noor.islamic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.noor.islamic.data.AdhkarSection
import app.noor.islamic.data.Dhikr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Sections shown as big tiles at the top of the list; matched by a word of their title. */
private val featured = listOf(
    "الصباح والمساء",
    "بعد السلام من الصلاة",
    "أذكار النوم",
    "الاستيقاظ",
    "دعاء الهم والحزن",
    "دعاء الكرب",
    "دعاء صلاة الاستخارة",
    "دعاء السفر",
)

@Composable
fun AdhkarScreen(nav: Nav) {
    val sections = LocalApp.current.data.adhkar ?: return
    val idx = nav.adhkar
    if (idx in sections.indices) {
        AdhkarReader(sections[idx], idx) { nav.adhkar = -1 }
    } else {
        AdhkarList(sections, nav)
    }
}

@Composable
private fun AdhkarList(sections: List<AdhkarSection>, nav: Nav) {
    val c = Noor.colors
    var query by rememberSaveable { mutableStateOf("") }
    val q = normalizeArabic(query)
    val shown = remember(q, sections) {
        sections.withIndex().filter { q.isEmpty() || normalizeArabic(it.value.title).contains(q) }
    }
    val feat = remember(sections) {
        featured.mapNotNull { k -> sections.indexOfFirst { it.title.contains(k) }.takeIf { it >= 0 } }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                NoorTopBar("الأذكار", onBack = null)
                SearchField(query, { query = it }, "ابحث في الأذكار")
                Spacer(Modifier.height(14.dp))
            }
        }
        if (q.isEmpty()) {
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (pair in feat.chunked(2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (i in pair) {
                                FeaturedTile(sections[i], Modifier.weight(1f)) { nav.openAdhkar(i) }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            item { SectionLabel("كل الأذكار", Modifier.padding(horizontal = 16.dp)) }
        }
        if (shown.isEmpty()) item { EmptyHint("لا نتائج لهذا البحث") }
        items(shown, key = { it.index }) { iv ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { nav.openAdhkar(iv.index) }
                    .padding(horizontal = 20.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    iv.value.title,
                    style = NoorType.body,
                    color = c.text,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(12.dp))
                Text(digits(iv.value.items.size), style = NoorType.small, color = c.muted)
                Spacer(Modifier.width(8.dp))
                NIcon(Ic.ChevronL, tint = c.muted, size = 18.dp)
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = c.line)
        }
    }
}

@Composable
private fun FeaturedTile(s: AdhkarSection, modifier: Modifier, onClick: () -> Unit) {
    val c = Noor.colors
    Column(
        modifier
            .noorCard(18.dp)
            .clickable(onClick = onClick)
            .height(92.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            s.title.removePrefix("الأذكار ").removePrefix("أذكار ").removePrefix("دعاء "),
            style = NoorType.h3,
            color = c.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (s.items.size == 1) "ذكر واحد" else digits(s.items.size) + " ذكراً",
            style = NoorType.small,
            color = c.primary,
        )
    }
}

@Composable
private fun AdhkarReader(section: AdhkarSection, index: Int, onBack: () -> Unit) {
    val c = Noor.colors
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val left = remember(index) {
        mutableStateListOf<Int>().apply { section.items.forEach { add(it.count) } }
    }
    val total = left.size
    val done = left.count { it == 0 }
    val frac by animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, label = "progress")

    fun tap(i: Int) {
        if (left[i] > 0) {
            left[i] = left[i] - 1
            if (left[i] == 0) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                val next = (i + 1 until total).firstOrNull { left[it] > 0 }
                if (next != null) scope.launch {
                    delay(280)
                    list.animateScrollToItem(next)
                }
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else {
            // tapping a finished item again takes it back
            left[i] = section.items[i].count
        }
    }

    fun reset() {
        for (k in left.indices) left[k] = section.items[k].count
        scope.launch { list.animateScrollToItem(0) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBtn(Ic.ChevronR, onBack)
            Column(Modifier.weight(1f)) {
                Text(section.title, style = NoorType.h2, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (total > 1) {
                    Text("${digits(done)} من ${digits(total)}", style = NoorType.small, color = c.muted)
                }
            }
            if (done > 0) {
                Box(
                    Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.primarySoft)
                        .clickable { reset() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("إعادة", style = NoorType.label, color = c.primary)
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(c.line),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .background(c.primary),
            )
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(section.items) { i, d ->
                DhikrCard(d, left[i]) { tap(i) }
            }
            if (total > 0 && done == total) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        NIcon(Ic.Check, tint = c.primary, size = 28.dp)
                        Spacer(Modifier.height(6.dp))
                        Text("تقبّل الله منا ومنكم", style = NoorType.surahName, color = c.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DhikrCard(d: Dhikr, left: Int, onTap: () -> Unit) {
    val c = Noor.colors
    val finished = left == 0
    val fade by animateFloatAsState(if (finished) 0.5f else 1f, label = "fade")
    Column(
        Modifier
            .fillMaxWidth()
            .alpha(fade)
            .noorCard(18.dp)
            .clickable(onClick = onTap)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        if (d.note.isNotEmpty()) {
            Text(d.note, style = NoorType.small, color = c.accent)
            Spacer(Modifier.height(8.dp))
        }
        Text(d.text, style = NoorType.dhikr, color = c.text)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (d.count > 1) "يُقال " + digits(d.count) + " مرات" else "مرة واحدة",
                style = NoorType.small,
                color = c.muted,
                modifier = Modifier.weight(1f),
            )
            CounterRing(left, d.count)
        }
    }
}

@Composable
private fun CounterRing(left: Int, total: Int) {
    val c = Noor.colors
    val progress = if (total <= 0) 1f else (total - left).toFloat() / total
    val anim by animateFloatAsState(progress, label = "ring")
    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2f
            val box = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = c.line,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = box,
                style = Stroke(stroke),
            )
            drawArc(
                color = c.primary,
                startAngle = -90f,
                sweepAngle = 360f * anim,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = box,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        if (left == 0) {
            NIcon(Ic.Check, tint = c.primary)
        } else {
            Text(digits(left), style = NoorType.h2, color = c.primary, textAlign = TextAlign.Center)
        }
    }
}
