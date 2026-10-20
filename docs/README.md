# Report: Markdown to one PDF

Write the report as separate Markdown files and build **one PDF** with Pandoc and XeLaTeX. Extract this package at the root of the existing repository. It only adds `docs/`; the application folders remain as they are.

## Requirements

- Pandoc 3.x
- A full TeX Live or MiKTeX installation with `xelatex`
- TeX Gyre Termes and TeX Gyre Heros fonts (included in common TeX distributions)


## Build the PDF

From the repository root, run:

```powershell
.\docs\build.ps1
```

On Linux or macOS:

```sh
sh docs/build.sh
```

The output is `docs/output/IIP_BLE_Booking_System.pdf`. Rebuild it after editing the report. Generated PDFs are ignored by Git; export the final PDF for submission or attach it to a release.

## Files

- `report/chapters/`: sections 01–09 in the order required by the HSLU report guide. Section 08 starts the appendices; add the signed project assignment there before submission.
- `report/assets/figures/`: exported images (`.png`, `.jpg`, `.svg`). Markdown and PDF references point here.
- `report/assets/diagram-sources/`: editable diagram sources (`.mmd` for Mermaid). Use the same base filename as the exported figure.
- `report/metadata.yaml`: title, authors, date, language, abstract, and layout. Replace draft details before submission.
- `report/references.bib`: sources for citations such as `[@projectAssignment2026]`.
- `report/pandoc.yaml`: PDF settings and the exact input order. Add new chapter files here; filenames alone do not determine the order.
- `report/filters/`: Pandoc filters, including internal links between chapters in the combined PDF.
- `build.ps1` and `build.sh`: build commands for Windows and Unix.

## One self-contained report

The submitted PDF must contain the material needed to understand and assess the
project. Keep each piece of report content in one authoritative location:

- Main chapters: problem, approach, architecture, important design decisions,
  implementation and evaluation. Include the diagrams and explanations needed to
  follow the project without opening another repository file.
- Appendices in the same PDF: installation and user manuals, detailed operating
  procedures, full registers, test evidence and supporting technical tables.
  Introduce and link to the relevant appendix from the main text.

For links between chapters, use a relative Markdown filename and a stable heading
anchor, for example:

```markdown
[Terminal operating details](08-appendices.md#terminal-operational-details)
```

This works when reading the Markdown files. During PDF generation,
`filters/internal-section-links.lua` turns links to included chapters into
destinations inside the PDF and rejects missing destination anchors. External
sources remain ordinary links; they support the report rather than replacing its
project-specific explanations.

## Images in Markdown and PDF

Use the same image reference for Markdown previews and the PDF. In a file under
`report/chapters/`, link to the figures folder with `../assets/figures/`:

```markdown
![Backend architecture](../assets/figures/backend-architecture.png){width=100%}
```

In a Markdown file directly under `docs/`, the equivalent path is
`report/assets/figures/backend-architecture.png`. Always resolve the path from the
Markdown file, not from the repository root or the directory where the build runs.

Pandoc's `rebase_relative_paths` extension in `report/pandoc.yaml` resolves each
image relative to its source file when combining the chapters. No separate
PDF-only paths or copies of the images are needed. The optional `{width=100%}`
attribute controls the image width in the PDF; Markdown viewers may display that
attribute as text.

Use actual PNG or JPEG files for images shared by Markdown and the XeLaTeX PDF
build. Convert other formats instead of only changing their filename extension.
Store editable diagram sources separately from exported figures, with matching
base filenames:

```text
report/assets/
  diagram-sources/
    backend-architecture.mmd
  figures/
    backend-architecture.png
```

When updating a diagram, edit its source and regenerate the corresponding figure.
Commit both files so Markdown previews and PDF builds use the current image without
requiring a diagram renderer. SVG exports also belong in `figures/`; use the PNG
export when the same image is included in both Markdown and the XeLaTeX PDF.

