# Talk slides (Marp)

- **Deck:** [building-ukulele-companion.md](building-ukulele-companion.md) — process-focused narrative (Android spine, iOS + shipping).

## Preview and export

**VS Code:** install [Marp for VS Code](https://marketplace.visualstudio.com/items?itemName=marp-team.marp-vscode). This repo includes [`.vscode/settings.json`](../../.vscode/settings.json) so the custom theme **`ukulele`** loads from [theme/ukulele-companion.css](theme/ukulele-companion.css). Open the deck, preview, and **Export Slide Deck** (PDF, HTML, PPTX).

**CLI** (from repo root):

```bash
npx @marp-team/marp-cli@latest --no-stdin docs/talk/building-ukulele-companion.md \
  --allow-local-files \
  --theme-set docs/talk/theme/ukulele-companion.css \
  -o docs/talk/building-ukulele-companion.pdf
```

- `--no-stdin` avoids the CLI waiting for piped input when run from some terminals.
- `--allow-local-files` is needed for images such as `../app-icon.svg`.
- `--theme-set` registers the **ukulele** theme (`theme: ukulele` in the deck frontmatter).

## Optional images

Place talk-specific screenshots in [assets/](assets/). The deck’s appendix slide lists suggested filenames.
