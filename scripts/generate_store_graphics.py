#!/usr/bin/env python3
"""Generate Play Store graphics from the app icon.

Uses Nunito Sans (Google Fonts, OFL license). Font files are
auto-downloaded on first run and cached under .font-cache/.
"""

import math
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

PROJECT_ROOT = Path(__file__).resolve().parent.parent
ICON_PATH = PROJECT_ROOT / "docs" / "app-icon-512.png"
OUTPUT_DIR = PROJECT_ROOT / "store-listing"
FONT_CACHE = PROJECT_ROOT / ".font-cache"

FONT_FILES = {
    "NunitoSans-Bold.ttf": "https://fonts.gstatic.com/s/nunitosans/v19/pe1mMImSLYBIv1o4X1M8ce2xCx3yop4tQpF_MeTm0lfGWVpNn64CL7U8upHZIbMV51Q42ptCp5F5bxqqtQ1yiU4GMS5ntA.ttf",
    "NunitoSans-SemiBold.ttf": "https://fonts.gstatic.com/s/nunitosans/v19/pe1mMImSLYBIv1o4X1M8ce2xCx3yop4tQpF_MeTm0lfGWVpNn64CL7U8upHZIbMV51Q42ptCp5F5bxqqtQ1yiU4GCC5ntA.ttf",
    "NunitoSans-Regular.ttf": "https://fonts.gstatic.com/s/nunitosans/v19/pe1mMImSLYBIv1o4X1M8ce2xCx3yop4tQpF_MeTm0lfGWVpNn64CL7U8upHZIbMV51Q42ptCp5F5bxqqtQ1yiU4G1ilntA.ttf",
}

BG_DARK = (15, 76, 107)
BG_MID = (22, 105, 148)
BG_LIGHT = (35, 140, 185)
WHITE = (255, 255, 255)
WHITE_SEMI = (220, 230, 235)


def ensure_fonts() -> dict[str, Path]:
    """Download Nunito Sans if not cached. Returns dict of weight -> path."""
    FONT_CACHE.mkdir(exist_ok=True)
    paths = {}
    for name, url in FONT_FILES.items():
        dest = FONT_CACHE / name
        if not dest.exists():
            print(f"  Downloading {name}...")
            urllib.request.urlretrieve(url, dest)
        weight = name.split("-")[1].split(".")[0]  # e.g. "Bold"
        paths[weight] = dest
    return paths


def load_font(font_path: Path, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(font_path), size)


def radial_gradient(size, center, color_inner, color_outer, radius_factor=1.0):
    w, h = size
    img = Image.new("RGB", size)
    pixels = img.load()
    cx, cy = center
    max_dist = math.sqrt(w * w + h * h) * radius_factor
    for y in range(h):
        for x in range(w):
            dist = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            t = min(dist / max_dist, 1.0)
            t = t * t
            r = int(color_inner[0] * (1 - t) + color_outer[0] * t)
            g = int(color_inner[1] * (1 - t) + color_outer[1] * t)
            b = int(color_inner[2] * (1 - t) + color_outer[2] * t)
            pixels[x, y] = (r, g, b)
    return img


def draw_music_notes(draw, bbox, color):
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Apple Symbols.ttf", 28)
    except OSError:
        return
    import random
    random.seed(42)
    symbols = ["♪", "♫", "♩", "♬", "♪"]
    x_start, y_start, x_end, y_end = bbox
    x_step = (x_end - x_start) // 6
    for i in range(5):
        x = x_start + x_step * (i + 1) + random.randint(-15, 15)
        y = random.randint(y_start, y_end)
        draw.text((x, y), symbols[i], fill=color, font=font)


def create_play_store_icon():
    icon = Image.open(ICON_PATH).convert("RGBA")
    icon = icon.resize((512, 512), Image.LANCZOS)
    bg = Image.new("RGB", (512, 512), BG_DARK)
    bg.paste(icon, (0, 0), icon)
    return bg


def create_feature_graphic(fonts: dict[str, Path]):
    width, height = 1024, 500

    bg = radial_gradient(
        (width, height),
        center=(width * 0.35, height * 0.45),
        color_inner=BG_MID,
        color_outer=BG_DARK,
        radius_factor=0.55,
    )

    # Subtle diagonal lines for texture
    bg_rgba = bg.convert("RGBA")
    for i in range(-10, 30):
        x_offset = i * 60
        alpha = 12 if i % 3 == 0 else 6
        ImageDraw.Draw(bg_rgba).line(
            [(x_offset, height), (x_offset + height, 0)],
            fill=(255, 255, 255, alpha),
            width=1,
        )
    bg = bg_rgba.convert("RGB")

    # Soft glow behind icon area
    glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_cx, glow_cy = 195, height // 2
    for r in range(200, 0, -2):
        a = int(18 * (1 - r / 200))
        glow_draw.ellipse(
            [glow_cx - r, glow_cy - r, glow_cx + r, glow_cy + r],
            fill=(BG_LIGHT[0], BG_LIGHT[1], BG_LIGHT[2], a),
        )
    bg_rgba = Image.alpha_composite(bg.convert("RGBA"), glow)
    bg = bg_rgba.convert("RGB")
    draw = ImageDraw.Draw(bg)

    # App icon with drop shadow
    icon = Image.open(ICON_PATH).convert("RGBA")
    icon_size = 300
    icon = icon.resize((icon_size, icon_size), Image.LANCZOS)

    shadow = Image.new("RGBA", (icon_size + 20, icon_size + 20), (0, 0, 0, 0))
    shadow.paste(Image.new("RGBA", (icon_size, icon_size), (0, 0, 0, 60)), (10, 12))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=12))

    icon_x, icon_y = 45, (height - icon_size) // 2
    bg_rgba = bg.convert("RGBA")
    bg_rgba.paste(shadow, (icon_x - 10, icon_y - 4), shadow)
    bg_rgba.paste(icon, (icon_x, icon_y), icon)
    bg = bg_rgba.convert("RGB")
    draw = ImageDraw.Draw(bg)

    # Text
    text_x = icon_x + icon_size + 55

    title_font = load_font(fonts["Bold"], 64)
    sub_font = load_font(fonts["SemiBold"], 25)
    tag_font = load_font(fonts["Regular"], 19)

    title1 = "Ukulele"
    title2 = "Companion"
    subtitle = "Chords  ·  Scales  ·  Theory  ·  Songs"
    tagline = "Free  ·  Fully Offline  ·  Accessible"

    t1_h = draw.textbbox((0, 0), title1, font=title_font)[3]
    t2_h = draw.textbbox((0, 0), title2, font=title_font)[3]
    sub_h = draw.textbbox((0, 0), subtitle, font=sub_font)[3]
    tag_h = draw.textbbox((0, 0), tagline, font=tag_font)[3]

    total_h = t1_h + 8 + t2_h + 28 + sub_h + 16 + tag_h
    y = (height - total_h) // 2

    draw.text((text_x, y), title1, fill=WHITE, font=title_font)
    y += t1_h + 8
    draw.text((text_x, y), title2, fill=WHITE, font=title_font)
    y += t2_h + 28
    draw.text((text_x, y), subtitle, fill=WHITE, font=sub_font)
    y += sub_h + 16
    draw.text((text_x, y), tagline, fill=WHITE_SEMI, font=tag_font)

    draw_music_notes(draw, (width - 140, 40, width - 20, height - 40), (255, 255, 255))

    return bg


def main():
    OUTPUT_DIR.mkdir(exist_ok=True)

    print("Ensuring Nunito Sans font is cached...")
    fonts = ensure_fonts()

    print("Generating Play Store App Icon (512×512)...")
    icon = create_play_store_icon()
    icon_out = OUTPUT_DIR / "play-store-icon-512.png"
    icon.save(icon_out, "PNG")
    print(f"  → {icon_out.relative_to(PROJECT_ROOT)}")

    print("Generating Feature Graphic (1024×500)...")
    fg = create_feature_graphic(fonts)
    fg_out = OUTPUT_DIR / "feature-graphic-1024x500.png"
    fg.save(fg_out, "PNG")
    print(f"  → {fg_out.relative_to(PROJECT_ROOT)}")

    print("\nDone!")


if __name__ == "__main__":
    main()
