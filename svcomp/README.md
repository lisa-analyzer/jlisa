# jlisa-wit

Violation-witness validator for Java programs (SV-COMP, Java categories).
It runs the program under the Java Debug Interface and steers the
`Verifier.nondet*()` calls with the values recorded in the witness.

Project: https://github.com/lisa-analyzer/jlisa-witness-validator

## Contents

- `jlisa-wit` — single executable: the Python wrapper and the Java validator
  with its dependencies, packed with PyInstaller
- `smoketest.sh` — validates the two witnesses in `test/`
- `test/` — a small test program, the SV-COMP `Verifier` classes
  (`test/common`, Apache-2.0, from https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks)
  and one valid and one spurious witness

## Requirements

- JDK 21 or newer (`openjdk-21-jdk-headless`), as `java` on the `PATH` or via `JAVA_HOME`

## Usage

    ./jlisa-wit --witness witness.graphml --property valid-assert.prp common/ benchmark/

The last line on stdout is `Witness Correct` (witness confirmed),
`Witness Spurious` (witness rejected) or `Could not validate`.
Supported properties: `valid-assert` and `no-runtime-exception`;
any other property file is an error (exit code 1).
