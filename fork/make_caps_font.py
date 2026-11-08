#!/usr/bin/env python3
"""Make archivo_black_caps.ttf: Archivo Black whose lowercase letters draw the capitals.

ink's rule: Archivo Black is always UPPERCASE. Compose has no text-transform in a TextStyle, so
rather than uppercasing every heading's string at its call site (dozens of Arc files, merge
conflicts), the Ink theme uses this copy of the font. Only the cmap changes (each lowercase code
point points at its capital's glyph), plus the family name. OFL 1.1 allows modified versions;
Archivo Black declares no Reserved Font Name, and the copy is renamed anyway.

Run (needs fontTools):  python make_caps_font.py <archivo_black.ttf> <archivo_black_caps.ttf>
"""
import sys

from fontTools.ttLib import TTFont

src, dst = sys.argv[1], sys.argv[2]
font = TTFont(src)

changed = 0
for table in font["cmap"].tables:
    if not table.isUnicode():
        continue
    cmap = table.cmap
    for cp in list(cmap):
        up = chr(cp).upper()
        if len(up) == 1 and ord(up) != cp and ord(up) in cmap:
            cmap[cp] = cmap[ord(up)]
            changed += 1

name = font["name"]
for rec in name.names:
    if rec.nameID in (1, 4, 16):   # family, full name, typographic family
        rec.string = str(rec.toUnicode()).replace("Archivo Black", "Archivo Black Caps")
    elif rec.nameID == 6:          # PostScript name
        rec.string = str(rec.toUnicode()).replace("ArchivoBlack", "ArchivoBlackCaps")

font.save(dst)
print(f"{dst}: {changed} lowercase mappings pointed at capitals")
