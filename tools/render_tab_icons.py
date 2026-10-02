import sys
from pathlib import Path

from PIL import Image

PALETTE = {
    "o": (112, 70, 35),
    "d": (137, 94, 59),
    "m": (157, 116, 75),
    "l": (175, 140, 91),
    "h": (199, 168, 116),
    "w": (242, 226, 191),
}

ENDER_PALETTE = {
    "o": (20, 16, 13),
    "d": (42, 34, 27),
    "m": (64, 52, 41),
    "l": (88, 72, 57),
    "h": (116, 97, 77),
    "w": (150, 129, 104),
}

ICONS = {
    "field_guide": [
        "................",
        "....oo....oo....",
        "...ohlo..ohlo...",
        "...ohlo..ohlo...",
        "....oo....oo....",
        ".oo..........oo.",
        "ohlo..oooo..ohlo",
        "ohlo.ohhllo.ohlo",
        ".oo.ohhhllmo.oo.",
        "...ohhhhllmmo...",
        "...ohhhllmmmo...",
        "...ohhllmmmdo...",
        "....ollmmmdo....",
        ".....oooooo.....",
        "................",
        "................",
    ],
    "notes": [
        "............ooo.",
        "...........ohwo.",
        "..........ohwlo.",
        ".oooooooooohwlo.",
        ".owwwwwwwohwlo..",
        ".owhhhhhohwlo...",
        ".owwwwwohwlo....",
        ".owhhhhohwlow...",
        ".owwwwohlloow...",
        ".owhhhoolloho...",
        ".owwwwoooowwo...",
        ".owhhhhhhhhwo...",
        ".owwwwwwwwwwo...",
        ".ollllllllllo...",
        ".oooooooooooo...",
        "................",
    ],
    "ender": [
        "................",
        ".....oooooo.....",
        "...ooddmmddoo...",
        "..odmmllllmmdo..",
        "..omlhhhhhhlmo..",
        ".odlhwwooowhldo.",
        ".omhwwoooooWhmo.",
        ".omhwooooooowmo.",
        ".omhwooooooowmo.",
        ".omhwwoooooWhmo.",
        ".odlhwwooowhldo.",
        "..omlhhhhhhlmo..",
        "..odmmllllmmdo..",
        "...ooddmmddoo...",
        ".....oooooo.....",
        "................",
    ],
}


def blank():
    return [["."] * 16 for _ in range(16)]


def outline(grid):
    result = [row[:] for row in grid]
    for y in range(16):
        for x in range(16):
            if grid[y][x] != ".":
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and grid[ny][nx] not in ".o":
                    result[y][x] = "o"
    return result


def rotate(grid):
    return [[grid[15 - x][y] for x in range(16)] for y in range(16)]


def overlay(base, top):
    return [[top[y][x] if top[y][x] != "." else base[y][x] for x in range(16)] for y in range(16)]


def hammer_and_saw():
    saw = blank()
    for r in range(1, 10):
        for d in range(3):
            c = r + d
            saw[r][c] = "w" if d == 2 else "h"
        if r % 2 == 1 and r > 1:
            saw[r][r - 1] = "o"
    for r in range(10, 14):
        for c in range(10, 14):
            saw[r][c] = "m"
    saw[11][11] = saw[11][12] = "."
    saw = outline(saw)
    hammer = blank()
    for t in range(11):
        hammer[14 - t][1 + t] = "l"
    for r in range(16):
        for c in range(16):
            along = (r - 3) + (c - 12)
            across = (r - 3) - (c - 12)
            if abs(along) <= 3 and abs(across) <= 1:
                hammer[r][c] = "d" if across < 0 else "m"
    hammer = outline(hammer)
    return ["".join(row) for row in overlay(saw, hammer)]


def compass_rose():
    extents = {1: 1, 2: 1, 3: 2, 4: 2, 5: 2, 6: 3}
    grid = blank()
    for along_row, extent in list(extents.items()) + [(0, 0)]:
        along = 7 - along_row
        for across in range(-extent, extent + 1):
            edge = abs(across) == extent
            key = "o" if edge else "l" if across <= 0 else "w"
            for r, c in ((7 - along, 7 + across), (7 + along, 7 - across), (7 + across, 7 + along), (7 - across, 7 - along)):
                grid[r][c] = key
    for k, key in ((2, "o"), (3, "m"), (4, "m")):
        for r, c in ((k, k), (k, 14 - k), (14 - k, k), (14 - k, 14 - k)):
            if grid[r][c] == ".":
                grid[r][c] = key
    grid[7][7] = "w"
    for r, c in ((6, 7), (8, 7), (7, 6), (7, 8)):
        grid[r][c] = "o"
    return ["".join(row) for row in grid]


def draw(rows, palette=PALETTE):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        if len(row) != 16:
            raise ValueError(f"row {y} is {len(row)} wide: {row}")
        for x, key in enumerate(row):
            if key != ".":
                image.putpixel((x, y), palette[key.lower()] + (255,))
    return image


def main():
    target = Path(sys.argv[1]) / "textures" / "gui" / "icon"
    target.mkdir(parents=True, exist_ok=True)
    icons = dict(ICONS, recipes=hammer_and_saw(), atlas=compass_rose())
    for name, rows in icons.items():
        draw(rows, ENDER_PALETTE if name == "ender" else PALETTE).save(target / f"{name}.png")


main()
