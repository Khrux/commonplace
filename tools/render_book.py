import math
import random
import sys
from pathlib import Path

from PIL import Image

CANVAS_WIDTH = 300
CANVAS_HEIGHT = 200
BOOK_LEFT = 3
BOOK_TOP = 12
WIDTH = 295
HEIGHT = 180
PAGE_TOP = 6
PAGE_BOTTOM = HEIGHT - 6
LEFT_PAGE = (7, 147)
RIGHT_PAGE = (147, WIDTH - 7)

STYLES = {
    "default": {
        "cover": (104, 52, 30),
        "cover_dark": (58, 27, 14),
        "cover_light": (140, 78, 44),
        "stitch": (196, 150, 96),
        "corner": (201, 162, 74),
        "corner_dark": (128, 94, 36),
        "page": (241, 226, 190),
        "page_dark": (214, 189, 140),
        "page_edge": (196, 166, 116),
        "page_burn": (186, 140, 80),
        "page_line": (134, 92, 52),
        "tab": (236, 222, 190),
        "tab_dark": (170, 140, 96),
        "ink": (70, 44, 28),
        "noise": True,
    },
    "bare_bones": {
        "cover": (112, 64, 40),
        "cover_dark": (72, 38, 22),
        "cover_light": (112, 64, 40),
        "stitch": (112, 64, 40),
        "corner": (112, 64, 40),
        "corner_dark": (72, 38, 22),
        "page": (246, 238, 220),
        "page_dark": (230, 218, 192),
        "page_edge": (214, 198, 166),
        "tab": (238, 228, 204),
        "tab_dark": (150, 118, 84),
        "ink": (72, 48, 34),
        "noise": False,
    },
}


ENDER_PAGES = {
    "default": {"page": (178, 176, 170), "page_dark": (146, 143, 136), "page_edge": (128, 124, 116), "page_burn": (98, 94, 88), "page_line": (60, 56, 52), "tab": (170, 168, 162), "tab_dark": (92, 88, 82)},
    "bare_bones": {"page": (184, 182, 176), "page_dark": (166, 164, 158), "page_edge": (150, 148, 142), "tab": (176, 174, 168), "tab_dark": (104, 100, 94)},
}


def smooth_noise(seed, width, height, scale):
    rng = random.Random(seed)
    grid_w = width // scale + 2
    grid_h = height // scale + 2
    grid = [[rng.random() for _ in range(grid_w)] for _ in range(grid_h)]
    values = []
    for y in range(height):
        row = []
        for x in range(width):
            gx, gy = x / scale, y / scale
            x0, y0 = int(gx), int(gy)
            fx, fy = gx - x0, gy - y0
            fx = fx * fx * (3 - 2 * fx)
            fy = fy * fy * (3 - 2 * fy)
            top = grid[y0][x0] * (1 - fx) + grid[y0][x0 + 1] * fx
            bottom = grid[y0 + 1][x0] * (1 - fx) + grid[y0 + 1][x0 + 1] * fx
            row.append(top * (1 - fy) + bottom * fy)
        values.append(row)
    return values


def weathering(style):
    field = [[0.0] * WIDTH for _ in range(HEIGHT)]
    if not style["noise"]:
        return field
    rng = random.Random(7)
    for _ in range(6):
        cx, cy = rng.uniform(15, WIDTH - 15), rng.uniform(15, HEIGHT - 15)
        radius = rng.uniform(5, 14)
        ring = rng.random() < 0.4
        for y in range(max(0, int(cy - radius - 2)), min(HEIGHT, int(cy + radius + 3))):
            for x in range(max(0, int(cx - radius - 2)), min(WIDTH, int(cx + radius + 3))):
                d = math.hypot(x - cx, (y - cy) * 1.2)
                if ring:
                    field[y][x] += max(0.0, 0.35 - abs(d - radius) * 0.25)
                else:
                    field[y][x] += max(0.0, (radius - d) / radius) * 0.22
    for _ in range(90):
        x, y = rng.randrange(WIDTH), rng.randrange(HEIGHT)
        field[y][x] += rng.uniform(0.25, 0.55)
    return field


def streaks(seed):
    rng = random.Random(seed)
    field = [[0.0] * WIDTH for _ in range(HEIGHT)]
    for _ in range(26):
        cx = rng.uniform(0, WIDTH)
        cy = rng.uniform(0, HEIGHT)
        half_width = rng.uniform(3, 9)
        half_height = rng.uniform(25, 70)
        strength = rng.uniform(-0.35, 0.45)
        for y in range(max(0, int(cy - half_height)), min(HEIGHT, int(cy + half_height))):
            for x in range(max(0, int(cx - half_width)), min(WIDTH, int(cx + half_width))):
                d = ((x - cx) / half_width) ** 2 + ((y - cy) / half_height) ** 2
                if d < 1:
                    field[y][x] += (1 - d) ** 2 * strength
    return field


def draw_weathered_pages(s, pixels, grain, mottle):
    stains = weathering(s)
    bands = streaks(11)
    wobble = smooth_noise(5, WIDTH, HEIGHT, 4)
    along_x = [round(v * 2.4) for v in smooth_noise(6, WIDTH, 1, 5)[0]]
    along_y = [round(v * 2.4) for v in smooth_noise(8, HEIGHT, 1, 5)[0]]
    along_bottom = [round(v * 2.4) for v in smooth_noise(9, WIDTH, 1, 5)[0]]
    for left, right in (LEFT_PAGE, RIGHT_PAGE):
        first = left == LEFT_PAGE[0]
        gutter_side = right if first else left
        outer_side = left if first else right - 1
        for y in range(PAGE_TOP, PAGE_BOTTOM):
            for x in range(left, right):
                gutter = abs(x - gutter_side)
                rim = min(abs(x - outer_side), y - PAGE_TOP, PAGE_BOTTOM - 1 - y)
                ragged = rim + (wobble[y][x] - 0.5) * 3.5
                shade = (mottle[y][x] - 0.5) * 0.45 + (grain[y][x] - 0.5) * 0.12 + bands[y][x]
                shade += stains[y][x]
                if ragged < 10:
                    shade += ((10 - ragged) / 10) ** 1.7 * 1.15
                if gutter < 14:
                    shade += ((14 - gutter) / 14) ** 1.6 * 1.1
                inset = (abs(x - outer_side) - along_y[y], y - PAGE_TOP - along_x[x], PAGE_BOTTOM - 1 - y - along_bottom[x])
                if min(inset) < 0:
                    color = s["page_edge"] + (255,)
                elif min(inset) == 0:
                    color = s["page_line"] + (255,)
                elif shade <= 1:
                    color = mix(s["page"], s["page_dark"], max(0.0, shade))
                else:
                    color = mix(s["page_dark"], s["page_burn"], min(1.0, (shade - 1) * 0.8))
                pixels[x, y] = color
        corner_x = right - 1 if not first else left
        for y in range(PAGE_BOTTOM - 10, PAGE_BOTTOM):
            for x in range(left, right):
                dx = abs(x - corner_x)
                dy = PAGE_BOTTOM - 1 - y
                if first or dx + dy > 9:
                    continue
                if dx + dy < 9 and dx + dy > 1:
                    flap = dx + dy == 8 or dx == 0 or dy == 0
                    pixels[x, y] = (s["page_line"] if flap else s["page_dark"]) + (255,)
                if dx + dy == 9:
                    pixels[x, y] = s["page_line"] + (255,)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def draw_book(style):
    s = STYLES[style]
    image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    pixels = image.load()
    grain = smooth_noise(1, WIDTH, HEIGHT, 3) if s["noise"] else None
    mottle = smooth_noise(2, WIDTH, HEIGHT, 22) if s["noise"] else None
    if mottle:
        fine = smooth_noise(3, WIDTH, HEIGHT, 7)
        mottle = [[mottle[y][x] * 0.7 + fine[y][x] * 0.3 for x in range(WIDTH)] for y in range(HEIGHT)]
    for y in range(HEIGHT):
        for x in range(WIDTH):
            edge = x in (0, WIDTH - 1) or y in (0, HEIGHT - 1)
            if edge:
                pixels[x, y] = s["cover_dark"] + (255,)
                continue
            t = (grain[y][x] - 0.5) * 0.5 + 0.5 if grain else 0.5
            pixels[x, y] = mix(s["cover_dark"], s["cover_light"], 0.35 + t * 0.4) if s["noise"] else s["cover"] + (255,)
    for x in (0, 1, WIDTH - 2, WIDTH - 1):
        for y in (0, 1, HEIGHT - 2, HEIGHT - 1):
            pixels[x, y] = (0, 0, 0, 0)
    for x in range(1, WIDTH - 1):
        pixels[x, 1] = s["cover_dark"] + (255,) if x in (1, WIDTH - 2) else pixels[x, 1]
    if s["noise"]:
        for x in range(4, WIDTH - 4, 3):
            for y in (3, HEIGHT - 4):
                pixels[x, y] = s["stitch"] + (255,)
                pixels[x + 1, y] = s["stitch"] + (255,)
        for y in range(4, HEIGHT - 4, 3):
            for x in (3, WIDTH - 4):
                pixels[x, y] = s["stitch"] + (255,)
                pixels[x, y + 1] = s["stitch"] + (255,)
    if s["noise"]:
        draw_weathered_pages(s, pixels, grain, mottle)
    else:
        for left, right in (LEFT_PAGE, RIGHT_PAGE):
            gutter_side = right if left == LEFT_PAGE[0] else left
            for y in range(PAGE_TOP, PAGE_BOTTOM):
                for x in range(left, right):
                    pixels[x, y] = mix(s["page"], s["page_dark"], 0.6 if abs(x - gutter_side) < 2 else 0.0)
    for y in range(PAGE_TOP - 2, PAGE_BOTTOM + 2):
        pixels[LEFT_PAGE[1] - 1, y] = s["page_edge"] + (255,)
        pixels[RIGHT_PAGE[0], y] = s["page_edge"] + (255,)
    for y in (PAGE_TOP - 2, PAGE_TOP - 1, PAGE_BOTTOM, PAGE_BOTTOM + 1):
        pixels[LEFT_PAGE[1] - 1, y] = s["cover_dark"] + (255,)
        pixels[RIGHT_PAGE[0], y] = s["cover_dark"] + (255,)
    if s["noise"]:
        for cx, cy in ((0, 0), (WIDTH - 9, 0), (0, HEIGHT - 9), (WIDTH - 9, HEIGHT - 9)):
            for y in range(9):
                for x in range(9):
                    inner_x = x if cx == 0 else 8 - x
                    inner_y = y if cy == 0 else 8 - y
                    if inner_x + inner_y > 9 or (cx + x) in (0, WIDTH - 1) and (cy + y) in (0, HEIGHT - 1):
                        continue
                    border = inner_x + inner_y == 9 or inner_x == 0 or inner_y == 0
                    pixels[cx + x, cy + y] = (s["corner_dark"] if border else s["corner"]) + (255,)
    return image


DEFAULT_COLOR = (140, 59, 38)


def luminance(color):
    return color[0] * 0.299 + color[1] * 0.587 + color[2] * 0.114


def draw_cover_mask(style):
    book = draw_book(style)
    probe = dict(STYLES[style])
    for key in ("cover", "cover_dark", "cover_light"):
        probe[key] = tuple(min(255, value + 37) for value in probe[key])
    STYLES["probe"] = probe
    shifted = draw_book("probe")
    del STYLES["probe"]
    mask = Image.new("RGBA", book.size, (0, 0, 0, 0))
    base = luminance(DEFAULT_COLOR)
    for y in range(book.size[1]):
        for x in range(book.size[0]):
            pixel = book.getpixel((x, y))
            if pixel[3] and pixel != shifted.getpixel((x, y)):
                grey = min(255, round(255 * luminance(pixel) / base))
                mask.putpixel((x, y), (grey, grey, grey, 255))
    return mask


def draw_tab(style, width, height, selected, margin=6):
    s = STYLES[style]
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    pixels = image.load()
    out = height - margin - (1 if selected else 0)
    tucked = mix(s["tab"], s["tab_dark"], 0.45)
    for y in range(height):
        for x in range(width):
            rounded = y == 0 and x in (0, width - 1)
            if rounded:
                continue
            on_page = selected and y >= height - 1
            outline = (y == 0 or x in (0, width - 1)) and not on_page
            if outline:
                color = s["tab_dark"] + (255,)
            elif selected:
                color = s["page"] + (255,)
            elif y >= out:
                color = tucked
            elif y == out - 1 and s["noise"]:
                color = mix(s["tab"], s["tab_dark"], 0.2)
            else:
                color = s["tab"] + (255,)
            pixels[x, y] = color
    if selected:
        for y in range(out, height - 1):
            for x in (1, width - 2):
                pixels[x, y] = mix(s["page"], s["tab_dark"], 0.3)
    return image


def draw_side_tab(style, width, height, selected):
    top = draw_tab(style, height, width, selected, 7)
    return top.transpose(Image.Transpose.ROTATE_90)


def draw_arrow(style):
    s = STYLES[style]
    image = Image.new("RGBA", (22, 15), (0, 0, 0, 0))
    pixels = image.load()
    for y in range(15):
        for x in range(22):
            head = x >= 13 and abs(y - 7) <= 21 - x
            shaft = x < 14 and 5 <= y <= 9
            if head or shaft:
                pixels[x, y] = INK_SOFT + (255,)
    return image


def draw_icon(style, name):
    s = STYLES[style]
    image = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    pixels = image.load()
    ink = s["ink"] + (255,)
    if name == "pen":
        for i in range(9):
            pixels[2 + i, 9 - i] = ink
            pixels[3 + i, 9 - i] = ink
        pixels[1, 10] = ink
    elif name == "eraser":
        for y in range(4, 10):
            for x in range(1, 11):
                if abs((x - 1) - (9 - y)) < 6:
                    pixels[x, y] = s["page_edge"] + (255,) if x > 5 else ink
    elif name == "text":
        for x in range(2, 10):
            pixels[x, 2] = ink
        for y in range(2, 11):
            pixels[5, y] = ink
            pixels[6, y] = ink
    return image


INK = (112, 70, 35)
INK_SOFT = (175, 140, 91)
INK_MID = (137, 94, 59)
PAPER_BOX = (238, 227, 199)
PAPER_SHADE = (224, 208, 174)
PAPER_DEEP = (214, 196, 158)


def box(width, height, outline, fill, dashed=False, shade=None):
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    pixels = image.load()
    for y in range(height):
        for x in range(width):
            corner = x in (0, width - 1) and y in (0, height - 1)
            if corner:
                continue
            border = x in (0, width - 1) or y in (0, height - 1)
            if border:
                if not dashed or (x + y) % 3 != 0:
                    pixels[x, y] = outline + (255,)
                else:
                    pixels[x, y] = fill + (255,)
            elif shade and (x == 1 or y == 1):
                pixels[x, y] = shade + (255,)
            else:
                pixels[x, y] = fill + (255,)
    return image


def stacked(width, height, outline, fill, dashed=False, shade=None):
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    image.alpha_composite(box(width - 3, height - 3, outline, PAPER_DEEP, dashed), (3, 3))
    image.alpha_composite(box(width - 3, height - 3, outline, fill, dashed, shade), (0, 0))
    return image


def page_arrow(forward, highlighted):
    image = Image.new("RGBA", (12, 17), (0, 0, 0, 0))
    pixels = image.load()
    color = INK if highlighted else INK_MID
    for y in range(17):
        span = 8 - abs(y - 8)
        for x in range(12):
            reach = x - 2
            inside = 0 <= reach <= span and span >= 1
            if inside:
                px = x if forward else 11 - x
                edge = reach == span or reach == 0 or y in (0, 16)
                pixels[px, y] = (color if edge or highlighted else INK_SOFT) + (255,)
    return image


def filter_button(enabled, highlighted):
    image = box(26, 16, INK if highlighted else INK_MID, PAPER_BOX, shade=PAPER_SHADE)
    pixels = image.load()
    check = box(10, 10, INK, (250, 241, 219))
    image.alpha_composite(check, (8, 3))
    if enabled:
        for x, y in ((10, 8), (11, 9), (12, 10), (13, 9), (14, 8), (15, 7), (16, 6)):
            pixels[x, y] = INK + (255,)
            pixels[x, y - 1] = INK + (255,)
    return image


def magnifier():
    image = Image.new("RGBA", (9, 9), (0, 0, 0, 0))
    pixels = image.load()
    for x, y in ((2, 0), (3, 0), (4, 0), (1, 1), (5, 1), (0, 2), (6, 2), (0, 3), (6, 3), (0, 4), (6, 4), (1, 5), (5, 5), (2, 6), (3, 6), (4, 6), (6, 6), (7, 7), (8, 8), (7, 8), (8, 7)):
        pixels[x, y] = INK + (255,)
    return image


def flame():
    rows = [
        "....o.....",
        "...oho....",
        "...ohho...",
        "..ohhho.o.",
        "..ohwhhoho",
        ".ohwwhhhho",
        ".ohwwwhhho",
        "ohwwwwwhho",
        "ohwwwwwwho",
        ".ohwwwwho.",
        "..oooooo..",
    ]
    colors = {"o": INK, "h": INK_SOFT, "w": (231, 206, 160)}
    image = Image.new("RGBA", (10, len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != ".":
                image.putpixel((x, y), colors[key] + (255,))
    return image


def main():
    assets = Path(sys.argv[1])
    style = sys.argv[2] if len(sys.argv) > 2 else "default"
    gui = assets / "textures" / "gui"
    sprites = gui / "sprites"
    sprites.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (CANVAS_WIDTH, CANVAS_HEIGHT), (0, 0, 0, 0))
    canvas.paste(draw_book(style), (BOOK_LEFT, BOOK_TOP))
    canvas.save(gui / "book.png")
    cover = Image.new("RGBA", (CANVAS_WIDTH, CANVAS_HEIGHT), (0, 0, 0, 0))
    cover.paste(draw_cover_mask(style), (BOOK_LEFT, BOOK_TOP))
    cover.save(gui / "book_cover.png")
    draw_tab(style, 24, 24, False).save(sprites / "tab_top.png")
    draw_tab(style, 24, 27, True).save(sprites / "tab_top_selected.png")
    draw_side_tab(style, 25, 24, False).save(sprites / "tab_side.png")
    draw_side_tab(style, 28, 24, True).save(sprites / "tab_side_selected.png")
    draw_arrow(style).save(sprites / "recipe_arrow.png")
    box(23, 23, INK, PAPER_BOX, shade=PAPER_SHADE).save(sprites / "recipe_slot_craftable.png")
    box(23, 23, INK_SOFT, PAPER_SHADE, dashed=True).save(sprites / "recipe_slot_uncraftable.png")
    stacked(23, 23, INK, PAPER_BOX, shade=PAPER_SHADE).save(sprites / "recipe_slot_many_craftable.png")
    stacked(23, 23, INK_SOFT, PAPER_SHADE, dashed=True).save(sprites / "recipe_slot_many_uncraftable.png")
    selection = Image.new("RGBA", (23, 23), (0, 0, 0, 0))
    for y in range(23):
        for x in range(23):
            ring = min(x, y, 22 - x, 22 - y)
            corner = x in (0, 22) and y in (0, 22)
            if ring <= 1 and not corner:
                selection.putpixel((x, y), INK + (255,))
    selection.save(sprites / "recipe_slot_selected.png")
    box(18, 18, INK_MID, PAPER_BOX, shade=PAPER_SHADE).save(sprites / "slot.png")
    box(26, 26, INK, PAPER_BOX, shade=PAPER_SHADE).save(sprites / "result_slot.png")
    for forward, name in ((True, "page_forward"), (False, "page_backward")):
        page_arrow(forward, False).save(sprites / f"{name}.png")
        page_arrow(forward, True).save(sprites / f"{name}_highlighted.png")
    for enabled, name in ((True, "filter_enabled"), (False, "filter_disabled")):
        filter_button(enabled, False).save(sprites / f"{name}.png")
        filter_button(enabled, True).save(sprites / f"{name}_highlighted.png")
    ender = dict(STYLES[style], **ENDER_PAGES[style])
    STYLES["ender"] = ender
    ender_canvas = Image.new("RGBA", (CANVAS_WIDTH, CANVAS_HEIGHT), (0, 0, 0, 0))
    ender_canvas.paste(draw_book("ender"), (BOOK_LEFT, BOOK_TOP))
    ender_canvas.save(gui / "book_ender.png")
    draw_tab("ender", 24, 24, False).save(sprites / "tab_top_ender.png")
    draw_tab("ender", 24, 27, True).save(sprites / "tab_top_ender_selected.png")
    draw_side_tab("ender", 25, 24, False).save(sprites / "tab_side_ender.png")
    draw_side_tab("ender", 28, 24, True).save(sprites / "tab_side_ender_selected.png")
    del STYLES["ender"]
    magnifier().save(sprites / "search.png")
    flame().save(sprites / "flame.png")
    Image.new("RGBA", (16, 16), (255, 255, 255, 255)).save(gui / "ink.png")
    for name in ("pen", "eraser", "text"):
        draw_icon(style, name).save(sprites / f"{name}.png")


main()
