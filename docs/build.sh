#!/bin/sh
set -eu

DOCS_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if ! command -v pandoc >/dev/null 2>&1; then
  echo 'Pandoc is missing. See docs/README.md.' >&2
  exit 1
fi
if ! command -v xelatex >/dev/null 2>&1; then
  echo 'XeLaTeX is missing. See docs/README.md.' >&2
  exit 1
fi

mkdir -p "$DOCS_DIR/output"
mkdir -p "$DOCS_DIR/output/.tmp"
TMPDIR="$DOCS_DIR/output/.tmp" pandoc --defaults "$DOCS_DIR/report/pandoc.yaml"
echo "PDF created: $DOCS_DIR/output/IIP_BLE_Booking_System.pdf"
