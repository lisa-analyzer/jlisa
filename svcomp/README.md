# jlisa-wit

Violation-witness validator for Java programs (SV-COMP, Java categories).
It runs the program under the Java Debug Interface and makes the
`Verifier.nondet*()` calls return the values recorded in the witness.

Project: https://github.com/lisa-analyzer/jlisa-witness-validator

## Contents

- `jlisa-wit`: a single executable, the Python wrapper and the Java validator
  with its dependencies, packed with PyInstaller
- `smoketest.sh`: runs the validator on the witnesses and programs in `test/`
- `test/`: two small programs (`nondet-assert`, `nondet-rte`), three witnesses
  (one valid, one spurious, one without values) and the SV-COMP `Verifier`
  classes (`test/common`, Apache-2.0, from
  https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks)

## Requirements

- JDK 21 or newer (`openjdk-21-jdk-headless`), as `java` on the `PATH` or via `JAVA_HOME`

## Usage

Validating a witness:

    ./jlisa-wit --witness witness.graphml --property valid-assert.prp common/ benchmark/

The last line on stdout is `Witness Correct` (witness confirmed),
`Witness Spurious` (witness rejected) or `Could not validate`.

With `--input-search`, when the witness does not give a value for every
nondet call, the validator also searches for values of the other calls that
lead to the violation the witness describes. It is off by default.

Bug finding, without a witness:

    ./jlisa-wit --find-bug [--time-limit SECONDS] --property valid-assert.prp common/ benchmark/

The last line on stdout is `Violation found` or `Could not validate`. The
default time limit is 60 seconds.

Supported properties: `valid-assert` and `no-runtime-exception`; any other
property file is an error (exit code 1).
