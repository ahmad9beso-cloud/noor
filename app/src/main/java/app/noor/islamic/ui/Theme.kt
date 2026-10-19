package app.noor.islamic.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.noor.islamic.R
import app.noor.islamic.logic.ArabicDates

@Immutable
class NoorColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val accent: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val paper: Color,
    val ink: Color,
    val heroStart: Color,
    val heroEnd: Color,
)

val LightNoor = NoorColors(
    dark = false,
    bg = Color(0xFFF7F3EA),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEFE9DA),
    primary = Color(0xFF0F6B57),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFD9ECE4),
    accent = Color(0xFFB8893B),
    text = Color(0xFF1B2420),
    muted = Color(0xFF6B746F),
    line = Color(0xFFE4DDCC),
    paper = Color(0xFFFFFDF6),
    ink = Color(0xFF1B1B1B),
    heroStart = Color(0xFF14806A),
    heroEnd = Color(0xFF0A4A3B),
)

val DarkNoor = NoorColors(
    dark = true,
    bg = Color(0xFF0C1310),
    surface = Color(0xFF14201B),
    surfaceAlt = Color(0xFF1B2923),
    primary = Color(0xFF5CC4A4),
    onPrimary = Color(0xFF06231A),
    primarySoft = Color(0xFF1D3A31),
    accent = Color(0xFFD9B26A),
    text = Color(0xFFE9EFEB),
    muted = Color(0xFF97A59D),
    line = Color(0xFF263530),
    paper = Color(0xFF101916),
    ink = Color(0xFFE6ECE8),
    heroStart = Color(0xFF16584A),
    heroEnd = Color(0xFF0B2D26),
)

val LocalNoor = staticCompositionLocalOf { LightNoor }

/** true = show digits as ٠١٢٣, false = 0123. */
val LocalEastern = staticCompositionLocalOf { false }

object Noor {
    val colors: NoorColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNoor.current
}

val Tajawal = FontFamily(
    Font(R.font.tajawal_extralight, FontWeight.ExtraLight),
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
)

/** Amiri Quran: the Quran text and the adhkar. */
val Amiri = FontFamily(Font(R.font.amiri_quran, FontWeight.Normal))

object NoorType {
    val clock = TextStyle(fontFamily = Tajawal, fontWeight = FontWeight.ExtraLight, fontSize = 72.sp, lineHeight = 78.sp)
    val clockSeconds = TextStyle(fontFamily = Tajawal, fontWeight = FontWeight.ExtraLight, fontSize = 26.sp)
    val title = TextStyle(fontFamily = Tajawal, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp)
    val h2 = TextStyle(fontFamily = Tajawal, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp)
    val h3 = TextStyle(fontFamily = Tajawal, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = Tajawal, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp)
    val label = TextStyle(fontFamily = Tajawal, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    val small = TextStyle(fontFamily = Tajawal, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp)
    val surahName = TextStyle(fontFamily = Amiri, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 36.sp)
    val dhikr = TextStyle(fontFamily = Amiri, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 42.sp)
}

@Composable
fun NoorTheme(dark: Boolean, eastern: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkNoor else LightNoor
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, secondary = c.accent,
            background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
            outline = c.line,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, secondary = c.accent,
            background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
            outline = c.line,
        )
    }
    CompositionLocalProvider(LocalNoor provides c, LocalEastern provides eastern) {
        MaterialTheme(colorScheme = scheme) {
            Surface(color = c.bg, contentColor = c.text, content = content)
        }
    }
}

/** Converts the digits of [s] to Arabic-Indic when the user chose that style. */
@Composable
fun digits(s: String): String = if (LocalEastern.current) ArabicDates.toEastern(s) else s

@Composable
fun digits(n: Int): String = digits(n.toString())

/** Verse and page numbers of the Quran are always Arabic-Indic. */
fun quranDigits(n: Int): String = ArabicDates.toEastern(n.toString())
