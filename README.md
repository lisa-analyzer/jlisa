# jlisa

`jlisa` is a static analysis tool built upon the [LiSA](https://github.com/lisa-analyzer/lisa) framework. It provides a frontend for analyzing Java source code using configurable abstract domains and analysis strategies.

## Table of Contents

- [Overview](#overview)
- [Installation](#installation)
- [Usage](#usage)
- [Command Line Options](#command-line-options)
- [Architecture](#architecture)
- [Development](#development)
- [License](#license)

## Overview

This tool parses Java source code, analyzes it using abstract interpretation, and generates analysis results in a specified output directory. It is built on top of the LiSA framework, providing support for heap analysis, numeric analysis, and type inference.

## Installation

Clone the repository:

```bash
git clone https://github.com/lisa-analyzer/jlisa.git
cd jlisa
```

Build with Gradle:

```bash
./gradlew clean build
```

## Usage

To analyze Java source files:

```bash
java -jar target/jlisa.jar -s <source-files> -o <output-directory> [-l <log-level>]
```

## Command Line Options

| Option | Long Option | Argument | Description |
|--------|-------------|----------|-------------|
| `-h`   | `--help`    | None     | Print help message |
| `-s`   | `--source`  | Files    | Java source files to analyze |
| `-o`   | `--outdir`  | Path     | Output directory for results |
| `-l`   | `--log-level` | Level | Logging level (e.g., INFO, DEBUG, ERROR) |
| `-v`   | `--version` | None | Version of current jlisa's implementation |

## Architecture

The main execution flow is handled by the `Main` class:

1. Parses command line arguments using Apache Commons CLI.
2. Parses Java source files using the `JavaFrontend`.
3. Builds a LiSA `Program` from the parsed files.
4. Configures analysis parameters (`LiSAConfiguration`).
5. Executes the analysis and writes results.

### Default Abstract State

The default abstract state consists of:

- `FieldSensitivePointBasedHeap` for heap abstraction
- `IntegerConstantPropagation` for numeric analysis
- `InferredTypes` for type inference

## Development

Make sure to have the following prerequisites:

- Java 17 or later
- Maven 3.6+

To run locally using Gradle:

```bash
./gradlew run --args="-s path/to/File.java -o out/"
```

## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).
