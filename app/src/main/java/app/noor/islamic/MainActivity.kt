package app.noor.islamic

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.noor.islamic.ui.AdhkarScreen
import app.noor.islamic.ui.CityPickerScreen
import app.noor.islamic.ui.HomeScreen
import app.noor.islamic.ui.Ic
import app.noor.islamic.ui.LocalApp
import app.noor.islamic.ui.NIcon
import app.noor.islamic.ui.Nav
import app.noor.islamic.ui.Noor
import app.noor.islamic.ui.NoorTheme
import app.noor.islamic.ui.NoorType
import app.noor.islamic.ui.QiblaScreen
import app.noor.islamic.ui.QuranIndexScreen
import app.noor.islamic.ui.QuranReader
import app.noor.islamic.ui.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as NoorApp
        setContent { NoorRoot(app, this) }
    }
}

@Composable
private fun NoorRoot(app: NoorApp, activity: ComponentActivity) {
    val prefs = app.prefs
    val systemDark = isSystemInDarkTheme()
    val dark = when (prefs.themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }

    // The app has its own light/dark switch, so the system bars follow the app's theme, not the phone's.
    DisposableEffect(dark) {
        val style = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose { }
    }

    // The Quran and the adhkar are read from the assets once, off the main thread.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { app.data.load(app.assets) }
    }

    val nav = remember { Nav() }
    // The whole app is Arabic, so it is always laid out right-to-left whatever the phone's language is.
    CompositionLocalProvider(
        LocalApp provides app,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        NoorTheme(dark = dark, eastern = prefs.easternDigits) {
            if (app.data.quran == null || app.data.adhkar == null) {
                Splash()
            } else {
                Shell(nav)
            }
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(Noor.colors.bg), contentAlignment = Alignment.Center) {
        Text("نور", style = NoorType.title, color = Noor.colors.primary)
    }
}

@Composable
private fun Shell(nav: Nav) {
    val c = Noor.colors
    // Registered before the screens, so a screen that handles Back itself (the reader's verse bar) wins.
    BackHandler(enabled = nav.canGoBack()) { nav.back() }

    val inReader = nav.tab == 1 && nav.quranPage != 0
    val showBar = nav.overlay == 0 && !inReader
    val screen = when {
        nav.overlay != 0 -> "overlay" + nav.overlay
        inReader -> "reader"
        else -> "tab" + nav.tab
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        Box(Modifier.weight(1f)) {
            Crossfade(targetState = screen, animationSpec = tween(160), label = "screen") { key ->
                when (key) {
                    "overlay1" -> SettingsScreen(nav)
                    "overlay2" -> CityPickerScreen(nav)
                    "reader" -> QuranReader(nav.quranPage) { nav.quranPage = 0 }
                    "tab1" -> QuranIndexScreen(nav)
                    "tab2" -> AdhkarScreen(nav)
                    "tab3" -> QiblaScreen(nav)
                    else -> HomeScreen(nav)
                }
            }
        }
        if (showBar) BottomBar(nav)
    }
}

private val tabs = listOf(
    Ic.Home to "الرئيسية",
    Ic.Quran to "القرآن",
    Ic.Adhkar to "الأذكار",
    Ic.Compass to "القبلة",
)

@Composable
private fun BottomBar(nav: Nav) {
    val c = Noor.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
        ) {
            tabs.forEachIndexed { i, (icon, label) ->
                val on = nav.tab == i
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable {
                            if (i == 2 && nav.tab == 2) nav.adhkar = -1
                            if (i == 1 && nav.tab == 1) nav.quranPage = 0
                            nav.tab = i
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .width(56.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(if (on) c.primarySoft else androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        NIcon(icon, tint = if (on) c.primary else c.muted)
                    }
                    Text(
                        label,
                        style = NoorType.small,
                        color = if (on) c.primary else c.muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
