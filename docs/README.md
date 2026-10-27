# Project report

The English report is maintained in `report/chapters/` and compiled as **one PDF**. Its layout and chapter responsibilities follow `STRUTTURA`; technical content comes from the project sources. Empty author sections use hidden Markdown comments.

## Requirements

- Pandoc 3.x
- A full TeX Live or MiKTeX installation with `xelatex`
- TeX Gyre Heros and TeX Gyre Cursor fonts (included in common TeX distributions)

Ensure `pandoc` and `xelatex` are on `PATH`.


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

- `report/chapters/`: sections 01–09 in the order required by the HSLU report guide. Section 08 contains one Appendices chapter and includes all eight pages of the supplied original assignment. Keep the original PDF unchanged; add a signed version only if supplied.
- `report/assets/`: graphics organized by component, with editable Mermaid sources next to their PNG/SVG exports. From a chapter use `![Backend architecture](../assets/backend/backend-architecture.png)`; preserve aspect ratio.
- `report/metadata.yaml`: title, authors, date, language and layout. Verify draft details with the authors before submission.
- `report/references.bib`: sources for citations such as `[@hsluReportGuide2025]`.
- `report/pandoc.yaml`: PDF settings and the exact input order. Add new chapter files here; filenames alone do not determine the order.
- `build.ps1` and `build.sh`: build commands for Windows and Unix.

## Content ownership

| Location | Responsibility |
|---|---|
| Chapter 1 | Context, scope and concise requirements overview |
| Chapter 2 | Research and practice; missing material remains an author task |
| Chapter 3 | Architecture, rationale, essential flows, assumptions and integration conflicts |
| Chapter 4 | Development, planning and evaluation methods |
| Chapter 5 | Documented implementation and discrepancies with repository configuration |
| Chapter 6 | Available evidence and evaluation limits; ignored tests are not passed tests |
| Chapter 7 | Reflection and future work, only when documented |
| Appendices | Requirements, expected results, project records, technical reference, tests, guides, security proposals and the original assignment |
| Lists and Registers | Automatic figure/table lists, author-maintained registers and bibliography |

**Interfaces and Data Structures** within Appendices owns backend endpoints/permissions/errors, mobile interfaces/classes/models/DTOs, database fields/constraints and terminal interface catalogues. Main chapters link to this reference.



The `planning/` registers and component/root READMEs remain operational documents. The appendix contains the full available requirements, expected results and backlog, plus relevant database/mobile procedures. When operational sources change, update their corresponding report sections. Detailed GitHub Issue content that was not supplied is not fabricated.

## Captions and links

The asset layout is deliberately shallow:

```text
report/assets/
  architecture/       # Cross-component BLE flows and recovery
  web/ui/             # Web screen wireframes
  backend/            # Backend architecture, layers, lifecycle, tests and ERD
  mobile/             # Mobile components, classes and authentication flows
  terminal/           # Terminal software modules and interfaces
    hardware/         # Wiring, power, enclosure, latch and cutting drawings
```

Keep each diagram's `.mmd`, `.png` and optional `.svg` together with the same base filename. Use relative image paths from the Markdown file, such as `../assets/web/ui/s01-signin.png`. These same paths work in Markdown viewers and in the PDF through Pandoc's `rebase_relative_paths` option. Do not create separate copies just for PDF output.

Use automatic heading numbering, unique figure anchors and descriptive table captions. Table captions use `: Caption {#tbl-name}`, with a matching HTML anchor before the table. Cross-chapter links use `08-appendices.md#destination`; filters resolve them to internal PDF links and reject missing destinations. The cover, fonts, headings, footers and numbering are centralized in the metadata file.

Keep original assets even when excluded from the rendered report. The existing ERD remains excluded pending reconciliation, as in the source report. No template diagram replaces a current project graphic.

