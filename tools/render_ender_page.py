import math
import random
import sys
from pathlib import Path

from PIL import Image

WIDTH = 300
HEIGHT = 200
LEFT_FRAME = (16, 22, 144, 182)
RIGHT_FRAME = (156, 22, 284, 182)

STYLES = {
    "default": {
        "ink": (22, 88, 84),
        "ink_soft": (64, 128, 120),
        "lilac": (176, 150, 214),
        "lilac_deep": (120, 88, 172),
        "city": (88, 60, 128),
        "fill": (196, 222, 214),
        "scene": True,
        "flourish": True,
    },
    "bare_bones": {
        "ink": (40, 96, 92),
        "ink_soft": (40, 96, 92),
        "lilac": (176, 150, 214),
        "lilac_deep": (120, 88, 172),
        "city": (88, 60, 128),
        "fill": (210, 226, 220),
        "scene": False,
        "flourish": False,
    },
}


class Canvas:
    def __init__(self):
        self.image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
        self.pixels = self.image.load()

    def put(self, x, y, color, alpha=255):
        if not (0 <= x < WIDTH and 0 <= y < HEIGHT) or alpha <= 0:
            return
        r, g, b, a = self.pixels[x, y]
        alpha = min(255, alpha)
        if a == 0:
            self.pixels[x, y] = color + (alpha,)
            return
        out = alpha + a * (255 - alpha) // 255
        mix = lambda c0, c1: (c1 * alpha + c0 * a * (255 - alpha) // 255) // max(out, 1)
        self.pixels[x, y] = (mix(r, color[0]), mix(g, color[1]), mix(b, color[2]), out)

    def hline(self, x0, x1, y, color, alpha=255):
        for x in range(x0, x1 + 1):
            self.put(x, y, color, alpha)

    def vline(self, x, y0, y1, color, alpha=255):
        for y in range(y0, y1 + 1):
            self.put(x, y, color, alpha)


def diamond(c, x, y, radius, color, filled=True, alpha=255):
    for dy in range(-radius, radius + 1):
        span = radius - abs(dy)
        if filled:
            c.hline(x - span, x + span, y + dy, color, alpha)
        else:
            c.put(x - span, y + dy, color, alpha)
            c.put(x + span, y + dy, color, alpha)


def sparkle(c, x, y, arm, color, alpha=255):
    c.put(x, y, color, alpha)
    for i in range(1, arm + 1):
        fade = alpha * (arm + 1 - i) // (arm + 1)
        for dx, dy in ((i, 0), (-i, 0), (0, i), (0, -i)):
            c.put(x + dx, y + dy, color, fade)
    for dx, dy in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        c.put(x + dx, y + dy, color, alpha // 3)


def curl(c, x, y, sx, sy, s):
    for px, py in ((0, 0), (1, 0), (2, 0), (3, 0), (0, 1), (0, 2), (0, 3), (2, 2), (3, 2), (3, 3), (2, 3)):
        c.put(x + px * sx, y + py * sy, s["ink"])
    c.put(x + 5 * sx, y + 1 * sy, s["ink_soft"])
    c.put(x + 1 * sx, y + 5 * sy, s["ink_soft"])


def frame(c, box, s):
    x0, y0, x1, y1 = box
    for inset, color in ((0, s["ink"]), (2, s["ink_soft"])):
        c.hline(x0 + inset + 4, x1 - inset - 4, y0 + inset, color)
        c.hline(x0 + inset + 4, x1 - inset - 4, y1 - inset, color)
        c.vline(x0 + inset, y0 + inset + 4, y1 - inset - 4, color)
        c.vline(x1 - inset, y0 + inset + 4, y1 - inset - 4, color)
    if s["flourish"]:
        curl(c, x0, y0, 1, 1, s)
        curl(c, x1, y0, -1, 1, s)
        curl(c, x0, y1, 1, -1, s)
        curl(c, x1, y1, -1, -1, s)
        mid = (y0 + y1) // 2
        diamond(c, x0 + 1, mid, 2, s["ink"])
        diamond(c, x1 - 1, mid, 2, s["ink"])
    else:
        for x, y in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
            diamond(c, x, y, 2, s["ink"])


def divider(c, x0, x1, y, s, center=True):
    middle = (x0 + x1) // 2
    for x in range(x0, x1 + 1):
        edge = min(x - x0, x1 - x)
        c.put(x, y, s["ink"], min(255, 60 + edge * 24))
    if center:
        diamond(c, middle, y, 2, s["ink"])
        c.put(middle - 4, y, s["ink"])
        c.put(middle + 4, y, s["ink"])


def rule(c, x0, x1, y, s):
    c.hline(x0, x1, y, s["ink"], 200)
    diamond(c, x1 + 2, y, 1, s["ink"])


def banner(c, x0, y0, x1, y1, s, swallowtail):
    c.hline(x0 + 2, x1 - 2, y0, s["ink"])
    c.hline(x0 + 2, x1 - 2, y1, s["ink"])
    for y in range(y0 + 1, y1):
        c.hline(x0 + 2, x1 - 2, y, s["fill"], 70)
    mid = (y0 + y1) // 2
    for y in range(y0, y1 + 1):
        offset = abs(y - mid) * 2 // max(1, (y1 - y0) // 2) if swallowtail else 0
        c.put(x0 + offset, y, s["ink"])
        c.put(x1 - offset, y, s["ink"])
    if s["flourish"]:
        c.put(x0 - 2, mid, s["ink_soft"])
        c.put(x1 + 2, mid, s["ink_soft"])
        sparkle(c, x0 - 6, mid, 2, s["ink"])
        sparkle(c, x1 + 6, mid, 2, s["ink"])


def scroll(c, x0, y0, x1, y1, s):
    banner(c, x0, y0, x1, y1, s, False)
    if s["flourish"]:
        for y in range(y0 + 1, y1):
            c.put(x0 + 1, y, s["ink_soft"])
        c.put(x1 - 1, y1 - 1, s["ink_soft"])
        c.put(x1 - 1, y1 - 2, s["ink_soft"])
        diamond(c, (x0 + x1) // 2, y1 + 2, 1, s["ink"])


def arrow_rule(c, x0, x1, y, s):
    c.hline(x0, x1, y, s["ink"], 200)
    for i in range(1, 3):
        c.put(x1 - i, y - i, s["ink"])
        c.put(x1 - i, y + i, s["ink"])
    c.put(x1 - 4, y, s["ink"])


def ink_drop(c, cx, top, s):
    for dy in range(0, 16):
        if dy < 7:
            half = dy * 0.55
        else:
            t = (dy - 11) / 5
            half = 5 * math.sqrt(max(0.0, 1 - t * t))
        for dx in range(-int(half), int(half) + 1):
            shade = 0.35 + 0.65 * (dy / 15)
            color = tuple(int(s["ink_soft"][i] * (1 - shade) + s["ink"][i] * shade) for i in range(3))
            c.put(cx + dx, top + dy, color, 230)
    c.put(cx - 2, top + 9, s["fill"], 220)
    c.put(cx - 2, top + 10, s["fill"], 220)
    c.put(cx - 1, top + 8, s["fill"], 160)
    for i in range(14):
        angle = math.pi * (0.9 + i / 9)
        c.put(int(cx + math.cos(angle) * 9), int(top + 10 + math.sin(angle) * 5), s["ink_soft"], 150)
    for i in range(10):
        angle = math.pi * (-0.1 + i / 12)
        c.put(int(cx + 2 + math.cos(angle) * 8), int(top + 12 + math.sin(angle) * 6), s["ink_soft"], 120)


def quill_flourish(c, cx, y, s):
    for i in range(9):
        c.put(cx - 4 + i, y + 4 - i // 2, s["ink"])
        if i > 2:
            c.put(cx - 4 + i, y + 3 - i // 2, s["ink_soft"])
    c.put(cx - 5, y + 5, s["ink"])
    for side in (-1, 1):
        for i in range(22):
            x = cx + side * (10 + i)
            wave = round(math.sin(i / 3.5) * 1.5)
            c.put(x, y + 2 + wave, s["ink_soft"], 220 - i * 6)
        curl_x = cx + side * 34
        c.put(curl_x, y + 1, s["ink_soft"], 160)
        c.put(curl_x + side, y, s["ink_soft"], 120)


def end_scene(c, s):
    rng = random.Random(0xE7D)
    cx, cy = 240, 128
    for ring, (rx, ry, start, end, width) in enumerate(((40, 24, 0.55, 1.9, 3), (30, 17, 1.1, 2.4, 2), (46, 30, 1.6, 2.6, 2), (22, 12, -0.3, 0.9, 2))):
        steps = int(rx * 6)
        for i in range(steps):
            t = start + (end - start) * i / steps
            for w in range(width):
                x = cx + math.cos(t * math.pi) * (rx + w)
                y = cy + math.sin(t * math.pi) * (ry + w) - (t - start) * 6
                fade = 1 - abs((i / steps) - 0.5) * 1.6
                c.put(int(x), int(y), s["lilac"], int(110 * max(0.15, fade)))
    sparkle(c, 238, 116, 7, s["lilac_deep"], 200)
    for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
        c.put(238 + dx, 116 + dy, (236, 226, 250), 230)
    for _ in range(26):
        x = rng.randint(196, 280)
        y = rng.randint(86, 168)
        c.put(x, y, s["lilac"], rng.randint(90, 180))
        if rng.random() < 0.25:
            sparkle(c, x, y, 1, s["lilac"], 120)
    island_top = 166
    for row in range(14):
        half = 22 - row * 3 // 2 - (row * row) // 12
        if half <= 0:
            break
        for x in range(258 - half, 258 + half + 1):
            jag = (x * 7 + row * 3) % 5 == 0 and row > 3
            if not jag:
                c.put(x, island_top + row, s["city"], max(20, 150 - row * 11))
    c.hline(236, 280, island_top, s["lilac_deep"], 170)
    towers = ((242, 4, 12), (250, 5, 22), (259, 4, 30), (266, 5, 18), (273, 3, 10))
    for x, width, height in towers:
        top = island_top - height
        for y in range(top + 3, island_top):
            for dx in range(width):
                fade = 150 if (y - top) < height - 4 else 110
                c.put(x + dx, y, s["city"], fade)
            if (y - top) % 6 == 0:
                c.put(x + width // 2, y, s["lilac"], 170)
        c.hline(x - 1, x + width, top + 2, s["lilac_deep"], 180)
        c.hline(x - 2, x + width + 1, top + 3, s["city"], 160)
        c.hline(x, x + width - 1, top + 1, s["city"], 150)
        c.vline(x + width // 2, top - 3, top, s["lilac_deep"], 160)
    for ix, iy, width in ((168, 140, 9), (184, 152, 7), (198, 132, 6)):
        for row in range(width // 2 + 2):
            c.hline(ix - width // 2 + row, ix + width // 2 - row, iy + row, s["city"], 110 - row * 8)
        c.hline(ix - width // 2, ix + width // 2, iy - 1, s["lilac"], 120)


def draw(style):
    s = STYLES[style]
    c = Canvas()
    frame(c, LEFT_FRAME, s)
    frame(c, RIGHT_FRAME, s)
    if s["scene"]:
        end_scene(c, s)
    for x0, x1 in ((LEFT_FRAME[0], LEFT_FRAME[2]), (RIGHT_FRAME[0], RIGHT_FRAME[2])):
        sparkle(c, x0 + 14, 33, 3, s["ink"])
        sparkle(c, x1 - 14, 33, 3, s["ink"])
        divider(c, x0 + 8, x1 - 8, 43, s)
    scroll(c, 22, 47, 140, 61, s)
    rule(c, 62, 136, 81, s)
    divider(c, 26, 134, 117, s, center=False)
    rule(c, 40, 136, 125, s)
    if s["flourish"]:
        ink_drop(c, 124, 128, s)
    banner(c, 30, 155, 132, 169, s, True)
    if s["flourish"]:
        quill_flourish(c, 81, 173, s)
    return c.image


def main():
    assets = Path(sys.argv[1])
    style = sys.argv[2] if len(sys.argv) > 2 else "default"
    gui = assets / "textures" / "gui"
    gui.mkdir(parents=True, exist_ok=True)
    draw(style).save(gui / "ender_page.png")


main()
