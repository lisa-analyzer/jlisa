# JLiSA-Wit — Witness Validator for Java

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Built on JLiSA](https://img.shields.io/badge/Built%20on-JLiSA-informational)](https://github.com/lisa-analyzer/jlisa)

**JLiSA-Wit** validates verification witnesses for Java programs. Given a program, a property and a
violation witness produced by a verifier, it executes the program under the Java Debug Interface
(JDI), drives every nondeterministic input with the value recorded in the witness, and reports
whether the described execution really violates the property. JLiSA-Wit takes part in the Java
witness-validation track of [SV-COMP](https://sv-comp.sosy-lab.org/).

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
- **Bug finding** *(`--find-bug`)*. Without a witness, the same search looks for any execution that
  violates the property. The SV-COMP wrapper of [JLiSA](https://github.com/lisa-analyzer/jlisa) uses
  this mode when the static analysis cannot reach a verdict.
- **Correctness witnesses** *(experimental)*. Loop invariants are checked with JLiSA's abstract
  interpretation.

Supported properties:

| Property file | `--property` | Violated when |
| --- | --- | --- |
| `valid-assert.prp` | `assert` | an `assert` statement fails (the program runs with `-ea`) |
| `no-runtime-exception.prp` | `runtime` | a `java.lang.RuntimeException` is not caught |

---

## Installation

**Prerequisites:** Java 21+, Gradle (wrapper included), GitHub credentials for the LiSA dependency,
and a local checkout of JLiSA.

### LiSA dependency

LiSA snapshots are published on GitHub Packages, which requires authentication. Add a personal
access token with the `read:packages` scope to `~/.gradle/gradle.properties`:

```properties
gpr.user=<github-username>
gpr.key=<github-token>
```

The environment variables `USERNAME` and `TOKEN` can be used instead.

### JLiSA checkout

JLiSA is built together with the validator as a Gradle composite build. By default it is expected
next to this repository:

```
workspace/
├── jlisa/                     # git clone -b svcomp-2027 https://github.com/lisa-analyzer/jlisa.git
└── jlisa-witness-validator/   # this repository
```

A different location can be given with `-PjlisaPath=<path>/jlisa/jlisa` or with the `JLISA_HOME`
environment variable.

### Build

```bash
./gradlew installDist
```

The launcher is generated in `build/install/jlisa-witness-validator/bin/`.

### SV-COMP

For SV-COMP, the validator ships inside the JLiSA archive, as `jlisa validate` (and as the bug-finding
fallback of `jlisa check`), built by `build_jlisa.sh --jlisa-wit-dir` in
https://github.com/lisa-analyzer/sv-comp.

---

## Usage

The task is given as SV-COMP gives it to the tool, its input files and its property file:

```bash
# validate a violation witness
build/install/jlisa-witness-validator/bin/jlisa-witness-validator \
    --witness witness.graphml \
    --svcomp-property valid-assert.prp --source common/ task/

# search for a violation without a witness, and write the witness of the violation found
build/install/jlisa-witness-validator/bin/jlisa-witness-validator \
    --find-bug --time-limit 60 --witness-output witness.yml \
    --svcomp-property valid-assert.prp --source common/ task/
```

The task is compiled into a temporary directory: its files are only read. `--source` takes all the
following arguments, hence it comes last.

The task can also be given by a task file, with the property by name; the task is then compiled into
its own directory:

```bash
build/install/jlisa-witness-validator/bin/jlisa-witness-validator \
    --witness witness.graphml \
    --benchmark /path/to/task.yml --property assert
```

---

## Command-Line Options

| Option | Description |
| --- | --- |
| `--witness <file>` | Witness to validate: `.graphml` (format 1.0) or `.yml` (format 2.x). |
| `--source <inputs...>` | The input files and directories of the task, as SV-COMP gives them (with `--svcomp-property`). |
| `--svcomp-property <file.prp>` | The property file of the task (with `--source`). |
| `--benchmark <task.yml>` | Verification task file (with `--property`, instead of `--source`). |
| `--property <assert\|runtime>` | Property to check (with `--benchmark`). |
| `--extra-sources <dir>` | Additional sources compiled with the benchmark, e.g. SV-COMP's `common/` (with `--benchmark`). |
| `--input-search` | Search for values of the nondeterministic calls the witness does not specify. Off by default. |
| `--find-bug` | Bug-finding mode: no witness, search for a violating execution. |
| `--time-limit <seconds>` | Time budget for `--find-bug` (default: 60). |
| `--witness-output <file>` | With `--find-bug`: write the violation witness (format 2.4) of the violation found (needs `--source`). |
| `--java <path>` | Java executable used to run the benchmark (default: `java` on the `PATH`). |
| `--verbose` | Detailed logging on standard error. |
| `--help` | Print the usage. |

---

## Output

The verdict is printed as the only line on standard output:

| Verdict | Meaning | BenchExec result |
| --- | --- | --- |
| `Witness Correct` | The violation is reproduced (or the invariants hold). | `false` — witness confirmed |
| `Witness Spurious` | The execution described by the witness does not violate the property. | `true` — witness rejected |
| `Could not validate` | No verdict: unsupported input, parse error or timeout. | `unknown` |
| `Violation found` | `--find-bug` only: an execution violates the property. | — |

With `--witness-output`, the witness of the violation found has one `function_return` waypoint for
each `Verifier.nondet*()` call of the execution, with the value it returned, then the target. Its
metadata are those of JLiSA, with the description "Found with bug-finding mode". When the source
locations cannot be told apart (e.g., two candidates on the same line), no witness is written and the
summary says why.

The exit code is always `0`. Diagnostic output goes to standard error; by default only warnings are
printed, including a one-line summary of each run:

```
WARN  [STATS]  witness.values=1 plan.targets=0 plan.avoid-points=0 plan.branches=0 nondet.fixed=1 witness.values-unused=0
WARN  [REASON] correct/assertion-error: java.lang.AssertionError at Main.java:7
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

The program is analyzed with JLiSA, and each loop invariant is checked for initiation,
inductiveness and safety. Only the initiation check is currently implemented.

---

## Architecture

```
src/main/java/it/unive/jlisa/witness/validator/
├── Main.java          command-line interface
├── Logging.java       logging configuration
├── RunSummary.java    per-run summary ([STATS], [REASON])
├── parser/            witness formats 1.0 (GraphML) and 2.x (YAML), task definitions
├── model/             internal witness model: execution plan, interceptions, waypoints, invariants
├── filter/            scopes and values of witness assumptions
├── violation/         JDI launch, event loop, value injection, bug finding
├── search/            input search
└── correctness/       correctness witnesses (JLiSA)
tools/                 scripts for local benchmark runs
```

---

## Limitations

- **Format 2.x:** the reader expects a mapping at the top level, while the official format is a
  list of entries. Witnesses in the official 2.x layout are not supported yet.
- **String values** in witnesses are not supported yet.
- **Correctness witnesses:** initiation check only, linear arithmetic only, no format 1.0.
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
