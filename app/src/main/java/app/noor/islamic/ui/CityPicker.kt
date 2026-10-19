package app.noor.islamic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.noor.islamic.data.Cities
import app.noor.islamic.data.Place
import app.noor.islamic.notify.PrayerScheduler

/** Pick a built-in city: works without any permission or internet. */
@Composable
fun CityPickerScreen(nav: Nav) {
    val c = Noor.colors
    val ctx = LocalContext.current
    val prefs = LocalApp.current.prefs
    var query by rememberSaveable { mutableStateOf("") }
    val q = normalizeArabic(query)
    val cities = remember(q) { Cities.all.filter { q.isEmpty() || normalizeArabic(it.name).contains(q) } }
    val locate = rememberLocateAction { ok -> if (ok) nav.back() }
    val current = prefs.place

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        NoorTopBar("اختيار المدينة", onBack = { nav.back() })
        Column(Modifier.padding(horizontal = 16.dp)) {
            SearchField(query, { query = it }, "ابحث عن مدينة")
            Spacer(Modifier.height(10.dp))
            SoftButton("استخدام موقعي الحالي", onClick = locate, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            if (cities.isEmpty()) item { EmptyHint("لا توجد مدينة بهذا الاسم") }
            items(cities, key = { it.name }) { city ->
                val selected = current != null && current.name == city.name
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            prefs.setPlace(Place(city.name, city.lat, city.lon))
                            PrayerScheduler.scheduleNext(ctx)
                            nav.back()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        city.name,
                        style = NoorType.body,
                        color = if (selected) c.primary else c.text,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) NIcon(Ic.Check, tint = c.primary, size = 20.dp)
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = c.line)
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
