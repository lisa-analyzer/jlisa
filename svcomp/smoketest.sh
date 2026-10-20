#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

version="$(./jlisa-wit --version)"
echo "version: $version"
[ -n "$version" ]

# check [--input-search] WITNESS PROPERTY PROGRAM EXPECTED...
check() {
  local flags=()
  if [ "$1" = "--input-search" ]; then
    flags=("$1")
    shift
  fi
  local witness="$1" property="$2" program="$3"
  shift 3
  local actual
  actual="$(./jlisa-wit ${flags[@]+"${flags[@]}"} --witness "test/$witness" --property "test/$property" test/common/ "test/$program/" | tail -n 1)"
  echo "${flags[*]:+${flags[*]} }$witness, $property, $program: $actual"
  for expected in "$@"; do
    [ "$actual" = "$expected" ] && return 0
  done
  return 1
}

# find_bug PROPERTY PROGRAM EXPECTED...
find_bug() {
  local property="$1" program="$2"
  shift 2
  local actual
  actual="$(./jlisa-wit --find-bug --time-limit 30 --property "test/$property" test/common/ "test/$program/" | tail -n 1)"
  echo "--find-bug, $property, $program: $actual"
  for expected in "$@"; do
    [ "$actual" = "$expected" ] && return 0
  done
  return 1
}

check witness-correct.graphml valid-assert.prp nondet-assert "Witness Correct"
check witness-spurious.graphml valid-assert.prp nondet-assert "Witness Spurious"
check witness-novalue.graphml valid-assert.prp nondet-assert "Could not validate"
# no values in the witness: the input search finds x == 0
check --input-search witness-novalue.graphml valid-assert.prp nondet-assert "Witness Correct"
check witness-correct.graphml no-runtime-exception.prp nondet-rte "Witness Correct"
check witness-correct.graphml no-runtime-exception.prp nondet-assert "Could not validate"
find_bug valid-assert.prp nondet-assert "Violation found"
find_bug no-runtime-exception.prp nondet-rte "Violation found"
# an assertion error is not a runtime exception
find_bug no-runtime-exception.prp nondet-assert "Could not validate"
