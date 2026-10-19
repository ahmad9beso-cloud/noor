# Notices and licences

Read this file before publishing the app anywhere public (a store, a public release, a shared APK).
Two items below are **unresolved** and need a decision from the project owner.

## ⚠ 1. Source and licence of the Quran text — UNRESOLVED

`app/src/main/assets/mushaf.txt` contains the Quran text. It is built by `tools/build_assets.py` from the npm
package **`quran-cloud` 1.0.0** (<https://github.com/saikothasan/quran-cloud>), and the lines/words are placed
using the layout data of **`quran-qcf4`** (see 2).

What the package itself says, and where it disagrees with itself:

| Where | Says |
|---|---|
| `README.md` | The Uthmani text comes from *The Noble Qur'an Encyclopedia* (quranenc.com); licence **CC BY-SA 4.0** |
| `LICENSE.txt` | CC BY-SA 4.0 |
| `package.json` | `"license": "CC-BY-4.0"` |

The script style (alef wasla ٱ, small sukun ۡ, open tanween code points) looks like the Tanzil "Uthmani" text, but
that was **not verified**; do not rely on it.

Before a public release:

1. Settle the true origin of the text and its licence. If it is CC BY-SA, give attribution and publish the derived
   data file (`mushaf.txt`) under the same licence. A cleaner option is to rebuild `mushaf.txt` from a text whose
   terms are unambiguous (for example the official Tanzil download, honouring its verbatim-copy terms).
2. **Have the text proofread by a qualified person** against a printed Madinah mushaf. Nobody has done this yet.
   The data was only checked by program (6,236 verse endings, word counts per verse, layout alignment), which
   does not replace a human review of a sacred text.

### Changes the build script makes to the text (disclosed on purpose)

* The "open tanween" marks are re-encoded: U+065E → U+08F1, U+0656 → U+08F2, U+0657 → U+08F0. The bundled font
  (Amiri Quran) has no glyph for U+065E; the new code points draw the same marks. Letters are not changed.
* Thin spaces become normal spaces; the rub-el-hizb sign (U+06DE) is dropped because the layout draws its own
  marker; a stray space before a combining mark is closed up.
* In the rare verses where the printed mushaf splits one written word over two lines, the word is kept whole.

## 2. Mushaf layout (which words sit on which line of which page)

`quran-qcf4` 1.1.0 — the JSON data files are **MIT** (© 2026 Mohamad Hajj Rabee). Only the page/line/word
structure is used. The QCF4 **font files in that package are not used and not bundled**: their licence forbids
redistribution without permission from the King Fahd Complex.

## 3. Fonts (bundled in `app/src/main/res/font`)

* **Amiri Quran** — SIL Open Font License 1.1 (© The Amiri Project Authors)
* **Tajawal** — SIL Open Font License 1.1 (© The Tajawal Project Authors)

Obtained through `@expo-google-fonts/*` (MIT packaging of the Google Fonts files).

## 4. Adhkar

`app/src/main/assets/adhkar.json` — from the npm package `@kazishariar/hisnul-muslim-data` 1.0.3, which republishes
*Hisn al-Muslim* (Sa'id bin Ali bin Wahf al-Qahtani). Licence declared: **CC BY 4.0**. Attribution is shown in the
app's *About* section. The text was cleaned automatically (repetition counts moved into a separate field, extra
whitespace removed); it has not been proofread either. Some entries keep the book's own wording such as
"if he wakes in the morning…" as a note above the dhikr.

## 5. Code libraries

AndroidX / Jetpack Compose / Kotlin coroutines — Apache License 2.0.
