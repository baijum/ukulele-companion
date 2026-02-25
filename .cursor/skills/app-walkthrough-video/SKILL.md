---
name: app-walkthrough-video
description: "[RETIRED] This skill has been replaced by the TOML video project system. Use /assemble-video and /record-clips instead."
---

# App Walkthrough Video (Retired)

**This skill has been replaced by the TOML video project system.**

To create a walkthrough video:

1. Create a `project.toml` in `docs/videos/walkthrough/` with scenes covering each app screen
2. Use `/record-clips` to record the scene clips
3. Use `/assemble-video` to generate the final narrated video with jingles

See existing projects for reference:
- `docs/videos/explorer/project.toml`
- `docs/videos/tuner/project.toml`

## Related Skills

- `/assemble-video` -- Assemble video from TOML project
- `/record-clips` -- Record scene clips for a project
