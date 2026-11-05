# JLiSA-Wit — Witness Validator for Java

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Built on JLiSA](https://img.shields.io/badge/Built%20on-JLiSA-informational)](https://github.com/lisa-analyzer/jlisa)

**JLiSA-Wit** validates verification witnesses for Java programs. Given a program, a property and a
violation witness produced by a verifier, it executes the program under the Java Debug Interface
(JDI), drives every nondeterministic input with the value recorded in the witness, and reports
whether the described execution really violates the property. Correctness witnesses are read and
checked against the format, but not validated yet. JLiSA-Wit takes part in the Java witness-validation
tracks of [SV-COMP](https://sv-comp.sosy-lab.org/).

JLiSA-Wit is developed by the **[Software and Systems Verification (SSV) Research Group](https://unive-ssv.github.io/)**
at [Ca' Foscari University of Venice](https://www.unive.it).

---

## Table of Contents

- [Overview](#overview)
- [Installation](#installation)
- [Usage](#usage)
- [Command-Line Options](#command-line-options)
- [Output](#output)
- [How It Works](#how-it-works)
- [Architecture](#architecture)
- [Limitations](#limitations)
- [License](#license)
- [References](#references)

---

## Overview

- **Execution-based validation.** The program is compiled and run on a real JVM. Witness values are
  injected into `Verifier.nondet*()` calls through JDI, without modifying or instrumenting the
  program.
- **Spurious-witness detection.** A witness is rejected when its execution ends without the
  violation, or when it reaches a waypoint the witness marks as `avoid`.
- **Producer-independent.** Witnesses are interpreted by their structure only, with no special
  handling for the verifier that produced them.
- **Input search** *(optional, `--input-search`)*. When a witness leaves some nondeterministic
  calls unspecified, a lightweight, feedback-guided search looks for values of those calls that
  reproduce the violation described by the witness.
- **Bug finding** *(`jlisa find-bug`)*. Without a witness, the same search looks for any execution that
  violates the property. The SV-COMP wrapper of [JLiSA](https://github.com/lisa-analyzer/jlisa) uses
  this mode when the static analysis cannot reach a verdict.
- **Correctness witnesses** *(not validated yet)*. They are read and checked against the format, and
  give `Could not validate`.

Supported properties:

| Property file | Violated when |
| --- | --- |
| `valid-assert.prp` | an `assert` statement fails (the program runs with `-ea`) |
| `no-runtime-exception.prp` | a `java.lang.RuntimeException` is not caught |

---

## Installation

JLiSA-Wit is part of JLiSA: it is built with it, from the `jlisa` folder (the one with
`build.gradle.kts`). The prerequisites are those of JLiSA: Java 21+ and GitHub credentials for the LiSA
dependency, a personal access token with the `read:packages` scope in `~/.gradle/gradle.properties`:

```properties
gpr.user=<github-username>
gpr.key=<github-token>
```

```bash
./gradlew installDist
```

The launcher is generated in `build/install/jlisa/bin/jlisa`: the validator is its sub-command
`validate`, and the bug finding its sub-command `find-bug`.

For SV-COMP, the validator ships inside the JLiSA archive, as `jlisa validate` (and as the bug-finding
fallback of `jlisa check`), built by `build_jlisa.sh` in https://github.com/lisa-analyzer/sv-comp.

---

## Usage

The task is given as SV-COMP gives it to the tool, its input files and its property file:

```bash
# validate a witness (violation or correctness: the kind is read from the witness)
build/install/jlisa/bin/jlisa validate \
    -w witness.yml \
    --svcomp-property valid-assert.prp -s common/ task/

# search for a violation without a witness, and write the witness of the violation found
build/install/jlisa/bin/jlisa find-bug \
    -t 60 --witness-output witness.yml \
    --svcomp-property valid-assert.prp -s common/ task/
```

The task is compiled into a temporary directory: its files are only read. `-s` takes all the
following arguments up to the next option.

The same runs from the sources, without building the launcher, with `./gradlew run`:

```bash
./gradlew run --args="validate -w witness.yml --svcomp-property valid-assert.prp -s common/ task/"
./gradlew run --args="find-bug --witness-output witness.yml --svcomp-property valid-assert.prp -s common/ task/"
```

Gradle runs them in the `jlisa` folder: relative paths start from there.

### Examples

With checkouts of [SV-Benchmarks](https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks) and
[SV-Witnesses](https://gitlab.com/sosy-lab/benchmarking/sv-witnesses), on the task `ExDarko_false`
(the assertion fails for some inputs) and `ExDarko_true` (it never fails), the Java examples of
SV-Witnesses that LiSA also uses in its tests (`lisa-sdk/witness-testcases/java`). The paths below
assume both checkouts next to the jlisa repository:

```bash
SVB=../../sv-benchmarks/java
SVW=../../sv-witnesses

# validate a violation witness (format 2.4)
./gradlew run --args="validate -w $SVW/examples/java/ExDarko_false.witness-2.4.yml --svcomp-property $SVB/properties/valid-assert.prp -s $SVB/common $SVB/jpf-regression/ExDarko_false"

# validate a correctness witness (format 2.4)
./gradlew run --args="validate -w $SVW/examples/java/ExDarko_true.witness-2.4.yml --svcomp-property $SVB/properties/valid-assert.prp -s $SVB/common $SVB/jpf-regression/ExDarko_true"

# bug finding: look for a violation and write its witness
./gradlew run --args="find-bug --witness-output build/witness.yml --svcomp-property $SVB/properties/valid-assert.prp -s $SVB/common $SVB/jpf-regression/ExDarko_false"

# validate the witness just written
./gradlew run --args="validate -w build/witness.yml --svcomp-property $SVB/properties/valid-assert.prp -s $SVB/common $SVB/jpf-regression/ExDarko_false"
```

Bug finding prints `Violation found`, and the validation of the witness it writes `Witness Correct`.
The correctness witness gives `Could not validate`: correctness witnesses are only parsed. The
violation example of SV-Witnesses also gives `Could not validate` on the current `ExDarko_false`:
its lines are those of an older version of the program (shifted by two).

---

## Command-Line Options

`jlisa validate`:

| Option | Description |
| --- | --- |
| `-w`, `--witness <file>` | Witness to validate: `.yml` (format 2.4). |
| `--type <violation\|correctness>` | The kind of witness expected; a witness of the other kind gives `Invalid witness type`. Without it, any kind (read from the witness). |
| `--input-search` | Search for values of the nondeterministic calls the witness does not specify. Off by default. |

`jlisa find-bug`:

| Option | Description |
| --- | --- |
| `-t`, `--time-limit <seconds>` | Time budget of the search (default: 60). |
| `--witness-output <file>` | Write the violation witness (format 2.4) of the violation found. |

Both:

| Option | Description |
| --- | --- |
| `-s`, `--source <inputs...>` | The input files and directories of the task, as SV-COMP gives them. |
| `--svcomp-property <file.prp>` | The property file of the task. |
| `-j`, `--java <path>` | Java executable used to run the program (default: `java` on the `PATH`). |
| `-l`, `--log-level <level>` | Log level on standard error (default: `WARN`; `INFO` or `DEBUG` for details). |
| `-h`, `--help` | Print the usage. |
| `-v`, `--version` | Print the version. |

---

## Output

The verdict is printed as the only line on standard output:

| Verdict | Meaning | BenchExec result |
| --- | --- | --- |
| `Witness Correct` | The violation is reproduced. | `false` — witness confirmed |
| `Witness Spurious` | The execution described by the witness does not violate the property. | `true` — witness rejected |
| `Could not validate` | No verdict: unsupported input or timeout. A correctness witness that parses gives this verdict (it is not validated). | `unknown` |
| `Invalid witness format` | The witness has a version of the witness format that is not supported (only 2.4 is), e.g. 2.3. | `ERROR (invalid witness format)` |
| `Parsing failed` | The witness cannot be read: a GraphML witness, an empty or malformed file. | `ERROR (parsing failed)` |
| `Invalid witness type` | The witness is not of the kind given with `--type` (e.g. a correctness witness with `--type violation`). | `ERROR (invalid witness type)` |
| `Violation found` | `find-bug` only: an execution violates the property. | — |

With `--witness-output`, the witness of the violation found has one `function_return` waypoint for
each `Verifier.nondet*()` call of the execution, with the value it returned, then the target. Its
metadata are those of JLiSA, with the description "Found with bug-finding mode". When the source
locations cannot be told apart (e.g., two candidates on the same line), no witness is written and the
summary says why.

The exit code is always `0`. The logs go to standard error, by default only the warnings (`-l` sets
the level). Every run also prints a summary on standard error, whatever the log level:

```
[STATS]  witness.values=1 plan.targets=0 plan.avoid-points=0 plan.branches=0 nondet.fixed=1 witness.values-unused=0
[REASON] correct/assertion-error: java.lang.AssertionError at Main.java:7
```

---

## How It Works

### Violation witnesses

1. The benchmark is compiled with `javax.tools.JavaCompiler` (`-source 8 -target 8 -g`).
2. It is launched under JDI, with assertions enabled for `assert`.
3. Each `Verifier.nondet*()` call is intercepted with a `MethodEntryRequest`, and the value given by
   the witness is returned with `ThreadReference.forceEarlyReturn()`.
4. Breakpoints are placed on the witness's target, `avoid` and branching waypoints.
5. The witness is **correct** if the execution reaches the violation at the target. It is
   **spurious** if it reaches an `avoid` waypoint, or ends without the violation once all witness
   values have been used.

For `no-runtime-exception`, the exception must also occur with assertions disabled, so that an
assertion error is never mistaken for a runtime exception.

### Input search

When the first execution does not reach the violation and some calls were left unspecified, the
validator searches for values of those calls. The search adapts *colorization* and
*input-to-state correspondence* from REDQUEEN [1] to the nondeterministic inputs of Java programs:

1. A first execution assigns a distinct, recognizable value to every call site, so that values
   observed later in comparisons can be traced back to the input they come from.
2. Values compared against those inputs, constants from the source code and type boundaries become
   candidate inputs, tried individually and in combination.
3. The search is bounded: at most 40 executions of at most 10 seconds each, and 75 seconds in total
   during validation.

The search can only confirm a witness, never reject one: if no violating execution is found, the
verdict of the first execution is kept.

### Correctness witnesses

Correctness witnesses are not validated yet: once the witness is read, `validate` answers
`Could not validate`.

### Reading the witness

The witness is read by LiSA (`it.unive.lisa.witness.v2.WitnessReader`), which also checks it against
the rules of the format. The kind of witness (violation or correctness) comes from its `entry_type`.
A version other than 2.4 gives `Invalid witness format`; a file that cannot be read gives
`Parsing failed`.

---

## Architecture

```
src/main/java/it/unive/jlisa/
├── Main.java               jlisa: dispatches to the sub-commands
├── cli/                    command line of check, validate and find-bug, shared options
└── witness/validator/
    ├── RunSummary.java     per-run summary ([STATS], [REASON])
    ├── parser/             witness model and task properties
    ├── model/              internal witness model: execution plan, interceptions, waypoints, invariants
    ├── filter/             scopes and values of witness assumptions
    ├── violation/          JDI launch, event loop, value injection, bug finding
    ├── search/             input search
    └── correctness/        correctness witnesses
```

---

## Limitations

- **Format:** only version 2.4 of the YAML format; GraphML witnesses (format 1.0) are not read.
- **String values** in witnesses are not supported yet.
- **Correctness witnesses** are only read, not validated.
- **Multi-threaded programs** are not supported.

---

## License

JLiSA-Wit is released under the [MIT License](LICENSE).

---

## References

1. C. Aschermann, S. Schumilo, T. Blazytko, R. Gawlik, T. Holz. *REDQUEEN: Fuzzing with
   Input-to-State Correspondence.* NDSS 2019. [doi:10.14722/ndss.2019.23371](https://doi.org/10.14722/ndss.2019.23371)
2. P. Ayaziová, D. Beyer, M. Lingsch-Rosenfeld, M. Spiessl, J. Strejček. *Software Verification
   Witnesses 2.0.* SPIN 2024.
3. [SV-COMP](https://sv-comp.sosy-lab.org/) ·
   [BenchExec](https://github.com/sosy-lab/benchexec) ·
   [SV-Witnesses](https://gitlab.com/sosy-lab/benchmarking/sv-witnesses) ·
   [JLiSA](https://github.com/lisa-analyzer/jlisa) ·
   [LiSA](https://github.com/lisa-analyzer/lisa)
