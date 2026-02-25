---
name: feature-detail-video
description: "[RETIRED] This skill has been replaced by the TOML video project system. Use /assemble-video and /record-clips instead."
---

# Feature Detail Video (Retired)

**This skill has been replaced by the TOML video project system.**

To create a feature detail video:

1. Create a `project.toml` in `docs/videos/<feature>/` with scenes covering the feature
2. Run `python3 scripts/assemble_video.py project.toml --audio-only` to generate audio and see required clip durations
3. Use `/record-clips` to record the scene clips
4. Run `python3 scripts/assemble_video.py project.toml` to assemble the final video

See existing projects for reference:
- `docs/videos/explorer/project.toml` (6 scenes)
- `docs/videos/tuner/project.toml` (5 scenes)

## Related Skills

- `/assemble-video` -- Assemble video from TOML project
- `/record-clips` -- Record scene clips for a project
