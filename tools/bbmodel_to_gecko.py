#!/usr/bin/env python3
"""
Convert Blockbench .bbmodel files to GeckoLib / Bedrock geometry and animation JSON.

Supports:
  - Cube elements with per-face UV from bbmodel outliner bone hierarchy
  - Animation conversion (bone UUID -> name mapping)
  - Mesh -> cube approximation for Figura sphere meshes
  - Embedded base64 texture extraction from bbmodel
  - Procedural fallback animations for entity interaction states
"""

from __future__ import annotations

import argparse
import base64
import json
import math
import re
import shutil
import struct
import sys
import zlib
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def load_bbmodel(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as fh:
        return json.load(fh)


def save_json(path: Path, data: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as fh:
        json.dump(data, fh, indent="\t", ensure_ascii=False)
        fh.write("\n")


def fmt_time(seconds: float) -> str:
    """Bedrock animation keys use short decimal strings."""
    if seconds == 0.0:
        return "0.0"
    text = f"{seconds:.5f}".rstrip("0").rstrip(".")
    return text if "." in text else f"{text}.0"


def parse_num(value: Any) -> Any:
    """Return float when possible, otherwise keep expression strings."""
    if isinstance(value, (int, float)):
        return float(value)
    if isinstance(value, str):
        stripped = value.strip()
        if not stripped:
            return 0.0
        try:
            return float(stripped)
        except ValueError:
            return stripped
    return value


def vec3_from_datapoint(dp: dict[str, Any]) -> list[Any]:
    return [parse_num(dp.get("x", 0)), parse_num(dp.get("y", 0)), parse_num(dp.get("z", 0))]


def bedrock_pivot(origin: list[float]) -> list[float]:
    return [origin[0], origin[1], -origin[2]]


def bedrock_cube_origin_size(from_p: list[float], to_p: list[float]) -> tuple[list[float], list[float]]:
    origin = [from_p[0], from_p[1], -to_p[2]]
    size = [to_p[0] - from_p[0], to_p[1] - from_p[1], to_p[2] - from_p[2]]
    return origin, size


def flip_rotation_z(values: list[Any]) -> list[Any]:
    """Mirror numeric Z rotation for Bedrock handedness; preserve Molang strings."""
    if len(values) < 3:
        return values
    z = values[2]
    if isinstance(z, (int, float)):
        return [values[0], values[1], -z]
    return values


def flip_position_z(values: list[Any]) -> list[Any]:
    if len(values) < 3:
        return values
    z = values[2]
    if isinstance(z, (int, float)):
        return [values[0], values[1], -z]
    if isinstance(z, str):
        if z == "0":
            return [values[0], values[1], "0"]
        return [values[0], values[1], f"-({z})"]
    return values


# ---------------------------------------------------------------------------
# Texture extraction
# ---------------------------------------------------------------------------

def decode_data_uri(data_uri: str) -> bytes | None:
    if not data_uri or not data_uri.startswith("data:"):
        return None
    try:
        _header, encoded = data_uri.split(",", 1)
    except ValueError:
        return None
    return base64.b64decode(encoded)


def minimal_png(width: int, height: int, rgba: tuple[int, int, int, int]) -> bytes:
    """Build a flat-color PNG when bbmodel embeds a broken/placeholder texture."""
    r, g, b, a = rgba
    row = bytes([r, g, b, a]) * width
    raw = b"".join([b"\x00" + row for _ in range(height)])
    compressed = zlib.compress(raw, 9)

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")


def is_valid_png(data: bytes, min_size: int = 500) -> bool:
    return len(data) >= min_size and data.startswith(b"\x89PNG\r\n\x1a\n")


def extract_texture(texture_entry: dict[str, Any], fallback_path: Path | None, out_path: Path) -> bool:
    source = texture_entry.get("source", "")
    data = decode_data_uri(source)
    if data and is_valid_png(data):
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_bytes(data)
        return True
    if fallback_path and fallback_path.is_file():
        out_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(fallback_path, out_path)
        return True
    # Placeholder so resource pack stays valid
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(minimal_png(64, 64, (200, 160, 120, 255)))
    return False


def find_texture_by_name(model: dict[str, Any], name: str) -> dict[str, Any] | None:
    for tex in model.get("textures", []):
        if tex.get("name", "").lower() == name.lower():
            return tex
    return None


# ---------------------------------------------------------------------------
# Geometry conversion
# ---------------------------------------------------------------------------

@dataclass
class BoneNode:
    name: str
    uuid: str
    origin: list[float]
    children: list["BoneNode | str"] = field(default_factory=list)


def build_element_map(model: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {element["uuid"]: element for element in model.get("elements", [])}


def walk_outliner(nodes: list[Any]) -> list[BoneNode]:
    result: list[BoneNode] = []
    for node in nodes:
        if isinstance(node, str):
            continue
        bone = BoneNode(
            name=node["name"],
            uuid=node["uuid"],
            origin=list(node.get("origin", [0, 0, 0])),
            children=node.get("children", []),
        )
        result.append(bone)
    return result


def flatten_bones(nodes: list[Any], element_map: dict[str, dict[str, Any]]) -> tuple[dict[str, BoneNode], dict[str, str]]:
    """Return bone lookup and element-uuid -> parent bone name."""
    bones: dict[str, BoneNode] = {}
    element_parent: dict[str, str] = {}

    def recurse(node_list: list[Any], parent_name: str | None = None) -> None:
        for node in node_list:
            if isinstance(node, str):
                if parent_name:
                    element_parent[node] = parent_name
                continue
            bone = BoneNode(
                name=node["name"],
                uuid=node["uuid"],
                origin=list(node.get("origin", [0, 0, 0])),
                children=node.get("children", []),
            )
            bones[bone.name] = bone
            recurse(node.get("children", []), bone.name)

    recurse(nodes)
    return bones, element_parent


def convert_face_uv(faces: dict[str, Any], tex_w: int, tex_h: int) -> dict[str, dict[str, Any]]:
    uv_out: dict[str, dict[str, Any]] = {}
    for face_name, face in faces.items():
        uv = face.get("uv")
        if not uv or len(uv) != 4:
            continue
        uv_out[face_name] = {
            "uv": [uv[0], uv[1], uv[2], uv[3]],
            "texture_width": tex_w,
            "texture_height": tex_h,
        }
    return uv_out


def convert_cube_element(element: dict[str, Any], tex_w: int, tex_h: int) -> dict[str, Any]:
    from_p = element["from"]
    to_p = element["to"]
    origin, size = bedrock_cube_origin_size(from_p, to_p)
    cube: dict[str, Any] = {"origin": origin, "size": size}
    if element.get("faces"):
        cube["uv"] = convert_face_uv(element["faces"], tex_w, tex_h)
    inflate = element.get("inflate", 0)
    if inflate:
        cube["inflate"] = inflate
    return cube


def mesh_bounds(element: dict[str, Any]) -> tuple[list[float], list[float]]:
    verts = element.get("vertices", {})
    xs = [v[0] for v in verts.values()]
    ys = [v[1] for v in verts.values()]
    zs = [v[2] for v in verts.values()]
    origin = element.get("origin", [0, 0, 0])
    from_p = [min(xs) + origin[0], min(ys) + origin[1], min(zs) + origin[2]]
    to_p = [max(xs) + origin[0], max(ys) + origin[1], max(zs) + origin[2]]
    return from_p, to_p


def synthesize_sphere_cubes(
    element: dict[str, Any],
    tex_w: int,
    tex_h: int,
    *,
    target_size: float = 8.0,
    center_y: float = 5.5,
) -> list[dict[str, Any]]:
    """
    Approximate a Figura mesh sphere with a primary body cube plus corner/edge
    filler cubes for a slightly rounder silhouette.
    """
    from_p, to_p = mesh_bounds(element)
    mesh_w = to_p[0] - from_p[0]
    mesh_h = to_p[1] - from_p[1]
    mesh_d = to_p[2] - from_p[2]

    # Prefer explicit target when mesh bounds are close; otherwise use mesh AABB.
    use_w = target_size if abs(mesh_w - target_size) < 2.0 else round(mesh_w, 2)
    use_h = target_size if abs(mesh_h - target_size) < 2.0 else round(mesh_h, 2)
    use_d = target_size if abs(mesh_d - target_size) < 2.0 else round(mesh_d, 2)

    cx = (from_p[0] + to_p[0]) / 2.0
    cz = (from_p[2] + to_p[2]) / 2.0
    cy = center_y if center_y else (from_p[1] + to_p[1]) / 2.0

    half_w, half_h, half_d = use_w / 2.0, use_h / 2.0, use_d / 2.0
    main_from = [cx - half_w, cy - half_h, cz - half_d]
    main_to = [cx + half_w, cy + half_h, cz + half_d]

    fake = {
        "from": main_from,
        "to": main_to,
        "faces": {
            "north": {"uv": [0, 0, tex_w, tex_h], "texture": 0},
            "south": {"uv": [0, 0, tex_w, tex_h], "texture": 0},
            "east": {"uv": [0, 0, tex_w, tex_h], "texture": 0},
            "west": {"uv": [0, 0, tex_w, tex_h], "texture": 0},
            "up": {"uv": [0, 0, tex_w, tex_h], "texture": 0},
            "down": {"uv": [0, 0, tex_w, tex_h], "texture": 0},
        },
    }
    cubes = [convert_cube_element(fake, tex_w, tex_h)]

    # Small edge cubes (25% scale) on 6 face centers for subtle roundness.
    edge = min(use_w, use_h, use_d) * 0.25
    offsets = [
        (0, half_h, 0),
        (0, -half_h, 0),
        (half_w, 0, 0),
        (-half_w, 0, 0),
        (0, 0, half_d),
        (0, 0, -half_d),
    ]
    for ox, oy, oz in offsets:
        e_from = [cx + ox - edge / 2, cy + oy - edge / 2, cz + oz - edge / 2]
        e_to = [cx + ox + edge / 2, cy + oy + edge / 2, cz + oz + edge / 2]
        edge_elem = {
            "from": e_from,
            "to": e_to,
            "faces": fake["faces"],
        }
        cubes.append(convert_cube_element(edge_elem, tex_w, tex_h))

    return cubes


def compute_visible_bounds(all_origins: list[list[float]], all_sizes: list[list[float]]) -> tuple[float, float, list[float]]:
    if not all_origins:
        return 2.0, 2.0, [0.0, 1.0, 0.0]
    min_x = min_y = min_z = float("inf")
    max_x = max_y = max_z = float("-inf")
    for origin, size in zip(all_origins, all_sizes):
        min_x = min(min_x, origin[0])
        min_y = min(min_y, origin[1])
        min_z = min(min_z, origin[2])
        max_x = max(max_x, origin[0] + size[0])
        max_y = max(max_y, origin[1] + size[1])
        max_z = max(max_z, origin[2] + size[2])
    width = max(max_x - min_x, max(max_y - min_y, max_z - min_z)) / 16.0 * 2.0
    height = (max_y - min_y) / 16.0 * 1.5
    offset_y = (min_y + max_y) / 2.0 / 16.0
    return max(width, 0.5), max(height, 0.5), [0.0, offset_y, 0.0]


def build_geometry(
    model: dict[str, Any],
    identifier: str,
    *,
    skip_bones: set[str] | None = None,
    mesh_bones: set[str] | None = None,
    mesh_target_size: float = 8.0,
    mesh_center_y: float = 5.5,
    include_bones: set[str] | None = None,
) -> tuple[dict[str, Any], list[str], dict[str, list[float]]]:
    skip_bones = skip_bones or set()
    mesh_bones = mesh_bones or {"sphere"}
    element_map = build_element_map(model)
    bones_map, element_parent = flatten_bones(model.get("outliner", []), element_map)
    resolution = model.get("resolution", {"width": 16, "height": 16})
    tex_w = int(resolution.get("width", 16))
    tex_h = int(resolution.get("height", 16))

    bone_cubes: dict[str, list[dict[str, Any]]] = {name: [] for name in bones_map}
    all_origins: list[list[float]] = []
    all_sizes: list[list[float]] = []

    for uuid, element in element_map.items():
        parent = element_parent.get(uuid)
        if not parent or parent in skip_bones:
            continue
        if include_bones and parent not in include_bones and not any(
            bones_map.get(parent) and parent in bones_map for _ in [0]
        ):
            pass
        if element.get("visibility") is False:
            continue
        if element.get("type") == "mesh" and parent in mesh_bones:
            for cube in synthesize_sphere_cubes(
                element, tex_w, tex_h, target_size=mesh_target_size, center_y=mesh_center_y
            ):
                bone_cubes.setdefault(parent, []).append(cube)
                all_origins.append(cube["origin"])
                all_sizes.append(cube["size"])
            continue
        if element.get("type") not in (None, "cube"):
            continue
        cube = convert_cube_element(element, tex_w, tex_h)
        bone_cubes.setdefault(parent, []).append(cube)
        all_origins.append(cube["origin"])
        all_sizes.append(cube["size"])

    geo_bones: list[dict[str, Any]] = []
    bone_order: list[str] = []

    def emit_bone(name: str) -> None:
        if name in skip_bones:
            return
        if include_bones and name not in include_bones:
            # Still recurse into children in case include set targets deeper bones.
            pass
        bone = bones_map[name]
        entry: dict[str, Any] = {
            "name": bone.name,
            "pivot": bedrock_pivot(bone.origin),
        }
        cubes = bone_cubes.get(name, [])
        if cubes:
            entry["cubes"] = cubes
        geo_bones.append(entry)
        bone_order.append(name)

    def walk_emit(node_list: list[Any]) -> None:
        for node in node_list:
            if isinstance(node, str):
                continue
            name = node["name"]
            if name in skip_bones:
                continue
            if include_bones and name not in include_bones:
                # Allow subtree if any descendant is included
                child_names = {n["name"] for n in node.get("children", []) if isinstance(n, dict)}
                if not (child_names & include_bones):
                    walk_emit(node.get("children", []))
                    continue
            emit_bone(name)
            walk_emit(node.get("children", []))

    walk_emit(model.get("outliner", []))

    vb_w, vb_h, vb_off = compute_visible_bounds(all_origins, all_sizes)
    geometry = {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": identifier,
                    "texture_width": tex_w,
                    "texture_height": tex_h,
                    "visible_bounds_width": round(vb_w, 2),
                    "visible_bounds_height": round(vb_h, 2),
                    "visible_bounds_offset": [round(v, 3) for v in vb_off],
                },
                "bones": geo_bones,
            }
        ],
    }
    return geometry, bone_order, bones_map


# ---------------------------------------------------------------------------
# Animation conversion
# ---------------------------------------------------------------------------

def build_uuid_to_bone(model: dict[str, Any]) -> dict[str, str]:
    mapping: dict[str, str] = {}

    def walk(nodes: list[Any]) -> None:
        for node in nodes:
            if isinstance(node, str):
                continue
            mapping[node["uuid"]] = node["name"]
            walk(node.get("children", []))

    walk(model.get("outliner", []))
    return mapping


def convert_channel_values(channel: str, values: list[Any]) -> list[Any]:
    if channel == "rotation":
        return flip_rotation_z(values)
    if channel == "position":
        return flip_position_z(values)
    return values


def convert_bbmodel_animations(
    model: dict[str, Any],
    name_map: dict[str, str] | None = None,
) -> dict[str, dict[str, Any]]:
    """Convert bbmodel animation list to Bedrock animation dict keyed by original name."""
    uuid_to_bone = build_uuid_to_bone(model)
    anims_in = model.get("animations", [])
    if isinstance(anims_in, dict):
        return anims_in

    out: dict[str, dict[str, Any]] = {}
    for anim in anims_in:
        anim_name = anim.get("name", "unnamed")
        target_name = name_map.get(anim_name, anim_name) if name_map else anim_name
        length = float(anim.get("length", 0) or 0)
        loop_mode = anim.get("loop", "once")
        loop_val: Any = True
        if loop_mode in ("once", "hold"):
            loop_val = "hold_on_last_frame" if loop_mode == "hold" else False
        elif loop_mode in ("loop", True):
            loop_val = True

        bone_tracks: dict[str, dict[str, dict[str, list[Any]]]] = {}
        for uid, animator in anim.get("animators", {}).items():
            bone_name = uuid_to_bone.get(uid)
            if not bone_name:
                continue
            for kf in animator.get("keyframes", []):
                channel = kf.get("channel", "rotation")
                time_key = fmt_time(float(kf.get("time", 0)))
                data_points = kf.get("data_points") or ([kf["data_point"]] if "data_point" in kf else [])
                if not data_points:
                    continue
                values = convert_channel_values(channel, vec3_from_datapoint(data_points[0]))
                bone_tracks.setdefault(bone_name, {}).setdefault(channel, {})[time_key] = values

        entry: dict[str, Any] = {"bones": bone_tracks}
        if length > 0:
            entry["animation_length"] = length
        if loop_val is not False:
            entry["loop"] = loop_val
        out[target_name] = entry
    return out


def merge_animation_bones(base: dict[str, Any], extra_bones: dict[str, Any]) -> dict[str, Any]:
    merged = dict(base)
    bones = merged.setdefault("bones", {})
    for bone, channels in extra_bones.items():
        for channel, keys in channels.items():
            bones.setdefault(bone, {}).setdefault(channel, {}).update(keys)
    return merged


# ---------------------------------------------------------------------------
# Procedural animations
# ---------------------------------------------------------------------------

def procedural_box_animations() -> dict[str, dict[str, Any]]:
    return {
        "animation.verity_box.idle": {"loop": True, "bones": {}},
        "animation.verity_box.rustle_small": {
            "loop": False,
            "animation_length": 0.6,
            "bones": {
                "root": {
                    "rotation": {
                        "0.0": [0, 0, 0],
                        "0.15": [0, 0, 2.5],
                        "0.3": [0, 0, -2.5],
                        "0.45": [0, 0, 1.5],
                        "0.6": [0, 0, 0],
                    }
                },
                "cardboard": {
                    "rotation": {
                        "0.0": [0, 0, 0],
                        "0.2": [1.5, 0, 0],
                        "0.4": [-1.0, 0, 0],
                        "0.6": [0, 0, 0],
                    }
                },
            },
        },
        "animation.verity_box.knock": {
            "loop": False,
            "animation_length": 0.35,
            "bones": {
                "root": {
                    "position": {
                        "0.0": [0, 0, 0],
                        "0.08": [0, 0, -1.5],
                        "0.2": [0, 0, 0.5],
                        "0.35": [0, 0, 0],
                    },
                    "rotation": {
                        "0.0": [0, 0, 0],
                        "0.08": [3, 0, 0],
                        "0.35": [0, 0, 0],
                    },
                }
            },
        },
        "animation.verity_box.shift": {
            "loop": False,
            "animation_length": 0.5,
            "bones": {
                "root": {
                    "position": {
                        "0.0": [0, 0, 0],
                        "0.2": [2, 0, 0],
                        "0.5": [0, 0, 0],
                    }
                }
            },
        },
        "animation.verity_box.interaction_reaction": {
            "loop": False,
            "animation_length": 0.8,
            "bones": {
                "root": {
                    "rotation": {
                        "0.0": [0, 0, 0],
                        "0.1": [0, 0, 4],
                        "0.25": [0, 0, -4],
                        "0.4": [0, 0, 3],
                        "0.55": [0, 0, -2],
                        "0.8": [0, 0, 0],
                    },
                    "position": {
                        "0.0": [0, 0, 0],
                        "0.15": [0, 0.5, 0],
                        "0.8": [0, 0, 0],
                    },
                },
                "left_lid": {
                    "rotation": {
                        "0.0": [0, 0, 0],
                        "0.2": [0, 0, 8],
                        "0.8": [0, 0, 0],
                    }
                },
                "right_lid": {
                    "rotation": {
                        "0.0": [0, 0, 0],
                        "0.2": [0, 0, -8],
                        "0.8": [0, 0, 0],
                    }
                },
            },
        },
    }


def procedural_verity_animations() -> dict[str, dict[str, Any]]:
    return {
        "animation.verity.idle": {
            "loop": True,
            "animation_length": 2.0,
            "bones": {
                "root": {
                    "position": {
                        "0.0": [0, 0, 0],
                        "1.0": [0, 0.35, 0],
                        "2.0": [0, 0, 0],
                    },
                    "rotation": {
                        "0.0": [0, 0, 0],
                        "1.0": [0, 3, 0],
                        "2.0": [0, 0, 0],
                    },
                },
                "body": {
                    "rotation": {
                        "0.0": [0, 0, 0],
                        "1.0": [0, -2, 0],
                        "2.0": [0, 0, 0],
                    }
                },
            },
        },
        "animation.verity.reveal": {
            "loop": False,
            "animation_length": 1.2,
            "bones": {
                "root": {
                    "scale": {
                        "0.0": [0.01, 0.01, 0.01],
                        "0.4": [1.15, 1.15, 1.15],
                        "0.7": [0.95, 0.95, 0.95],
                        "1.2": [1, 1, 1],
                    },
                    "position": {
                        "0.0": [0, -2, 0],
                        "0.4": [0, 1, 0],
                        "1.2": [0, 0, 0],
                    },
                },
                "face_idle": {
                    "scale": {
                        "0.0": [0, 0, 0],
                        "0.5": [1, 1, 1],
                    }
                },
            },
        },
    }


# ---------------------------------------------------------------------------
# Entity-specific pipelines
# ---------------------------------------------------------------------------

def convert_verity_box(
    bbmodel_path: Path,
    texture_fallback: Path,
    existing_anim_path: Path | None,
    geo_out: Path,
    anim_out: Path,
    texture_out: Path,
) -> dict[str, Any]:
    model = load_bbmodel(bbmodel_path)
    geo, bones, _ = build_geometry(model, "geometry.verity_box")
    save_json(geo_out, geo)

    tex_entry = find_texture_by_name(model, "cardboardboxtexture.png") or (
        model.get("textures", [{}])[0] if model.get("textures") else {}
    )
    extract_texture(tex_entry, texture_fallback, texture_out)

    animations: dict[str, dict[str, Any]] = {"format_version": "1.8.0", "animations": {}}

    # Prefer already-exported Bedrock animation for shake if present.
    if existing_anim_path and existing_anim_path.is_file():
        with existing_anim_path.open("r", encoding="utf-8") as fh:
            existing = json.load(fh)
        for key, val in existing.get("animations", {}).items():
            new_key = key.replace("animation.model.", "animation.verity_box.")
            animations["animations"][new_key] = val

    converted = convert_bbmodel_animations(
        model,
        {
            "animation.model.shake": "animation.verity_box.shake",
            "animation.model.open": "animation.verity_box.open",
        },
    )
    bb_anims = convert_bbmodel_animations(model)
    for src_name, target in [
        ("animation.model.shake", "animation.verity_box.shake"),
        ("animation.model.open", "animation.verity_box.open"),
    ]:
        if target in animations["animations"]:
            continue
        raw = converted.get(target) or converted.get(src_name) or bb_anims.get(src_name)
        if raw:
            animations["animations"][target] = raw

    animations["animations"].update(procedural_box_animations())
    save_json(anim_out, animations)

    return {"bones": bones, "animations": list(animations["animations"].keys())}


def remap_verity_ball_outliner(model: dict[str, Any]) -> dict[str, Any]:
    """
    Produce a simplified outliner for the GeckoLib entity:
      root -> body (was sphere), face_idle, face_speak
    Cube elements stay mapped via synthetic outliner.
    """
    element_map = build_element_map(model)
    # Clone model shallowly with trimmed outliner
    trimmed = json.loads(json.dumps(model))
    mouth_idle = "56db7049-33fc-7c42-b89d-7d06d79c30a7"
    mouth_speak = "903c5ea7-4f4b-2f3f-606d-26cae12b86ae"
    mesh_uuid = "3473e1e5-f79c-d2f1-7f55-ba4f2aa939e0"

    root_origin = [0, 5.5, 0]
    for node in trimmed.get("outliner", []):
        if isinstance(node, dict) and node.get("name") == "root":
            root_origin = list(node.get("origin", root_origin))
            break

    trimmed["outliner"] = [
        {
            "name": "root",
            "origin": root_origin,
            "uuid": "verity-root",
            "children": [
                {
                    "name": "body",
                    "origin": [0, 5.5, 0],
                    "uuid": "verity-body",
                    "children": [mesh_uuid],
                },
                {
                    "name": "face_idle",
                    "origin": root_origin,
                    "uuid": "verity-face-idle",
                    "children": [mouth_idle],
                },
                {
                    "name": "face_speak",
                    "origin": root_origin,
                    "uuid": "verity-face-speak",
                    "children": [mouth_speak],
                },
            ],
        }
    ]
    return trimmed


def convert_verity_ball(
    bbmodel_path: Path,
    texture_dir: Path,
    geo_out: Path,
    anim_out: Path,
    texture_out: Path,
    face_idle_out: Path,
    face_speak_out: Path,
) -> dict[str, Any]:
    model = load_bbmodel(bbmodel_path)
    remapped = remap_verity_ball_outliner(model)

    geo, bones, _ = build_geometry(
        remapped,
        "geometry.verity",
        skip_bones={"box", "cardboard", "lids", "left_lid", "right_lid", "LeftItemPivot", "RightItemPivot",
                    "mouth_closed_sus", "mouth_open_sus", "mouth_closed_bored"},
        mesh_bones={"body"},
        mesh_target_size=8.0,
        mesh_center_y=5.5,
    )
    save_json(geo_out, geo)

    # Textures
    base_tex = find_texture_by_name(model, "verity_base_texture.png")
    idle_tex = find_texture_by_name(model, "idle.png")
    speak_tex = find_texture_by_name(model, "speak.png")

    fb_base = texture_dir / "verity_base_texture.png"
    fb_idle = texture_dir / "idle.png"
    fb_speak = texture_dir / "speak.png"

    extract_texture(base_tex or {}, fb_base, texture_out)
    extract_texture(idle_tex or {}, fb_idle, face_idle_out)
    extract_texture(speak_tex or {}, fb_speak, face_speak_out)

    # Animations: talk from bbmodel, idle/reveal procedural
    converted = convert_bbmodel_animations(model)
    talk_src = converted.get("talk", {})
    talk_bones: dict[str, Any] = {}
    if talk_src.get("bones"):
        mapping = {
            "mouth_closed_idle": "face_idle",
            "mouth_open_idle": "face_speak",
            "root": "root",
            "sphere": "body",
        }
        for src_bone, channels in talk_src["bones"].items():
            dst = mapping.get(src_bone, src_bone)
            if dst.startswith("mouth_") or dst == "box":
                continue
            talk_bones[dst] = channels
        for src_bone, channels in talk_src["bones"].items():
            dst = mapping.get(src_bone)
            if dst in ("face_idle", "face_speak"):
                talk_bones[dst] = channels

    animations: dict[str, Any] = {
        "format_version": "1.8.0",
        "animations": {
            **procedural_verity_animations(),
            "animation.verity.talk": {
                "loop": True,
                "animation_length": talk_src.get("animation_length", 0.875),
                "bones": talk_bones
                or {
                    "face_idle": {
                        "scale": {
                            "0.0": [1, 1, 1],
                            "0.17": [0, 0, 0],
                            "0.33": [1, 1, 1],
                            "0.54": [0, 0, 0],
                            "0.71": [1, 1, 1],
                        }
                    },
                    "face_speak": {
                        "scale": {
                            "0.0": [0, 0, 0],
                            "0.17": [1, 1, 1],
                            "0.33": [0, 0, 0],
                            "0.54": [1, 1, 1],
                            "0.71": [0, 0, 0],
                        }
                    },
                    "root": {
                        "position": {
                            "0.0": [0, 0, 0],
                            "0.25": [0, 0.2, 0],
                            "0.875": [0, 0, 0],
                        }
                    },
                },
            },
            "animation.verity.greeting": {
                "loop": False,
                "animation_length": 0.75,
                "bones": {
                    "root": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "0.25": [0, 15, 0],
                            "0.5": [0, -10, 0],
                            "0.75": [0, 0, 0],
                        },
                        "position": {
                            "0.0": [0, 0, 0],
                            "0.25": [0, 0.5, 0],
                            "0.75": [0, 0, 0],
                        },
                    },
                    "face_idle": {"scale": {"0.0": [1, 1, 1]}},
                    "face_speak": {
                        "scale": {
                            "0.0": [0, 0, 0],
                            "0.15": [1, 1, 1],
                            "0.35": [0, 0, 0],
                            "0.55": [1, 1, 1],
                            "0.75": [0, 0, 0],
                        }
                    },
                },
            },
        },
    }
    save_json(anim_out, animations)

    return {"bones": bones, "animations": list(animations["animations"].keys())}


def copy_provided_assets(
    box_bbmodel: Path,
    box_texture: Path,
    box_anim: Path,
    ball_bbmodel: Path,
    ball_texture_dir: Path,
    dest_root: Path,
) -> list[str]:
    copied: list[str] = []
    pairs = [
        (box_bbmodel, dest_root / "cardboard_box" / box_bbmodel.name),
        (box_texture, dest_root / "cardboard_box" / box_texture.name),
        (box_anim, dest_root / "cardboard_box" / box_anim.name),
        (ball_bbmodel, dest_root / "verity_ball" / ball_bbmodel.name),
    ]
    for src, dst in pairs:
        if src.is_file():
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dst)
            copied.append(str(dst))

    if ball_texture_dir.is_dir():
        for png in ball_texture_dir.glob("*.png"):
            dst = dest_root / "verity_ball" / png.name
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(png, dst)
            copied.append(str(dst))
    return copied


def suggest_hitbox(model: dict[str, Any], skip_bones: set[str] | None = None) -> dict[str, float]:
    skip_bones = skip_bones or set()
    element_map = build_element_map(model)
    _, element_parent = flatten_bones(model.get("outliner", []), element_map)
    min_x = min_y = min_z = float("inf")
    max_x = max_y = max_z = float("-inf")

    for uuid, element in element_map.items():
        parent = element_parent.get(uuid)
        if not parent or parent in skip_bones:
            continue
        if element.get("visibility") is False:
            continue
        if element.get("type") == "mesh":
            from_p, to_p = mesh_bounds(element)
        elif element.get("type") in (None, "cube"):
            from_p, to_p = element["from"], element["to"]
        else:
            continue
        min_x = min(min_x, from_p[0])
        min_y = min(min_y, from_p[1])
        min_z = min(min_z, from_p[2])
        max_x = max(max_x, to_p[0])
        max_y = max(max_y, to_p[1])
        max_z = max(max_z, to_p[2])

    if min_x == float("inf"):
        return {"width": 0.6, "height": 0.6, "depth": 0.6}

    # Blockbench units are pixels; 16 px = 1 block
    w = (max_x - min_x) / 16.0
    h = (max_y - min_y) / 16.0
    d = (max_z - min_z) / 16.0
    return {
        "width": round(max(w, 0.25), 2),
        "height": round(max(h, 0.25), 2),
        "depth": round(max(d, 0.25), 2),
        "min_y_blocks": round(min_y / 16.0, 2),
        "max_y_blocks": round(max_y / 16.0, 2),
    }


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def default_paths() -> dict[str, Path]:
    root = Path(__file__).resolve().parents[1]
    desktop = Path.home() / "Desktop"
    # Prefer authoritative V1.3 ball model for GeckoLib export.
    ball_v13 = desktop / "V1.3 Verity Ball Model Figura Avatar/verity_ball_model.bbmodel"
    ball_v15 = desktop / "V1.5 Verity Models Figura Avatars/Verity Ball Model [Figura Avatar COPverity_ball_model.bbmodel"
    ball_bbmodel = ball_v13 if ball_v13.is_file() else ball_v15
    return {
        "root": root,
        "box_bbmodel": desktop / "Verity Assets/cardboard_box_model.bbmodel",
        "box_texture": desktop / "Verity Assets/cardboardboxtexture.png",
        "box_anim": desktop / "Verity Assets/cardboard.animation.json",
        "ball_bbmodel": ball_bbmodel,
        "ball_textures": desktop / "V1.3 Verity Ball Model Figura Avatar",
        "geo_box": root / "src/main/resources/assets/universe_verity/geo/verity_box.geo.json",
        "geo_verity": root / "src/main/resources/assets/universe_verity/geo/verity.geo.json",
        "anim_box": root / "src/main/resources/assets/universe_verity/animations/verity_box.animation.json",
        "anim_verity": root / "src/main/resources/assets/universe_verity/animations/verity.animation.json",
        "tex_box": root / "src/main/resources/assets/universe_verity/textures/entity/verity_box.png",
        "tex_verity": root / "src/main/resources/assets/universe_verity/textures/entity/verity.png",
        "tex_face_idle": root / "src/main/resources/assets/universe_verity/textures/entity/verity_face_idle.png",
        "tex_face_speak": root / "src/main/resources/assets/universe_verity/textures/entity/verity_face_speak.png",
        "provided": root / "provided_assets",
    }


def main(argv: list[str] | None = None) -> int:
    paths = default_paths()
    parser = argparse.ArgumentParser(description="Convert Blockbench bbmodel to GeckoLib assets")
    parser.add_argument("--box-bbmodel", type=Path, default=paths["box_bbmodel"])
    parser.add_argument("--ball-bbmodel", type=Path, default=paths["ball_bbmodel"])
    parser.add_argument("--ball-textures", type=Path, default=paths["ball_textures"])
    parser.add_argument("--root", type=Path, default=paths["root"])
    args = parser.parse_args(argv)

    root = args.root
    geo_box = root / "src/main/resources/assets/universe_verity/geo/verity_box.geo.json"
    geo_verity = root / "src/main/resources/assets/universe_verity/geo/verity.geo.json"
    anim_box = root / "src/main/resources/assets/universe_verity/animations/verity_box.animation.json"
    anim_verity = root / "src/main/resources/assets/universe_verity/animations/verity.animation.json"
    tex_box = root / "src/main/resources/assets/universe_verity/textures/entity/verity_box.png"
    tex_verity = root / "src/main/resources/assets/universe_verity/textures/entity/verity.png"
    tex_face_idle = root / "src/main/resources/assets/universe_verity/textures/entity/verity_face_idle.png"
    tex_face_speak = root / "src/main/resources/assets/universe_verity/textures/entity/verity_face_speak.png"

    box_texture = paths["box_texture"]
    box_anim = paths["box_anim"]

    print("Converting Verity box...")
    box_result = convert_verity_box(
        args.box_bbmodel,
        box_texture,
        box_anim,
        geo_box,
        anim_box,
        tex_box,
    )

    print("Converting Verity ball...")
    ball_result = convert_verity_ball(
        args.ball_bbmodel,
        args.ball_textures,
        geo_verity,
        anim_verity,
        tex_verity,
        tex_face_idle,
        tex_face_speak,
    )

    print("Copying provided_assets...")
    copied = copy_provided_assets(
        args.box_bbmodel,
        box_texture,
        box_anim,
        args.ball_bbmodel,
        args.ball_textures,
        root / "provided_assets",
    )

    box_model = load_bbmodel(args.box_bbmodel)
    ball_model = load_bbmodel(args.ball_bbmodel)
    box_hitbox = suggest_hitbox(box_model)
    ball_hitbox = suggest_hitbox(
        ball_model,
        skip_bones={"box", "cardboard", "lids", "left_lid", "right_lid", "LeftItemPivot", "RightItemPivot"},
    )

    written = [
        str(geo_box),
        str(geo_verity),
        str(anim_box),
        str(anim_verity),
        str(tex_box),
        str(tex_verity),
        str(tex_face_idle),
        str(tex_face_speak),
    ]

    report = {
        "written_files": written,
        "provided_assets_copied": copied,
        "verity_box_bones": box_result["bones"],
        "verity_bones": ball_result["bones"],
        "verity_box_animations": box_result["animations"],
        "verity_animations": ball_result["animations"],
        "suggested_hitboxes": {
            "verity_box": box_hitbox,
            "verity": ball_hitbox,
        },
        "limitations": [
            "Figura mesh sphere (762 verts) replaced with 1 main 8x8x8 cube + 6 edge filler cubes.",
            "Face expression variants (sus, bored) omitted; only face_idle and face_speak exported.",
            "Figura accessory bones (box, item pivots) excluded from Verity entity geo.",
            "Bedrock Z-axis mirroring applied to pivots/origins; numeric rotation Z values mirrored.",
            "Molang expression keyframes preserved as strings for GeckoLib runtime evaluation.",
        ],
    }

    report_path = root / "tools/conversion_report.json"
    save_json(report_path, report)

    print(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
