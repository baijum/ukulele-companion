#!/usr/bin/env python3
"""Assemble a narrated video from a TOML project file.

Reads a project.toml that describes scenes with video clips and narration text,
generates missing voiceover audio via OpenAI TTS, pads clips to match narration
duration, concatenates scenes, and adds branding jingles.

Usage:
    python3 scripts/assemble_video.py docs/videos/explorer/project.toml
    python3 scripts/assemble_video.py docs/videos/explorer/project.toml --audio-only

Requires:
    - ffmpeg on PATH
    - OPENAI_API_KEY environment variable (source ~/.secrets)
    - pip install openai
"""

import argparse
import subprocess
import sys
import tomllib
from pathlib import Path


def get_duration(path: Path) -> float:
    result = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "csv=p=0", str(path)],
        capture_output=True, text=True,
    )
    try:
        return float(result.stdout.strip())
    except ValueError:
        return 0.0


def generate_audio(scenes: list, project_dir: Path, voiceover: dict) -> None:
    from openai import OpenAI
    client = OpenAI()

    model = voiceover.get("model", "tts-1-hd")
    voice = voiceover.get("voice", "nova")

    for scene in scenes:
        audio_path = project_dir / scene["audio"]
        if audio_path.exists():
            print(f"  Exists: {audio_path.name}")
            continue

        audio_path.parent.mkdir(parents=True, exist_ok=True)
        print(f"  Generating: {scene['name']}...")
        resp = client.audio.speech.create(
            model=model, voice=voice,
            input=scene["narration"].strip(),
        )
        resp.stream_to_file(str(audio_path))


def print_durations(scenes: list, project_dir: Path) -> None:
    print("\n  Required clip durations (audio + delay + 2s buffer):")
    print(f"  {'Scene':<25} {'Audio':>7} {'Delay':>6} {'Min Clip':>9}")
    print(f"  {'-'*25} {'-'*7} {'-'*6} {'-'*9}")

    for scene in scenes:
        audio_path = project_dir / scene["audio"]
        audio_dur = get_duration(audio_path)
        delay = scene.get("delay", 1.0)
        min_dur = audio_dur + delay + 2.0
        print(f"  {scene['name']:<25} {audio_dur:>6.1f}s {delay:>5.1f}s {min_dur:>8.1f}s")


def merge_scene(clip: Path, audio: Path, delay_s: float, volume: float,
                output: Path) -> None:
    clip_dur = get_duration(clip)
    audio_dur = get_duration(audio)
    delay_ms = int(delay_s * 1000)
    target_dur = audio_dur + delay_s
    pad_dur = target_dur - clip_dur

    if pad_dur > 0:
        cmd = [
            "ffmpeg", "-y", "-i", str(clip), "-i", str(audio),
            "-filter_complex",
            f"[0:v]tpad=stop_mode=clone:stop_duration={pad_dur:.3f}[vpad];"
            f"[1:a]adelay={delay_ms}|{delay_ms},volume={volume}[aout]",
            "-map", "[vpad]", "-map", "[aout]",
            "-c:v", "libx264", "-preset", "ultrafast", "-crf", "23",
            "-c:a", "aac", "-b:a", "192k",
            "-shortest", str(output),
        ]
    else:
        cmd = [
            "ffmpeg", "-y", "-i", str(clip), "-i", str(audio),
            "-filter_complex",
            f"[1:a]adelay={delay_ms}|{delay_ms},volume={volume}[aout]",
            "-map", "0:v", "-map", "[aout]",
            "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
            "-shortest", str(output),
        ]

    subprocess.run(cmd, capture_output=True)
    merged_dur = get_duration(output)
    pad_label = f" pad={pad_dur:.1f}s" if pad_dur > 0 else ""
    print(f"  {output.name}: clip={clip_dur:.1f}s audio={audio_dur:.1f}s{pad_label} -> {merged_dur:.1f}s")


def concatenate_scenes(scene_files: list[Path], output: Path) -> None:
    inputs = []
    filter_labels = []
    for i, sf in enumerate(scene_files):
        inputs.extend(["-i", str(sf)])
        filter_labels.append(f"[{i}:v][{i}:a]")

    n = len(scene_files)
    concat_filter = f"{''.join(filter_labels)}concat=n={n}:v=1:a=1[vout][aout]"

    cmd = ["ffmpeg", "-y"] + inputs + [
        "-filter_complex", concat_filter,
        "-map", "[vout]", "-map", "[aout]",
        "-c:v", "libx264", "-preset", "ultrafast", "-crf", "23",
        "-c:a", "aac", "-b:a", "192k",
        str(output),
    ]
    subprocess.run(cmd, capture_output=True)


def add_jingles(assembled: Path, intro: Path, outro: Path, output: Path) -> None:
    inputs = []
    filter_parts = []
    concat_labels = []
    idx = 0

    if intro and intro.exists():
        inputs.extend(["-i", str(intro)])
        filter_parts.append(
            f"[{idx}:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=mono[intro]")
        concat_labels.append("[intro]")
        idx += 1

    inputs.extend(["-i", str(assembled)])
    filter_parts.append(
        f"[{idx}:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=mono[main]")
    video_idx = idx
    concat_labels.append("[main]")
    idx += 1

    if outro and outro.exists():
        inputs.extend(["-i", str(outro)])
        filter_parts.append(
            f"[{idx}:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=mono[outro]")
        concat_labels.append("[outro]")
        idx += 1

    n = len(concat_labels)
    filter_str = ";".join(filter_parts)
    filter_str += f";{''.join(concat_labels)}concat=n={n}:v=0:a=1[aout]"

    cmd = ["ffmpeg", "-y"] + inputs + [
        "-filter_complex", filter_str,
        "-map", f"{video_idx}:v", "-map", "[aout]",
        "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
        str(output),
    ]
    subprocess.run(cmd, capture_output=True)


def main():
    parser = argparse.ArgumentParser(description="Assemble video from TOML project")
    parser.add_argument("project", help="Path to project.toml")
    parser.add_argument("--audio-only", action="store_true",
                        help="Generate audio only, print required clip durations")
    args = parser.parse_args()

    project_path = Path(args.project)
    if not project_path.exists():
        print(f"Error: {project_path} not found")
        sys.exit(1)

    project_dir = project_path.parent
    with open(project_path, "rb") as f:
        config = tomllib.load(f)

    voiceover = config.get("voiceover", {})
    branding = config.get("branding", {})
    scenes = config["scene"]
    volume = voiceover.get("volume_boost", 3.0)
    output = project_dir / config["project"]["output"]

    print(f"Project: {config['project']['title']}")
    print(f"Scenes: {len(scenes)}")

    # Step 1: Generate missing audio
    print("\n[1/4] Generating voiceover audio...")
    generate_audio(scenes, project_dir, voiceover)

    # Print durations
    print_durations(scenes, project_dir)

    if args.audio_only:
        print("\n--audio-only: Skipping video assembly.")
        print("Record clips with the durations above, then re-run without --audio-only.")
        return

    # Step 2: Merge each scene (pad if needed)
    print("\n[2/4] Merging scenes (padding clips to match narration)...")
    scene_files = []
    for i, scene in enumerate(scenes):
        clip = project_dir / scene["video"]
        audio = project_dir / scene["audio"]

        if not clip.exists():
            print(f"  ERROR: Missing clip: {clip}")
            sys.exit(1)

        delay_s = scene.get("delay", 1.0)
        merged = Path(f"/tmp/assemble_scene_{i:02d}.mp4")
        scene_files.append(merged)
        merge_scene(clip, audio, delay_s, volume, merged)

    # Step 3: Concatenate scenes
    print("\n[3/4] Concatenating scenes...")
    assembled = Path("/tmp/assemble_concat.mp4")
    concatenate_scenes(scene_files, assembled)
    assembled_dur = get_duration(assembled)
    print(f"  Assembled: {assembled_dur:.1f}s")

    # Step 4: Add branding jingles
    output.parent.mkdir(parents=True, exist_ok=True)
    intro = project_dir / branding["intro"] if branding.get("intro") else None
    outro = project_dir / branding["outro"] if branding.get("outro") else None

    if intro or outro:
        print("\n[4/4] Adding jingles...")
        add_jingles(assembled, intro, outro, output)
    else:
        print("\n[4/4] No branding, copying assembled video...")
        import shutil
        shutil.copy2(assembled, output)

    # Report
    duration = get_duration(output)
    size_mb = output.stat().st_size / (1024 * 1024)
    minutes = int(duration // 60)
    seconds = int(duration % 60)
    print(f"\nDone! {output}")
    print(f"Duration: {duration:.1f}s ({minutes}:{seconds:02d})")
    print(f"Size: {size_mb:.1f} MB")


if __name__ == "__main__":
    main()
