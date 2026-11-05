<img src="logo.png" alt="logo" width="300"/>

---

# JLiSA — Java Frontend of LiSA (Library for Static Analysis)

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![SV-COMP 2026](https://img.shields.io/badge/SV--COMP%202026-🥉%20Bronze%20Medal-CD7F32)](https://sv-comp.sosy-lab.org/2026/)
[![Built on LiSA](https://img.shields.io/badge/Built%20on-LiSA-informational)](https://github.com/lisa-analyzer/lisa)

**JLiSA** is a static analysis tool for Java programs, built on top of the [LiSA (Library for Static Analysis)](https://github.com/lisa-analyzer/lisa) framework. It provides a front-end that translates Java source files into LiSA's control flow graph (CFG) representation, enriches it with the semantics of a subset of the Java standard library, and runs configurable abstract interpretation analyses to detect bugs and verify program properties.

JLiSA is a joint effort between the **[Software and Systems Verification (SSV) Research Group](https://unive-ssv.github.io/)** at [Ca' Foscari University of Venice](https://www.unive.it) and the **[University of Parma](https://www.unipr.it)**.

---

## 🥉 SV-COMP 2026 — Bronze Medal

In its **first participation** at [SV-COMP 2026](https://sv-comp.sosy-lab.org/2026/) (15th International Competition on Software Verification), JLiSA achieved **3rd place in the Java track**, earning the bronze medal. SV-COMP is the world's leading competition in automated software verification; results are presented at [TACAS 2026](https://etaps.org/2026/tacas), held in Turin, Italy, in April 2026.

> Arceri et al., _"JLiSA: The Java Frontend of the Library for Static Analysis"_ (Competition Contribution), SV-COMP 2026.
> Artifact: [doi.org/10.5281/zenodo.17609338](https://doi.org/10.5281/zenodo.17609338)

Further details in the press releases from [Ca' Foscari University](https://www.unive.it/web/en/15205/article/7827) and [University of Parma](https://www.unipr.it/en/node/111091).

---

## Table of Contents

- [Overview](#overview)
- [Installation](#installation)
- [Usage](#usage)
- [Command-Line Options](#command-line-options)
- [Architecture](#architecture)
- [Development](#development)
- [License](#license)

---

## Overview

JLiSA translates Java source code into [LiSA](https://github.com/lisa-analyzer/lisa)'s intermediate representation and runs abstract interpretation analyses over the resulting program model. The analysis configuration is fully customizable — abstract domains, interprocedural strategy, and semantic checkers can all be selected independently. For SV-COMP 2026, JLiSA was configured with the following components:

- **Field-sensitive, point-based heap abstraction** — tracks heap objects at allocation sites with per-field sensitivity
- **Reduced product of constant propagation and interval domain** — abstracts numerical, string, and Boolean values
- **Type inference** — tracks runtime types of expressions
- **Reachability analysis** — distinguishes definitely reachable instructions from possibly reachable or unreachable ones, improving precision on conditional branches
- **Call-string-based interprocedural analysis** — context-sensitive up to 150 nested calls, with 0-CFA for call graph construction
- **Semantic checkers** — verifies `assert` statements and detects potential uncaught runtime exceptions

---

## Installation

**Prerequisites:** Java 17+, Gradle (wrapper included), GitHub credentials for the LiSA dependency.

### LiSA Dependency

LiSA packages are hosted on [GitHub Packages](https://github.com/lisa-analyzer/lisa/packages). Add your credentials to `~/.gradle/gradle.properties`:

```properties
gpr.user=<your-github-username>
gpr.key=<your-github-personal-access-token>
```

Or export the environment variables `USERNAME` and `TOKEN`.

### Build

```bash
git clone https://github.com/lisa-analyzer/jlisa.git
cd jlisa/jlisa
./gradlew build
```

This runs code style checks and compiles the project. To produce a self-contained executable ZIP:

```bash
./gradlew distZip
```

The archive is written to `build/distributions/jlisa-0.1.zip`.

---

## Usage

JLiSA has three sub-commands:

| Sub-command | What it does |
| ----------- | ------------ |
| `check`     | Analyses a program and checks its properties (the verifier) |
| `validate`  | Validates a witness of an SV-COMP task (see [WITNESS-VALIDATOR.md](jlisa/WITNESS-VALIDATOR.md)) |
| `find-bug`  | Looks for inputs that violate the property of an SV-COMP task, by running the program |

`jlisa <sub-command> -h` prints the options of a sub-command, and `jlisa -v` the version.

After building the distribution, run JLiSA directly:

```bash
./build/distributions/jlisa-0.1/bin/jlisa check -s path/to/File.java -o out/ -n ConstantPropagation
```

Or via Gradle without packaging:

```bash
./gradlew run --args="check -s path/to/File.java -o out/ -n ConstantPropagation"
```

Results are written to the output directory as a `report.json` summary. With `--dump-graphs`, the JSON graphs of the analysed methods (one per CFG) are written too, and with `--dump-html`, their HTML pages. With `--svcomp-property`, a violation witness is written for a definite violation of the property.

On an SV-COMP task (its input files and directories, and its property file):

```bash
jlisa check    -s common/ task/ -o out/ -n ConstantPropagation --svcomp-property valid-assert.prp
jlisa validate -s common/ task/ --svcomp-property valid-assert.prp -w witness.yml
jlisa find-bug -s common/ task/ --svcomp-property valid-assert.prp --witness-output witness.yml
```

`validate` and `find-bug` run the program under JDI: they need the modules `jdk.compiler` and `jdk.jdi`, which a JDK has (on Debian and Ubuntu, `openjdk-N-jre-headless` has them too). The logs of all sub-commands go to standard error; `validate` and `find-bug` print their verdict on standard output.

---

## Command-Line Options

### Shared by all sub-commands

| Option | Long Option         | Argument  | Description                                            |
| ------ | ------------------- | --------- | ------------------------------------------------------ |
| `-s`   | `--source`          | `file...` | Java source files and directories to analyze           |
| N/A    | `--svcomp-property` | `prp`     | SV-COMP property file of the task                      |
| `-l`   | `--log-level`       | `level`   | Log level on standard error: `OFF`, `ERROR`, `WARN` (default), `INFO`, `DEBUG` |
| `-v`   | `--version`         | —         | Print the tool version                                 |
| `-h`   | `--help`            | —         | Print the help message                                 |

### `check`

| Option | Long Option         | Argument  | Description                                            |
| ------ | ------------------- | --------- | ------------------------------------------------------ |
| `-o`   | `--outdir`          | `path`    | Output directory for analysis results                  |
| `-n`   | `--numericalDomain` | `domain`  | Numerical domain: `ConstantPropagation`, `ConstantPropagationWithTarsis`, `ConstantPropagationWithIntervals` |
| `-c`   | `--checker`         | `checker` | Semantic checker: `Assert`                             |
| `-m`   | `--mode`            | `mode`    | Execution mode: `Debug` (default), `Statistics`        |
| `-d`   | `--debug-information` | —       | Write debug information (open calls, bottom states)    |
| `-e`   | `--dump-exceptions` | —         | In statistics mode, print the exceptions on standard output |
| N/A    | `--dump-html`       | —         | Write the HTML pages (disabled by default)             |
| N/A    | `--dump-graphs`     | —         | Write the JSON graphs (disabled by default)            |

`--svcomp-property` can be repeated: one witness for each property.

### `validate`

| Option | Long Option      | Argument | Description                                              |
| ------ | ---------------- | -------- | -------------------------------------------------------- |
| `-w`   | `--witness`      | `file`   | Witness to validate (YAML, format 2.4)                   |
| N/A    | `--type`         | `type`   | Kind of witness expected: `violation` or `correctness`; a witness of the other kind gives `Invalid witness type` |
| N/A    | `--input-search` | —        | Search for values of the nondet calls the witness does not fix (off by default) |
| `-j`   | `--java`         | `path`   | Java executable used to run the program (default: `java` on the `PATH`) |

### `find-bug`

| Option | Long Option        | Argument  | Description                                           |
| ------ | ------------------ | --------- | ----------------------------------------------------- |
| `-t`   | `--time-limit`     | `seconds` | Time for the search (default 60)                      |
| N/A    | `--witness-output` | `file`    | Write the violation witness of the violation found    |
| `-j`   | `--java`           | `path`    | Java executable used to run the program (default: `java` on the `PATH`) |

---

## Architecture

Java source code is parsed via the [Eclipse Java Development Tools (JDT)](https://projects.eclipse.org/projects/eclipse.jdt) library.

JLiSA's frontend translates Java source files to LiSA's IR in five sequential passes, implemented using the JDT visitor pattern in the following classes:

1. **`PopulateUnitsASTVisitor`** — registers all class and interface stubs in the program
2. **`SetRelationshipsASTVisitor`** — resolves superclass and interface relationships
3. **`SetGlobalsASTVisitor`** — registers fields and enum constants
4. **`InitCodeMembersASTVisitor`** — declares method and constructor signatures
5. **`CompilationUnitASTVisitor`** — performs full body translation, generating CFGs

### Java Standard Library

Standard library classes are not parsed from source. Instead, hand-written stub files (`src/main/resources/libraries/*.txt`) declare class hierarchies, fields, and method signatures using a custom DSL parsed by an ANTLR grammar. Method semantics are implemented as Java classes under `it.unive.jlisa.program.java.constructs`. Stubs are loaded lazily at analysis time by `LibrarySpecificationProvider`.

### Key Packages

| Package                                  | Purpose                                                               |
| ---------------------------------------- | --------------------------------------------------------------------- |
| `it.unive.jlisa.frontend`                | Parsing pipeline, `JavaFrontend`, `ParserContext`                     |
| `it.unive.jlisa.frontend.util`           | Shared utilities (e.g. `FQNUtils` for building fully qualified names) |
| `it.unive.jlisa.frontend.visitors`       | AST visitor base classes and hierarchy                                |
| `it.unive.jlisa.program.cfg.expression`  | Java-specific expression nodes                                        |
| `it.unive.jlisa.program.cfg.statement`   | Java-specific statement nodes                                         |
| `it.unive.jlisa.program.java.constructs` | Library method semantics                                              |
| `it.unive.jlisa.program.libraries`       | Library stub loader                                                   |
| `it.unive.jlisa.program.type`            | Java type system                                                      |
| `it.unive.jlisa.analysis`                | Abstract domains (heap, value, type)                                  |
| `it.unive.jlisa.interprocedural`         | Call graph and interprocedural analysis                               |
| `it.unive.jlisa.checkers`                | Semantic checkers (`AssertChecker`)                                   |
| `it.unive.jlisa.witness`                 | Violation witness generation (GraphML)                                |

---

## Development

### Running Tests

```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "it.unive.jlisa.cron.MathTest"

# Run a single test method
./gradlew test --tests "it.unive.jlisa.cron.MathTest.testMath"
```

Test inputs live in `java-testcases/` and expected JSON outputs are stored alongside them. To regenerate expected outputs after an intentional change, temporarily set `conf.forceUpdate = true` in `TestHelpers`.

### Code Style

JLiSA uses [Spotless](https://github.com/diffplug/spotless) (Eclipse formatter) and [Checkstyle](https://checkstyle.org/). Formatting uses tabs, not spaces.

```bash
# Check style
./gradlew checkCodeStyle

# Auto-fix formatting
./gradlew spotlessApply
```

---

## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).
