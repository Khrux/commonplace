import sys
from pathlib import Path

from PIL import Image

QUILL = [
    "..............ol",
    ".............olm",
    "............olmd",
    "...........olmdo",
    "..........olmdo.",
    ".........olmdo..",
    "........olmdo...",
    ".......olmdo....",
    "......olmdo.....",
    ".....olmdo......",
    "....ogGs........",
    "...ogGgo........",
    "...ooggo........",
    "...sooo.........",
    "..s.............",
    ".n..............",
]
QUILL_COLORS = {
    "o": (18, 12, 24),
    "d": (44, 30, 58),
    "m": (78, 54, 102),
    "l": (126, 96, 156),
    "s": (196, 190, 206),
    "g": (32, 128, 104),
    "G": (118, 222, 182),
    "n": (40, 30, 30),
}
BOTTLE = [
    "............o...",
    "...........ow...",
    "..........owo...",
    ".........owo....",
    "......cccwo.....",
    "......cCcw......",
    ".....ogggo......",
    "......ogo.......",
    ".....ohgso......",
    "....ohgggso.....",
    "...ohgiiiiso....",
    "...ohiiiiiso....",
    "...ogiiiiiso....",
    "...ogiiiiiso....",
    "....oggggso.....",
    ".....ooooo......",
]
BOTTLE_GREYS = {"o": 48, "w": 236, "c": 150, "C": 188, "g": 200, "h": 248, "s": 160, "i": 96}


BARE_BONES_QUILL_COLORS = dict(QUILL_COLORS, d=(78, 54, 102), l=(78, 54, 102), G=(32, 128, 104))


def draw(rows, colors):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key in colors:
                color = colors[key]
                image.putpixel((x, y), (color if isinstance(color, tuple) else (color, color, color)) + (255,))
    return image


def main():
    assets = Path(sys.argv[1])
    items = assets / "textures" / "item"
    gui = assets / "textures" / "gui" / "icon"
    items.mkdir(parents=True, exist_ok=True)
    if len(sys.argv) > 2 and sys.argv[2] == "bare_bones":
        draw(QUILL, BARE_BONES_QUILL_COLORS).save(items / "ender_quill.png")
        return
    gui.mkdir(parents=True, exist_ok=True)
    draw(QUILL, QUILL_COLORS).save(items / "ender_quill.png")
    draw(BOTTLE, BOTTLE_GREYS).save(gui / "passphrase.png")


main()
