#!/usr/bin/env python3
"""
Builds the offline data files bundled with the app (assets + fonts) from open datasets
published on npm.  Run from the repository root:

    python3 tools/build_assets.py

Needs: python3 (3.9+), npm, network access to registry.npmjs.org.

Outputs
  app/src/main/assets/mushaf.txt   604 pages, 15 lines each, Madinah-mushaf line layout
  app/src/main/assets/meta.json    surah / juz / page index
  app/src/main/assets/adhkar.json  adhkar sections
  app/src/main/res/font/*.ttf      Amiri Quran (Quran text) + Tajawal (UI)

Sources (see NOTICE.md in the repo root for the licences)
  quran-cloud            Uthmani text (ayah level). Its README names QuranEnc as the source;
                         package.json says CC-BY-4.0 but LICENSE.txt/README say CC-BY-SA-4.0.
                         Provenance and licence are UNRESOLVED: read NOTICE.md before publishing.
  quran-qcf4             page / line / word layout of the Madinah mushaf       MIT (data only, no fonts used)
  quran-meta             juz boundaries                                       MIT
  @kazishariar/hisnul-muslim-data   Hisn al-Muslim adhkar                     CC-BY-4.0
  @expo-google-fonts/*   Amiri Quran, Tajawal                                 SIL OFL 1.1
"""
import collections
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets"
FONTS = ROOT / "app/src/main/res/font"
CACHE = ROOT / "tools/.cache"


def fetch(pkg: str) -> Path:
    """npm pack <pkg>, extract it and return the package directory (cached)."""
    safe = pkg.replace("/", "_").replace("@", "")
    out = CACHE / safe
    if (out / "package").exists():
        return out / "package"
    out.mkdir(parents=True, exist_ok=True)
    subprocess.run(["npm", "pack", pkg, "--silent"], cwd=out, check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    tgz = next(out.glob("*.tgz"))
    subprocess.run(["tar", "-xzf", str(tgz), "-C", str(out)], check=True)
    return out / "package"


# --------------------------------------------------------------------------- Quran

MARK_RE = re.compile("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u08F0-\u08F2]")
WEAK = set("اٱويىءأإؤئ")


def tokenize(text: str):
    """Split an Uthmani ayah into display words."""
    # thin space -> space, rub-el-hizb sign dropped (the layout draws it separately), then fix the open tanween marks
    text = fix_marks(text.replace("\u2009", " ").replace("\u06DE", ""))
    raw = [t for t in text.split(" ") if t]
    merged = []
    for t in raw:
        # The source sometimes leaves a stray space before combining marks: glue them back.
        if merged and MARK_RE.match(t[0]):
            merged[-1] += t
        else:
            merged.append(t)
    out = []
    for t in merged:
        if t.endswith("۩") and len(t) > 1:  # sajdah sign is its own "word" in the layout
            out.append(t[:-1])
            out.append("۩")
        else:
            out.append(t)
    return out


# This Uthmani text encodes the Madinah mushaf's "open" tanween with borrowed code points
# (fatha-with-two-dots, inverted damma, subscript alef).  Unicode has real code points for them
# (U+08F0..U+08F2) and Amiri Quran draws those, whereas it has no glyph at all for U+065E.
# The mapping was verified against the layout data: U+065E / U+0656 occur exactly as often as the
# dammatan / kasratan that the mushaf layout shows in the same places, and U+0657 is always followed by
# alef, alef-maksura, a waqf sign or the end of the word, i.e. it only ever marks open fathatan.
OPEN_TANWEEN = {"\u065E": "\u08F1", "\u0656": "\u08F2", "\u0657": "\u08F0"}


def fix_marks(text: str) -> str:
    for a, b in OPEN_TANWEEN.items():
        text = text.replace(a, b)
    return text


def skeleton(s: str) -> str:
    return "".join(c for c in s if not MARK_RE.match(c) and c not in WEAK and c != "ـ")


def build_quran():
    cloud = fetch("quran-cloud")
    qcf = fetch("quran-qcf4")
    meta_pkg = fetch("quran-meta")

    chapters = json.load(open(cloud / "dist/quran.json", encoding="utf-8"))
    verse_text = {}
    counts = {}
    for s in chapters:
        counts[s["id"]] = len(s["verses"])
        for v in s["verses"]:
            verse_text["%d:%d" % (s["id"], v["id"])] = v["text"]
    assert len(verse_text) == 6236, len(verse_text)

    # Load the layout, page by page.
    pages = []
    for i in range(1, 605):
        pages.append(json.load(open(qcf / "pages" / ("%03d.json" % i), encoding="utf-8")))

    # Collect layout words per verse (in reading order) so tokens can be aligned with them.
    layout_words = collections.defaultdict(list)  # verse_key -> [(page_idx, line_idx, item_idx, text)]
    for pi, pg in enumerate(pages):
        for li, ln in enumerate(pg["lines"]):
            for wi, w in enumerate(ln["words"]):
                if w["type"] == "word":
                    layout_words[w["verse_key"]].append((pi, li, wi, w["text"]))

    # token for every layout word:  (pi, li, wi) -> token string or "" (word folded into previous token)
    assign = {}
    report = []
    for key, text in verse_text.items():
        toks = tokenize(text)
        lw = layout_words[key]
        if len(toks) == len(lw):
            for tok, (pi, li, wi, _) in zip(toks, lw):
                assign[(pi, li, wi)] = tok
            continue
        # Rare cases (a handful of verses): the printed mushaf splits one written word in two.
        # Align by consonant skeleton length.
        j = 0
        for ti, tok in enumerate(toks):
            need = len(skeleton(tok))
            first = j
            acc = 0
            while j < len(lw) and (acc < need or j == first):
                acc += len(skeleton(lw[j][3]))
                j += 1
                if ti == len(toks) - 1:
                    continue
            if first < len(lw):
                assign[lw[first][:3]] = tok
                for extra in range(first + 1, j):
                    assign[lw[extra][:3]] = ""
        # layout words never reached stay unassigned (-> dropped)
        report.append((key, len(toks), len(lw)))
    print("verses aligned by skeleton:", report)

    last_ayah = {s: n for s, n in counts.items()}
    surah_page = {}
    new_start = []  # per page: first ayah that STARTS on that page
    out_pages = []
    for pi, pg in enumerate(pages):
        lines_out = []
        first_new = None
        for li, ln in enumerate(pg["lines"]):
            items = ln["words"]
            kinds = {w["type"] for w in items}
            if "surah_header" in kinds:
                sura = items[0]["sura"]
                surah_page.setdefault(sura, pi + 1)
                lines_out.append("H|%d" % sura)
                continue
            if "bismillah" in kinds:
                lines_out.append("B|%d" % items[0]["sura"])
                continue
            payload = []
            flags = ""
            start = None
            for wi, w in enumerate(items):
                if w["type"] == "quarter":
                    flags += "Q"
                    continue
                if w["type"] == "word":
                    sura, ayah = map(int, w["verse_key"].split(":"))
                    tok = assign.get((pi, li, wi), "")
                    if start is None:
                        start = (sura, ayah)
                    if w["position"] == 1 and first_new is None:
                        first_new = (sura, ayah)
                    if tok:
                        payload.append(tok)
                elif w["type"] == "end":
                    sura, ayah = map(int, w["verse_key"].split(":"))
                    if start is None:
                        start = (sura, ayah)
                    payload.append("}")
                    if ayah == last_ayah[sura]:
                        flags += "E"  # last ayah of the surah ends on this line
            if start is None:
                raise SystemExit("line without verse on page %d line %d" % (pi + 1, li + 1))
            lines_out.append("T|%d|%d|%s|%s" % (start[0], start[1], flags, " ".join(payload)))
        out_pages.append("\n".join(lines_out))
        new_start.append(first_new)

    # Pages that start no verse (none expected): inherit from the previous page.
    for i in range(len(new_start)):
        if new_start[i] is None:
            new_start[i] = new_start[i - 1]
            print("page without new verse start:", i + 1)

    ASSETS.mkdir(parents=True, exist_ok=True)
    (ASSETS / "mushaf.txt").write_text("\f".join(out_pages), encoding="utf-8")

    # ---- meta.json
    abs_to_pair = []
    for s in range(1, 115):
        for a in range(1, counts[s] + 1):
            abs_to_pair.append((s, a))
    hafs_js = next(meta_pkg.glob("dist/HafsLists-*.js")).read_text(encoding="utf-8")
    m = re.search(r"JuzList:\s*\[([^\]]*)\]", hafs_js)
    juz_abs = [int(x) for x in re.findall(r"\d+", m.group(1))][1:31]
    assert len(juz_abs) == 30 and juz_abs[0] == 1, juz_abs

    def page_of(s, a):
        best = 1
        for p, st in enumerate(new_start, start=1):
            if tuple(st) <= (s, a):
                best = p
            else:
                break
        return best

    juz = []
    for n in juz_abs:
        s, a = abs_to_pair[n - 1]
        juz.append({"s": s, "a": a, "p": page_of(s, a)})

    # sanity: a few juz starts every reader knows
    known = {2: (2, 142), 3: (2, 253), 4: (3, 93), 5: (4, 24), 10: (8, 41), 11: (9, 93), 15: (17, 1), 30: (78, 1)}
    for j, pair in known.items():
        got = (juz[j - 1]["s"], juz[j - 1]["a"])
        assert got == pair, ("juz", j, got, pair)

    sajdah = []
    for key, text in verse_text.items():
        if "۩" in text:
            s, a = map(int, key.split(":"))
            sajdah.append([s, a])
    sajdah.sort()
    assert len(sajdah) == 15, len(sajdah)

    surahs = []
    for s in chapters:
        surahs.append({
            "n": s["name"],
            "m": 1 if s["type"] == "meccan" else 0,
            "a": s["total_verses"],
            "p": surah_page[s["id"]],
        })
    meta = {
        "bismillah": tokenize(verse_text["1:1"]) and " ".join(tokenize(verse_text["1:1"])),
        "surahs": surahs,
        "juz": juz,
        "starts": [list(x) for x in new_start],
        "sajdah": sajdah,
    }
    (ASSETS / "meta.json").write_text(json.dumps(meta, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print("quran: %d pages, %d surahs written" % (len(out_pages), len(surahs)))


# --------------------------------------------------------------------------- Adhkar

NUM_WORDS = [
    ("مائة مرة", 100), ("مئة مرة", 100), ("ثلاثين مرة", 30), ("ثلاث وثلاثين", 33), ("أربع وثلاثين", 34),
    ("ثلاث مرات", 3), ("ثلاثاً", 3), ("ثلاثا", 3), ("مرتين", 2), ("مرة واحدة", 1), ("أربع مرات", 4),
    ("خمس مرات", 5), ("ست مرات", 6), ("سبع مرات", 7), ("عشر مرات", 10), ("مرة", 1),
]
SKIP_SECTIONS = {"المقدمة", "فضل الذكر"}


def clean_adhkar(t: str) -> str:
    t = t.replace("{", "﴿").replace("}", "﴾")
    t = t.replace("*", " ")
    t = re.sub(r"\s+", " ", t)
    t = re.sub(r"\s+([،.؛:!؟])", r"\1", t)
    t = re.sub(r"([،؛])(?=\S)", r"\1 ", t)
    return t.strip()


def build_adhkar():
    pkg = fetch("@kazishariar/hisnul-muslim-data")
    data = json.load(open(pkg / "data.json", encoding="utf-8"))["data"]
    sections = collections.OrderedDict()
    for it in data:
        sec = it["section"].strip()
        if sec in SKIP_SECTIONS:
            continue
        text = it["arabic"]
        count = 1
        note = ""
        # last parenthetical that mentions a repetition
        groups = list(re.finditer(r"\(([^()]*)\)", text))
        for g in reversed(groups):
            inner = g.group(1)
            if "مرة" in inner or "مرات" in inner or "مرتين" in inner:
                for word, n in NUM_WORDS:
                    if word in inner:
                        count = n
                        rest = inner.replace(word, "").strip(" ،:-")
                        note = clean_adhkar(rest)
                        break
                text = text[:g.start()] + text[g.end():]
                break
        text = clean_adhkar(text)
        if not text:
            continue
        entry = {"t": text}
        if count > 1:
            entry["c"] = count
        if note:
            entry["n"] = note
        sections.setdefault(sec, []).append(entry)
    out = {"sections": [{"t": k, "i": v} for k, v in sections.items()]}
    (ASSETS / "adhkar.json").write_text(json.dumps(out, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print("adhkar: %d sections, %d items" % (len(out["sections"]), sum(len(s["i"]) for s in out["sections"])))


# --------------------------------------------------------------------------- Fonts

def copy_fonts():
    FONTS.mkdir(parents=True, exist_ok=True)
    amiri = fetch("@expo-google-fonts/amiri-quran")
    taj = fetch("@expo-google-fonts/tajawal")
    shutil.copy(amiri / "400Regular/AmiriQuran_400Regular.ttf", FONTS / "amiri_quran.ttf")
    shutil.copy(taj / "200ExtraLight/Tajawal_200ExtraLight.ttf", FONTS / "tajawal_extralight.ttf")
    shutil.copy(taj / "400Regular/Tajawal_400Regular.ttf", FONTS / "tajawal_regular.ttf")
    shutil.copy(taj / "500Medium/Tajawal_500Medium.ttf", FONTS / "tajawal_medium.ttf")
    shutil.copy(taj / "700Bold/Tajawal_700Bold.ttf", FONTS / "tajawal_bold.ttf")
    print("fonts copied")


if __name__ == "__main__":
    CACHE.mkdir(parents=True, exist_ok=True)
    build_quran()
    build_adhkar()
    copy_fonts()
