import io
import json
import sys
import zipfile
from pathlib import Path

from PIL import Image

ELEVATIONS = ["valley", "low", "mid", "high", "peak"]
TILE_PREFIX = "assets/antique_atlas/textures/atlas/tile/"
GUI_FILES = {
    "assets/antique_atlas/textures/gui/player.png": "textures/gui/atlas/player.png",
    "assets/antique_atlas/textures/gui/icons/add_marker.png": "textures/gui/atlas/add_marker.png",
    "assets/antique_atlas/textures/gui/icons/del_marker.png": "textures/gui/atlas/del_marker.png",
}
MARKER_PREFIX = "assets/antique_atlas/textures/atlas/marker/custom/"
STRUCTURE_MARKER_PREFIX = "assets/antique_atlas/textures/atlas/marker/structure/"
STRUCTURE_PREFIX = "atlas/structure/"
DEFAULT_PRIORITY = 999


def texture_ids(textures):
    if isinstance(textures, dict):
        textures = list(textures.values())
    if isinstance(textures, str):
        textures = [textures]
    return [tile_id(texture) for texture in textures]


def marker_name(reference):
    return reference.split(":", 1)[1].removeprefix("structure/")


def resolve_structures(archive):
    structures = {"pieces": {}, "piece_types": {}, "starts": {}, "types": {}, "tags": {}}
    for name in archive.namelist():
        if not name.endswith(".json") or STRUCTURE_PREFIX not in name or not name.startswith("assets/"):
            continue
        namespace = name.split("/")[1]
        path = name.split(STRUCTURE_PREFIX, 1)[1][:-5]
        data = read_json(archive, name)
        entry = {}
        if "textures" in data:
            entry["textures"] = texture_ids(data["textures"])
            entry["priority"] = data.get("priority", DEFAULT_PRIORITY)
        if "markers" in data:
            entry["marker"] = marker_name(data["markers"] if isinstance(data["markers"], str) else data["markers"][0])
        if path.startswith("piece/jigsaw/single/"):
            structures["pieces"][namespace + ":" + path.removeprefix("piece/jigsaw/single/")] = entry
        elif path.startswith("piece/type/"):
            structures["piece_types"][namespace + ":" + path.removeprefix("piece/type/")] = entry
        else:
            kind, structure = path.split("/", 1)
            structures[kind + "s"][namespace + ":" + structure] = entry
    return structures


def structure_markers(archive):
    markers = {}
    for name in archive.namelist():
        if name.startswith(STRUCTURE_MARKER_PREFIX) and name.endswith(".png"):
            meta_name = name + ".mcmeta"
            meta = read_json(archive, meta_name).get("antique_atlas:marker", {}) if meta_name in archive.namelist() else {}
            image = Image.open(io.BytesIO(archive.read(name)))
            width = meta.get("textureWidth", image.width)
            height = meta.get("textureHeight", image.height)
            markers[name[len(STRUCTURE_MARKER_PREFIX):-4]] = {
                "width": width,
                "height": height,
                "texture_width": image.width,
                "texture_height": image.height,
                "offset_x": meta.get("offsetX", -width // 2),
                "offset_y": meta.get("offsetY", -height // 2),
                "near_clip": meta.get("nearClip", 0) > 0,
            }
    return markers
FEATURE_PREFIX = "assets/antique_atlas/atlas/biome/feature/"
BIOME_PREFIX = "assets/minecraft/atlas/biome/"


def tile_id(reference):
    namespace, path = reference.split(":", 1) if ":" in reference else ("antique_atlas", reference)
    if namespace != "antique_atlas":
        raise ValueError(f"unexpected tile namespace in {reference}")
    return path


def read_json(archive, name):
    return json.loads(archive.read(name).decode("utf-8"))


def resolve_tiling(archive):
    metas = {}
    for name in archive.namelist():
        if name.startswith(TILE_PREFIX) and name.endswith(".png"):
            path = name[len(TILE_PREFIX):-4]
            meta_name = name + ".mcmeta"
            meta = read_json(archive, meta_name).get("antique_atlas:tiling", {}) if meta_name in archive.namelist() else {}
            metas[path] = {
                "parent": tile_id(meta["parent"]) if "parent" in meta else None,
                "border": meta.get("borderType"),
                "tags": set(meta.get("tags", [])),
                **{key: set(meta.get(key, [])) for key in ("tilesTo", "tilesToHorizontal", "tilesToVertical", "tilesToThis", "tilesToThisHorizontal", "tilesToThisVertical")},
            }
    metas = {path: meta for path, meta in metas.items() if meta["parent"] is None or meta["parent"] in metas}
    keys = ("tags", "tilesTo", "tilesToHorizontal", "tilesToVertical", "tilesToThis", "tilesToThisHorizontal", "tilesToThisVertical")
    for meta in metas.values():
        parent = meta["parent"]
        while parent is not None:
            ancestor = metas[parent]
            if ancestor["border"] is not None:
                meta["border"] = ancestor["border"]
            for key in keys:
                meta[key] |= ancestor[key]
            parent = ancestor["parent"]
    tagged = {}
    for path, meta in metas.items():
        for tag in meta["tags"]:
            tagged.setdefault(tag, set()).add(path)
    for meta in metas.values():
        for key in keys[1:]:
            resolved = set()
            for entry in meta[key]:
                if entry.startswith("#"):
                    resolved |= tagged.get(entry[1:], set())
                else:
                    resolved.add(tile_id(entry))
            meta[key] = resolved
    for path, meta in metas.items():
        for source, target in (("tilesToThis", "tilesTo"), ("tilesToThisHorizontal", "tilesToHorizontal"), ("tilesToThisVertical", "tilesToVertical")):
            for other in meta[source]:
                if other in metas:
                    metas[other][target].add(path)
    return {
        path: {
            "inner_border": meta["border"] == "INNER",
            "tiles_to": sorted(meta["tilesTo"] & metas.keys()),
            "tiles_to_horizontal": sorted(meta["tilesToHorizontal"] & metas.keys()),
            "tiles_to_vertical": sorted(meta["tilesToVertical"] & metas.keys()),
        }
        for path, meta in sorted(metas.items())
    }


def expand(textures):
    if isinstance(textures, str):
        return [tile_id(textures)]
    if isinstance(textures, list):
        return [tile_id(entry) for entry in textures]
    return [tile_id(entry) for entry, weight in textures.items() for _ in range(weight)]


def resolve_provider(raw):
    textures = raw["textures"]
    if not isinstance(textures, dict) or not any(key in textures for key in ELEVATIONS):
        return [expand(textures)] * len(ELEVATIONS)
    result = [None] * len(ELEVATIONS)
    skipped = []
    last = None
    for index, elevation in enumerate(ELEVATIONS):
        if elevation not in textures:
            skipped.append(index)
            continue
        last = expand(textures[elevation])
        result[index] = last
        for skip in skipped:
            result[skip] = last
        skipped = []
    for skip in skipped:
        result[skip] = last
    return result


def resolve_providers(archive):
    raw = {}
    for name in archive.namelist():
        if name.endswith(".json") and name.startswith(BIOME_PREFIX):
            raw["minecraft:" + name[len(BIOME_PREFIX):-5]] = read_json(archive, name)
        elif name.endswith(".json") and name.startswith(FEATURE_PREFIX):
            raw["commonplace:" + name[len(FEATURE_PREFIX):-5]] = read_json(archive, name)
    providers = {key: resolve_provider(value) for key, value in raw.items() if "parent" not in value}
    for key, value in raw.items():
        if "parent" in value:
            parent = value["parent"]
            parent = parent if parent.startswith("minecraft:") else "minecraft:" + parent.split(":", 1)[-1]
            if parent in providers:
                providers[key] = providers[parent]
    return dict(sorted(providers.items()))


def main():
    jar = Path(sys.argv[1])
    assets = Path(sys.argv[2])
    with zipfile.ZipFile(jar) as archive:
        for name in archive.namelist():
            if name.startswith(TILE_PREFIX) and name.endswith(".png"):
                target = assets / "textures" / "atlas" / "tile" / name[len(TILE_PREFIX):]
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(name))
        for source, target in GUI_FILES.items():
            path = assets / target
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(archive.read(source))
        for name in archive.namelist():
            if name.startswith(STRUCTURE_MARKER_PREFIX) and name.endswith(".png"):
                target = assets / "textures" / "atlas" / "marker" / "structure" / name[len(STRUCTURE_MARKER_PREFIX):]
                target.parent.mkdir(parents=True, exist_ok=True)
                Image.open(io.BytesIO(archive.read(name))).convert("RGBA").save(target)
            if name.startswith(MARKER_PREFIX) and name.endswith(".png"):
                target = assets / "textures" / "atlas" / "marker" / name[len(MARKER_PREFIX):]
                target.parent.mkdir(parents=True, exist_ok=True)
                Image.open(io.BytesIO(archive.read(name))).convert("RGBA").save(target)
        licence = next(name for name in archive.namelist() if name.startswith("LICENSE"))
        (assets / "textures" / "atlas" / "LICENSE-antique-atlas.txt").write_bytes(archive.read(licence))
        data = assets / "atlas"
        data.mkdir(parents=True, exist_ok=True)
        (data / "tiles.json").write_text(json.dumps(resolve_tiling(archive), indent=1) + "\n")
        (data / "providers.json").write_text(json.dumps(resolve_providers(archive), indent=1) + "\n")
        (data / "structure_markers.json").write_text(json.dumps(structure_markers(archive), indent=1, sort_keys=True) + "\n")
        server_data = assets.parent.parent / "data" / assets.name / "atlas"
        server_data.mkdir(parents=True, exist_ok=True)
        (server_data / "structures.json").write_text(json.dumps(resolve_structures(archive), indent=1, sort_keys=True) + "\n")


main()
