#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

version="$(./jlisa-wit --version)"
echo "version: $version"
[ -n "$version" ]

# check WITNESS PROPERTY PROGRAM EXPECTED...
check() {
  local witness="$1" property="$2" program="$3"
  shift 3
  local actual
  actual="$(./jlisa-wit --witness "test/$witness" --property "test/$property" test/common/ "test/$program/" | tail -n 1)"
  echo "$witness, $property, $program: $actual"
  for expected in "$@"; do
    [ "$actual" = "$expected" ] && return 0
  done
  return 1
}

check witness-correct.graphml valid-assert.prp nondet-assert "Witness Correct"
check witness-spurious.graphml valid-assert.prp nondet-assert "Witness Spurious"
check witness-novalue.graphml valid-assert.prp nondet-assert "Could not validate"
check witness-correct.graphml no-runtime-exception.prp nondet-rte "Witness Correct"
check witness-correct.graphml no-runtime-exception.prp nondet-assert "Could not validate"
