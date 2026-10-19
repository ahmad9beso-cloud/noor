package app.noor.islamic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Rounded surface with a hairline border: the basic container of the app. */
@Composable
fun Modifier.noorCard(radius: Dp = 20.dp, color: Color = Noor.colors.surface): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(color)
        .border(1.dp, Noor.colors.line, shape)
}

@Composable
fun IconBtn(ic: Ic, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Noor.colors.text) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        NIcon(ic, tint = tint)
    }
}

/** Screen title row. [onBack] shows the back arrow (it points right because the app is right-to-left). */
@Composable
fun NoorTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconBtn(Ic.ChevronR, onBack)
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = NoorType.title, color = Noor.colors.text, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Noor.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceAlt)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) c.surface else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = NoorType.label,
                    color = if (on) c.primary else c.muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    val c = Noor.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NIcon(Ic.Search, tint = c.muted, size = 20.dp)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = NoorType.body.copy(color = c.text),
            cursorBrush = SolidColor(c.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(hint, style = NoorType.body, color = c.muted)
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onChange("") },
                contentAlignment = Alignment.Center,
            ) { NIcon(Ic.Close, tint = c.muted, size = 16.dp) }
        }
    }
}

/** Eight-pointed star badge holding a number: the classic marker of a surah. */
@Composable
fun OctagonBadge(label: String, size: Dp = 44.dp) {
    val c = Noor.colors
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val side = this.size.minDimension * 0.68f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val topLeft = Offset(center.x - side / 2f, center.y - side / 2f)
            val style = Stroke(width = 1.4.dp.toPx())
            drawRect(c.accent, topLeft = topLeft, size = Size(side, side), style = style)
            rotate(45f, center) {
                drawRect(c.accent, topLeft = topLeft, size = Size(side, side), style = style)
            }
        }
        Text(label, style = NoorType.small, color = c.primary, textAlign = TextAlign.Center)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Noor.colors
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = NoorType.label, color = c.onPrimary, textAlign = TextAlign.Center)
    }
}

@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Noor.colors
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.primarySoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = NoorType.label, color = c.primary, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = NoorType.label,
        color = Noor.colors.muted,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = NoorType.body, color = Noor.colors.muted, textAlign = TextAlign.Center)
    }
}

/** Arabic search: ignores vowel marks and the different forms of alef, teh marbuta and yeh. */
fun normalizeArabic(s: String): String {
    val sb = StringBuilder(s.length)
    for (ch in s.trim()) {
        val code = ch.code
        if (code in 0x064B..0x065F || code == 0x0670 || code == 0x0640) continue
        sb.append(
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                'ى' -> 'ي'
                'ة' -> 'ه'
                else -> ch
            },
        )
    }
    return sb.toString()
}
