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
- `report/assets/`: images and diagrams. Use paths relative to `report/` in Markdown, for example `![Architecture](assets/architecture.png)`.
- `report/metadata.yaml`: title, authors, date, language, abstract, and layout. Replace draft details before submission.
- `report/references.bib`: sources for citations such as `[@projectAssignment2026]`.
- `report/pandoc.yaml`: PDF settings and the exact input order. Add new chapter files here; filenames alone do not determine the order.
- `build.ps1` and `build.sh`: build commands for Windows and Unix.

