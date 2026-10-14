#!/usr/bin/env bash
# Checks a jlisa-wit.zip as SV-COMP expects it: one top-level directory, the
# required files, a working --version and a passing smoketest.sh.
#
#   check-release.sh [path/to/jlisa-wit.zip]
set -euo pipefail

SVCOMP="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ZIP="$(realpath "${1:-$(dirname "$SVCOMP")/build/svcomp/jlisa-wit.zip}")"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

[ -f "$ZIP" ] || fail "archive not found: $ZIP"
unzip -q "$ZIP" -d "$TMP"

TOP=("$TMP"/*)
[ "${#TOP[@]}" -eq 1 ] && [ -d "${TOP[0]}" ] || fail "the archive must contain exactly one top-level directory"
DIR="${TOP[0]}"
[ "$(basename "$DIR")" = "jlisa-wit" ] || fail "top-level directory is $(basename "$DIR"), expected jlisa-wit"

for file in jlisa-wit smoketest.sh LICENSE README.md \
            test/valid-assert.prp test/witness-correct.graphml test/witness-spurious.graphml test/witness-novalue.graphml \
            test/no-runtime-exception.prp test/nondet-assert/Main.java test/nondet-rte/Main.java \
            test/common/org/sosy_lab/sv_benchmarks/Verifier.java; do
  [ -f "$DIR/$file" ] || fail "missing $file"
done
for file in jlisa-wit smoketest.sh; do
  [ -x "$DIR/$file" ] || fail "$file is not executable"
done
echo "OK: all files present"

VERSION="$("$DIR/jlisa-wit" --version)"
[ -n "$VERSION" ] || fail "jlisa-wit --version printed nothing"
echo "OK: version $VERSION"

"$DIR/smoketest.sh" || fail "smoketest.sh failed"
echo "OK: smoketest passed"
echo "$ZIP is ready"
