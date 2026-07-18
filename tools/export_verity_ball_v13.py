#!/usr/bin/env python3
"""
Export V1.3 Figura Verity ball bbmodel → GeckoLib geo/animations/textures.

GeckoLib cannot load Figura meshes directly, so the sphere mesh is approximated
with a denser cube lattice derived from the authoritative mesh AABB (11×11×11).
Face planes and expression textures come from the bbmodel / folder assets.
"""

from __future__ import annotations

import json
import math
import shutil
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SRC_DIR = ROOT / "provided_assets" / "verity_ball_v1.3"
BBMODEL = SRC_DIR / "verity_ball_model.bbmodel"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "universe_verity"

# Perfect sphere hitbox/visual diameter (blocks). Source mesh AABB is 11×11×11 px = 0.6875³.
# Uniform scale keeps the yellow ball spherical (no vertical stretch).
TARGET_DIAMETER = 0.60
TARGET_W = TARGET_DIAMETER
TARGET_H = TARGET_DIAMETER
MESH_BLOCKS = 11.0 / 16.0  # 0.6875


def load_bb() -> dict[str, Any]:
    return json.loads(BBMODEL.read_text(encoding="utf-8"))


def save_json(path: Path, data: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent="\t", ensure_ascii=False) + "\n", encoding="utf-8")


def bedrock_pivot(origin: list[float]) -> list[float]:
    return [round(origin[0], 4), round(origin[1], 4), round(-origin[2], 4)]


def cube_from_bb(from_p: list[float], to_p: list[float], uv_rect: tuple[float, float, float, float]) -> dict[str, Any]:
    """from/to in Blockbench space → bedrock cube with uv_size."""
    # Ensure non-zero depth for zero-thickness face planes
    fp = list(from_p)
    tp = list(to_p)
    for i in range(3):
        if abs(tp[i] - fp[i]) < 0.05:
            tp[i] = fp[i] + (0.06 if i != 1 else 0.06)
            if i == 2:
                # thicken toward -Z (front in BB) so plane stays at front
                fp[i] = min(fp[i], tp[i]) - 0.03
                tp[i] = fp[i] + 0.06

    origin = [fp[0], fp[1], -tp[2]]
    size = [tp[0] - fp[0], tp[1] - fp[1], tp[2] - fp[2]]
    u0, v0, u1, v1 = uv_rect
    uw, vh = u1 - u0, v1 - v0

    def face(u: float, v: float, w: float, h: float) -> dict[str, Any]:
        return {"uv": [u, v], "uv_size": [w, h]}

    return {
        "origin": [round(x, 4) for x in origin],
        "size": [round(x, 4) for x in size],
        "uv": {
            "north": face(u0, v0, uw, vh),
            "east": face(u0, v0, max(1, uw * 0.05), vh),
            "south": face(u0, v0, uw, vh),
            "west": face(u0, v0, max(1, uw * 0.05), vh),
            "up": face(u0, v0, uw, max(1, vh * 0.05)),
            "down": face(u0, v0, uw, max(1, vh * 0.05)),
        },
    }


def synthesize_sphere_cubes(atlas_body_uv: tuple[float, float, float, float]) -> list[dict[str, Any]]:
    """
    High-segment near-perfect sphere for GeckoLib (cubes only — no extra mods).

    Source Figura mesh radius is 5.5 (AABB 11³). We build:
      1) a solid voxel core for volume / no hollow flicker
      2) dense lat/long shell cubes for a smooth silhouette
    Tiny bottom clamp (y >= 0.08) only so the ball does not visually float; silhouette stays spherical.
    """
    cx, cy, cz = 0.0, 5.5, 0.0
    radius = 5.5
    floor_y = 0.08  # tiny ground contact — not a flat pad

    cubes: list[dict[str, Any]] = []
    u0, v0, u1, v1 = atlas_body_uv
    seen: set[tuple[float, float, float, float]] = set()

    def add_box(x: float, y: float, z: float, s: float) -> None:
        half = s / 2.0
        y0 = y - half
        y1 = y + half
        if y1 <= floor_y:
            return
        if y0 < floor_y:
            y0 = floor_y
        key = (round(x, 3), round(y0, 3), round(z, 3), round(s, 3))
        if key in seen:
            return
        seen.add(key)
        cubes.append(cube_from_bb([x - half, y0, z - half], [x + half, y1, z + half], (u0, v0, u1, v1)))

    # Solid voxel fill (step ~0.95) — keeps the interior opaque and round.
    step = 0.95
    half_span = radius - step * 0.35
    n = int(math.ceil((2.0 * half_span) / step))
    for ix in range(n + 1):
        for iy in range(n + 1):
            for iz in range(n + 1):
                x = cx - half_span + ix * step
                y = cy - half_span + iy * step
                z = cz - half_span + iz * step
                # Slightly inset so shell cubes define the outer silhouette.
                if (x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2 <= (radius * 0.78) ** 2:
                    add_box(x, y, z, step * 1.05)

    # Dense UV-sphere shell for a smooth outline (enough segments to read as a ball).
    rings = 22
    for iy in range(rings + 1):
        v = iy / rings
        phi = math.pi * v  # 0..pi
        y = cy + radius * math.cos(phi)
        ring_r = radius * math.sin(phi)
        if ring_r < 0.35:
            add_box(cx, y, cz, 1.05)
            continue
        # ~24 segments at equator, fewer near poles — still dense.
        segs = max(10, int(round(26 * (ring_r / radius))))
        cube_s = 1.05 if 0.18 < v < 0.82 else 0.95
        for ix in range(segs):
            theta = (2.0 * math.pi * ix) / segs
            x = cx + ring_r * math.cos(theta)
            z = cz + ring_r * math.sin(theta)
            add_box(x, y, z, cube_s)

    # Extra mid-latitude shell offset by half-segment to break vertical ridges.
    for iy in range(1, rings):
        v = iy / rings
        if v < 0.12 or v > 0.88:
            continue
        phi = math.pi * v
        y = cy + radius * math.cos(phi)
        ring_r = radius * math.sin(phi)
        segs = max(10, int(round(26 * (ring_r / radius))))
        for ix in range(segs):
            theta = (2.0 * math.pi * (ix + 0.5)) / segs
            x = cx + ring_r * math.cos(theta)
            z = cz + ring_r * math.sin(theta)
            add_box(x, y, z, 0.92)

    return cubes


def make_blink_face(idle: Image.Image) -> Image.Image:
    """Close the oval eyes on the idle face sheet."""
    img = idle.convert("RGBA")
    w, h = img.size
    draw = ImageDraw.Draw(img)
    # Sample line color from idle eyes region
    line = (200, 200, 200, 255)
    # Cover eye ovals with black, draw thin closed lids
    # Eyes roughly upper-center on 512 sheet
    for cx in (w * 0.38, w * 0.62):
        ex, ey = cx, h * 0.42
        # black out eye area
        draw.ellipse((ex - w * 0.05, ey - h * 0.08, ex + w * 0.05, ey + h * 0.08), fill=(0, 0, 0, 255))
        draw.line((ex - w * 0.045, ey, ex + w * 0.045, ey), fill=line, width=max(2, w // 128))
    return img


def build_atlas(out_path: Path, expr_dir: Path) -> dict[str, tuple[float, float, float, float]]:
    """
    1024×1024 atlas:
      body yellow fill
      face slots: idle, speak, bored, blink, sus  (256×256 each along top)
    """
    base = Image.open(SRC_DIR / "verity_base_texture.png").convert("RGBA")
    # sample yellow
    yellow = base.getpixel((base.size[0] // 2, base.size[1] // 2))

    atlas = Image.new("RGBA", (1024, 1024), yellow)
    # subtle bottom wear band
    wear = Image.new("RGBA", (1024, 220), (max(0, yellow[0] - 35), max(0, yellow[1] - 40), max(0, yellow[2] - 20), 55))
    atlas.paste(wear, (0, 804), wear)

    faces = {
        "idle": Image.open(SRC_DIR / "idle.png").convert("RGBA"),
        "speak": Image.open(SRC_DIR / "speak.png").convert("RGBA"),
        "bored": Image.open(SRC_DIR / "bored.png").convert("RGBA"),
        "sus": Image.open(SRC_DIR / "sus.png").convert("RGBA"),
    }
    faces["blink"] = make_blink_face(faces["idle"])

    slots: dict[str, tuple[float, float, float, float]] = {}
    order = ["idle", "speak", "bored", "blink", "sus"]
    for i, name in enumerate(order):
        x = i * 200
        face = faces[name].resize((192, 192), Image.Resampling.NEAREST)
        atlas.paste(face, (x + 4, 4), face)
        slots[name] = (float(x + 4), 4.0, float(x + 4 + 192), 196.0)
        # also write standalone expression sheets for renderer swaps / tooling
        expr_dir.mkdir(parents=True, exist_ok=True)
        # composite body+face preview not required; store face sheet
        faces[name].save(expr_dir / f"verity_{'listening' if name == 'bored' else name}.png")

    # body UV region (yellow only, avoid face strip)
    slots["body"] = (0.0, 220.0, 1024.0, 1024.0)

    # Map friendly names
    slots["neutral"] = slots["idle"]
    slots["greeting"] = slots["speak"]
    slots["listening"] = slots["bored"]
    slots["long_blink"] = slots["blink"]

    out_path.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(out_path)

    # Also copy originals beside entity textures for reference
    tex_dir = out_path.parent
    shutil.copy2(SRC_DIR / "verity_base_texture.png", tex_dir / "verity_base_texture.png")
    shutil.copy2(SRC_DIR / "idle.png", tex_dir / "verity_face_idle.png")
    shutil.copy2(SRC_DIR / "speak.png", tex_dir / "verity_face_speak.png")
    shutil.copy2(SRC_DIR / "bored.png", tex_dir / "verity_face_bored.png")
    faces["blink"].save(tex_dir / "verity_face_blink.png")
    return slots


def build_geo(slots: dict[str, tuple[float, float, float, float]]) -> dict[str, Any]:
    body_cubes = synthesize_sphere_cubes(slots["body"])

    def face_cube(slot: str) -> dict[str, Any]:
        # Match bbmodel face plane: from [-4,2,-5.5] to [4,9,-5.5]
        return cube_from_bb([-4.0, 2.0, -5.56], [4.0, 9.0, -5.50], slots[slot])

    bones = [
        {"name": "root", "pivot": [0.0, 0.0, 0.0]},
        {"name": "body", "parent": "root", "pivot": bedrock_pivot([0.0, 5.5, 0.0])},
        {
            "name": "body_roll",
            "parent": "body",
            "pivot": bedrock_pivot([0.0, 5.5, 0.0]),
            "cubes": body_cubes,
        },
        {"name": "face_root", "parent": "root", "pivot": bedrock_pivot([0.0, 5.0, 0.0])},
        {
            "name": "face_neutral",
            "parent": "face_root",
            "pivot": bedrock_pivot([0.0, 5.0, 0.0]),
            "cubes": [face_cube("idle")],
        },
        {
            "name": "face_greeting",
            "parent": "face_root",
            "pivot": bedrock_pivot([0.0, 5.0, 0.0]),
            "cubes": [face_cube("speak")],
        },
        {
            "name": "face_listening",
            "parent": "face_root",
            "pivot": bedrock_pivot([0.0, 5.0, 0.0]),
            "cubes": [face_cube("bored")],
        },
        {
            "name": "face_blink",
            "parent": "face_root",
            "pivot": bedrock_pivot([0.0, 5.0, 0.0]),
            "cubes": [face_cube("blink")],
        },
        {
            "name": "mouth",
            "parent": "face_root",
            "pivot": bedrock_pivot([0.0, 5.0, 0.0]),
        },
        {
            "name": "expression_root",
            "parent": "face_root",
            "pivot": bedrock_pivot([0.0, 5.0, 0.0]),
        },
    ]

    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.verity",
                    "texture_width": 1024,
                    "texture_height": 1024,
                    "visible_bounds_width": 2.5,
                    "visible_bounds_height": 2.5,
                    "visible_bounds_offset": [0.0, 0.55, 0.0],
                },
                "bones": bones,
            }
        ],
    }


def kf(times: dict[str, list[float]]) -> dict[str, list[float]]:
    return times


def scale_hide() -> dict[str, list[float]]:
    return {"0.0": [0, 0, 0]}


def scale_show() -> dict[str, list[float]]:
    return {"0.0": [1, 1, 1]}


def expression_anim(show: str) -> dict[str, Any]:
    faces = ["face_neutral", "face_greeting", "face_listening", "face_blink"]
    bones = {}
    for f in faces:
        bones[f] = {"scale": scale_show() if f == show else scale_hide()}
    return {"loop": True, "bones": bones}


def build_animations() -> dict[str, Any]:
    # Subtle idle — restrained
    idle = {
        "loop": True,
        "animation_length": 6.0,
        "bones": {
            "root": {
                "position": {
                    "0.0": [0, 0, 0],
                    "3.0": [0, 0.12, 0],
                    "6.0": [0, 0, 0],
                },
                "rotation": {
                    "0.0": [0, 0, 0],
                    "3.0": [0, 0.8, 0],
                    "6.0": [0, 0, 0],
                },
            },
            "body": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "3.0": [0.4, 0, 0],
                    "6.0": [0, 0, 0],
                }
            },
        },
    }

    talk = {
        "loop": True,
        "animation_length": 0.875,
        "bones": {
            "root": {
                "position": {
                    "0.0": [0, 0, 0],
                    "0.2": [0, 0.18, 0],
                    "0.45": [0, 0, 0],
                    "0.7": [0, 0.12, 0],
                    "0.875": [0, 0, 0],
                },
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.25": [1.5, 0, 0],
                    "0.5": [-1.0, 0, 0],
                    "0.875": [0, 0, 0],
                },
            },
            # Mouth swap while talking (Figura-style idle/speak panels)
            "face_neutral": {
                "scale": {
                    "0.0": [1, 1, 1],
                    "0.17": [0, 0, 0],
                    "0.33": [1, 1, 1],
                    "0.54": [0, 0, 0],
                    "0.71": [1, 1, 1],
                }
            },
            "face_greeting": {
                "scale": {
                    "0.0": [0, 0, 0],
                    "0.17": [1, 1, 1],
                    "0.33": [0, 0, 0],
                    "0.54": [1, 1, 1],
                    "0.71": [0, 0, 0],
                }
            },
        },
    }

    greeting = {
        "loop": False,
        "animation_length": 0.9,
        "bones": {
            "root": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.3": [2, 8, 0],
                    "0.6": [0, -4, 0],
                    "0.9": [0, 0, 0],
                },
                "position": {
                    "0.0": [0, 0, 0],
                    "0.25": [0, 0.25, 0],
                    "0.9": [0, 0, 0],
                },
            },
        },
    }

    reveal = {
        "loop": False,
        "animation_length": 1.0,
        "bones": {
            "root": {
                "position": {
                    "0.0": [0, -1.5, 0],
                    "0.35": [0, 0.35, 0],
                    "0.55": [0, -0.1, 0],
                    "1.0": [0, 0, 0],
                },
                "rotation": {
                    "0.0": [8, 0, 0],
                    "0.5": [-2, 0, 0],
                    "1.0": [0, 0, 0],
                },
            },
        },
    }

    roll = {
        "loop": True,
        "animation_length": 1.0,
        "bones": {
            "body_roll": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.5": [180, 0, 0],
                    "1.0": [360, 0, 0],
                }
            },
            "face_root": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.5": [-8, 0, 0],
                    "1.0": [0, 0, 0],
                }
            },
        },
    }

    stop_settle = {
        "loop": False,
        "animation_length": 0.45,
        "bones": {
            "root": {
                "rotation": {
                    "0.0": [4, 0, 0],
                    "0.2": [-3, 0, 0],
                    "0.45": [0, 0, 0],
                },
                "position": {
                    "0.0": [0, 0.1, 0],
                    "0.45": [0, 0, 0],
                },
            }
        },
    }

    listening = {
        "loop": True,
        "animation_length": 2.0,
        "bones": {
            "root": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "1.0": [0, 0, 4],
                    "2.0": [0, 0, 0],
                }
            },
        },
    }

    blink = {
        "loop": False,
        "animation_length": 0.14,
        "bones": {},
    }

    long_blink = {
        "loop": False,
        "animation_length": 0.48,
        "bones": {},
    }

    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.verity.idle": idle,
            "animation.verity.reveal": reveal,
            "animation.verity.talk": talk,
            "animation.verity.greeting": greeting,
            "animation.verity.listening": listening,
            "animation.verity.blink": blink,
            "animation.verity.long_blink": long_blink,
            "animation.verity.roll_slow": roll,
            "animation.verity.roll_normal": roll,
            "animation.verity.roll_fast": {
                **roll,
                "animation_length": 0.55,
            },
            "animation.verity.stop_settle": stop_settle,
            "animation.verity.expression_neutral": expression_anim("face_neutral"),
            "animation.verity.expression_greeting": expression_anim("face_greeting"),
            "animation.verity.expression_listening": expression_anim("face_listening"),
            "animation.verity.expression_blink": expression_anim("face_blink"),
            "animation.verity.expression_long_blink": expression_anim("face_blink"),
        },
    }


def write_all_copies(geo: dict[str, Any], anims: dict[str, Any], atlas_src: Path) -> list[str]:
    written: list[str] = []
    geo_targets = [
        ASSETS / "geo" / "verity.geo.json",
        ASSETS / "geo" / "entity" / "verity.geo.json",
        ASSETS / "geckolib" / "geo" / "verity.geo.json",
        ASSETS / "geckolib" / "geo" / "entity" / "verity.geo.json",
    ]
    anim_targets = [
        ASSETS / "animations" / "verity.animation.json",
        ASSETS / "animations" / "entity" / "verity.animation.json",
        ASSETS / "geckolib" / "animations" / "verity.animation.json",
        ASSETS / "geckolib" / "animations" / "entity" / "verity.animation.json",
    ]
    tex_targets = [
        ASSETS / "textures" / "entity" / "verity.png",
    ]
    for p in geo_targets:
        save_json(p, geo)
        written.append(str(p))
    for p in anim_targets:
        save_json(p, anims)
        written.append(str(p))
    for p in tex_targets:
        p.parent.mkdir(parents=True, exist_ok=True)
        if p.resolve() != atlas_src.resolve():
            shutil.copy2(atlas_src, p)
        written.append(str(p))
    return written


def main() -> int:
    if not BBMODEL.is_file():
        raise SystemExit(f"Missing bbmodel: {BBMODEL}")

    expr_dir = ASSETS / "textures" / "entity" / "expressions"
    atlas_path = ASSETS / "textures" / "entity" / "verity.png"
    slots = build_atlas(atlas_path, expr_dir)
    geo = build_geo(slots)
    anims = build_animations()
    written = write_all_copies(geo, anims, atlas_path)

    report = {
        "source_bbmodel": str(BBMODEL),
        "mesh_blocks": MESH_BLOCKS,
        "target_hitbox": {
            "width": TARGET_W,
            "height": TARGET_H,
            "depth": TARGET_W,
            "note": "Perfect sphere: equal width/height/depth = 0.60 (matches yellow ball diameter).",
        },
        "scale_factors": {
            "x": TARGET_DIAMETER / MESH_BLOCKS,
            "y": TARGET_DIAMETER / MESH_BLOCKS,
            "z": TARGET_DIAMETER / MESH_BLOCKS,
        },
        "bones": [b["name"] for b in geo["minecraft:geometry"][0]["bones"]],
        "animations": list(anims["animations"].keys()),
        "atlas": str(atlas_path),
        "atlas_size": [1024, 1024],
        "body_cubes": len(geo["minecraft:geometry"][0]["bones"][2]["cubes"]),
        "written": written,
        "limitation": (
            "Figura mesh cannot be loaded by GeckoLib; no extra mods required. "
            "Approximated with a dense voxel+shell cube sphere (AABB 11×11×11), "
            "uniform 0.60 diameter hitbox/scale, V1.3 yellow + face textures preserved."
        ),
    }
    report_path = ROOT / "tools" / "verity_ball_v13_export_report.json"
    save_json(report_path, report)
    print(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
