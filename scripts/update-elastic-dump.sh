#!/usr/bin/env bash
#
# Regenerates the elasticdump test fixtures in test/resources/elastic_dump
# by running the konfo-backend mock data export in kouta-indeksoija and
# copying the resulting dump files into this repo.
#
# Requires docker and lein, and a sibling checkout of kouta-indeksoija
# (override the location with KOUTA_INDEKSOIJA_DIR).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
KOUTA_INDEKSOIJA_DIR="$(cd "${KOUTA_INDEKSOIJA_DIR:-$REPO_DIR/../kouta-indeksoija}" && pwd)"
DEST_DIR="$REPO_DIR/test/resources/elastic_dump"
SRC_DIR="$KOUTA_INDEKSOIJA_DIR/elasticdump/konfo-backend"

if [ ! -d "$KOUTA_INDEKSOIJA_DIR" ]; then
  echo "kouta-indeksoija checkout not found at $KOUTA_INDEKSOIJA_DIR" >&2
  echo "Set KOUTA_INDEKSOIJA_DIR to point at your kouta-indeksoija checkout." >&2
  exit 1
fi

echo "==> Running 'lein elasticdump:konfo-backend' in $KOUTA_INDEKSOIJA_DIR"
(cd "$KOUTA_INDEKSOIJA_DIR" && lein elasticdump:konfo-backend)

if [ ! -d "$SRC_DIR" ]; then
  echo "Expected dump output at $SRC_DIR but it doesn't exist" >&2
  exit 1
fi

echo "==> Replacing $DEST_DIR with dump from $SRC_DIR"
rm -f "$DEST_DIR"/*.json
cp "$SRC_DIR"/*.json "$DEST_DIR"/

echo "==> Done. Review the diff in test/resources/elastic_dump before committing."
