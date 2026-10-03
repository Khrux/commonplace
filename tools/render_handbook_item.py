import sys
from pathlib import Path

from PIL import Image

SIZE = 16
INK = {
    "outline": (30, 17, 8),
    "page": (243, 232, 204),
    "page_line": (212, 194, 158),
    "page_shade": (176, 152, 118),
    "corner": (230, 190, 98),
    "corner_dark": (156, 112, 46),
    "quill": (252, 251, 245),
    "vane": (218, 212, 200),
    "vane_dark": (160, 152, 140),
    "nib": (58, 46, 38),
    "ink": (34, 34, 56),
}
STYLES = {
    "default": {"highlight": 255, "cover": 222, "panel": 196, "shade": 170, "dark": 128, "edge": 92, "grain": True},
    "bare_bones": {"highlight": 222, "cover": 222, "panel": 222, "shade": 176, "dark": 150, "edge": 110, "grain": False},
}
SHAFT = [(5, 9), (6, 8), (7, 7), (8, 6), (9, 5), (10, 4), (11, 3), (12, 2), (13, 1), (14, 0)]


def upper(x):
    return 7 - x // 2 if x <= 8 else 3 + (x - 7) // 2


def lower(x):
    return 7 + (x + 1) // 2 if x <= 7 else 11 - (x - 7) // 2


def draw(style):
    s = STYLES[style]
    cover = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    overlay = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))

    def tint(x, y, key):
        cover.putpixel((x, y), (s[key], s[key], s[key], 255))

    def ink(x, y, key):
        overlay.putpixel((x, y), INK[key] + (255,))
        cover.putpixel((x, y), (0, 0, 0, 0))

    for x in range(SIZE):
        top, bottom = upper(x), lower(x)
        for y in range(top, bottom + 1):
            if y == top:
                ink(x, y, "outline")
            elif y == bottom:
                tint(x, y, "edge")
            elif y == top + 1 and x <= 9:
                tint(x, y, "highlight")
            elif y == bottom - 1 and x >= 7:
                tint(x, y, "shade")
            elif s["grain"] and (y == top + 2 or y == bottom - 2) and 3 <= x <= 12:
                tint(x, y, "panel")
            else:
                tint(x, y, "cover")
        if x <= 7:
            ink(x, bottom + 1, "page")
            ink(x, bottom + 2, "page_line" if x % 2 == 0 else "page")
            if x == 7:
                ink(x, bottom + 1, "page_shade")
                ink(x, bottom + 2, "page_shade")
            tint(x, bottom + 3, "dark")
        else:
            tint(x, bottom + 1, "shade")
            tint(x, bottom + 2, "dark" if x % 3 == 1 else "shade")
            tint(x, bottom + 3, "dark")
        if bottom + 4 < SIZE:
            ink(x, bottom + 4, "outline")
    for y in range(upper(0), lower(0) + 4):
        ink(0, y, "outline")
    for y in range(upper(15), lower(15) + 4):
        ink(15, y, "outline")
    for x, y in ((1, 7), (1, 8), (8, 4), (7, 4), (14, 7), (14, 6)):
        ink(x, y, "corner")
    for x, y in ((1, 9), (13, 7)):
        ink(x, y, "corner_dark")
    for x, y in SHAFT[1:]:
        if y >= upper(x + 1) and y < lower(x + 1):
            tint(x + 1, y, "dark")
    for i, (x, y) in enumerate(SHAFT):
        if i == 0:
            ink(x, y, "nib")
        else:
            ink(x, y, "quill" if i < 6 else "vane")
        if i >= 4:
            ink(x - 1, y, "quill" if i % 2 == 0 else "vane")
        if i >= 6 and y - 1 >= 0:
            ink(x - 1, y - 1, "vane_dark" if i % 2 else "vane")
    ink(4, 10, "ink")
    return cover, overlay


def main():
    assets = Path(sys.argv[1])
    style = sys.argv[2] if len(sys.argv) > 2 else "default"
    items = assets / "textures" / "item"
    items.mkdir(parents=True, exist_ok=True)
    cover, overlay = draw(style)
    cover.save(items / "commonplace.png")
    overlay.save(items / "handbook_overlay.png")


main()
