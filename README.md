# jLISA Witness Validator

A Java witness validator for the **SV-COMP Java track**, supporting both
format v1.0 (GraphML) and format v2.0 (YAML) witnesses, for both **violation**
and **correctness** witnesses.

---

## Key design features

| Feature | Description |
|---|---|
| **JDI runtime steering** | Runs the benchmark under the Java Debugger Interface (JDI). Injects return values into `Verifier.nondet*()` calls via `ThreadReference.forceEarlyReturn()` — no source modification, no recompilation, all JVM semantics intact. |
| **No producer-specific logic** | All witness parsing is content/structure-based. No `if (producer == "GDart")` branches anywhere. |
| **Cross-version JVM** | Validator runs on Java 23; benchmarks run on Java 8. The `--java` flag selects the Java 8 executable; JDWP is backwards-compatible. |
| **No shell invocations** | Compilation via `javax.tools.JavaCompiler`; JVM launch via `JDI LaunchingConnector`. |
| **Both v1 and v2** | Retrocompatibility with existing GraphML witnesses; ready for YAML v2.0 tools. |
| **Spurious detection** | Unconsumed-assumption counting and `avoid`-waypoint tracking detect spurious witnesses that older validators would accept as correct. |
| **Correctness witnesses** | Induction check (initiation / inductiveness / safety) via jLISA abstract interpretation. |

---

## SV-COMP Java properties

| Property file | `--property` | Meaning |
|---|---|---|
| `valid-assert.prp` | `assert` | No `assert` statement must fail (requires `-ea` flag) |
| `no-runtime-exception.prp` | `runtime` | No uncaught `java.lang.RuntimeException` |

The SV-COMP wrapper parses the `.prp` file and passes the name to the validator.
The property is never read from the task file (its `expected_verdict` is the ground truth).

---

## BenchExec output contract

| stdout | BenchExec result | Meaning |
|---|---|---|
| `Witness Correct` | confirmed | Violation reproduced / invariants verified |
| `Witness Spurious` | unknown | Path forced but no error; or invariants fail |
| `Could not validate` | unknown | Parse error, timeout, unsupported feature |

Exit code is always `0` (BenchExec interprets non-zero as a validator crash).

---

## Prerequisites

- **Java 23** JDK (required to run the validator)
- **Java 8** JDK (optional; recommended for SV-COMP benchmark execution)
- **Gradle** is handled by the included Gradle wrapper (`./gradlew`)
- **GitHub Packages credentials** for the LiSA SDK (used by jLISA):
  ```properties
  # ~/.gradle/gradle.properties
  gpr.user=<your-github-username>
  gpr.key=<your-github-token>   # needs read:packages scope
  ```
- **jLISA** source checkout — needed as a Gradle composite build. By default the
  build looks for it at `../jlisa` (sibling of this directory). Override with:
  ```bash
  # either a Gradle property...
  ./gradlew installDist -PjlisaPath=/absolute/path/to/jlisa
  # ...or an environment variable
  export JLISA_HOME=/absolute/path/to/jlisa
  ```

---

## Building

```bash
# Build and create the distribution layout under build/install/
./gradlew installDist

# The launcher script is generated at:
#   build/install/jlisa-witness-validator/bin/jlisa-witness-validator
```

---

## Running

### Standalone

```bash
# Using the generated launcher script (recommended — includes JVM flags)
./build/install/jlisa-witness-validator/bin/jlisa-witness-validator \
    --witness path/to/witness.graphml \
    --benchmark /abs/path/to/Task.yml \
    --property assert \
    [--java /usr/lib/jvm/java-8/bin/java] \
    [--verbose]

# Or via the SV-COMP wrapper, exactly as BenchExec calls it (see svcomp/README.md)
python3 svcomp/jlisa-wit.py \
    --witness path/to/witness.graphml \
    --property path/to/valid-assert.prp \
    path/to/common/ path/to/Task/
```

### With BenchExec

```bash
# First build the distribution
./gradlew installDist

# Then run BenchExec with the provided benchmark definitions
benchexec benchexec/validator-violation-witnesses.xml \
    --tool-directory . \
    -o results/
```

---

## Architecture

```
src/main/java/it/unive/jlisa/witness/validator/
├── Main.java                          # CLI entry point (Apache Commons CLI)
├── ValidationException.java
│
├── parser/
│   ├── WitnessModel.java              # Unified internal representation
│   ├── WitnessParserV1.java           # GraphML (v1) → WitnessModel
│   └── WitnessParserV2.java           # YAML (v2) → WitnessModel
│
├── model/
│   ├── ViolationPlan.java             # Ordered execution plan
│   ├── Interception.java              # (class, method, returnValue, count)
│   ├── BranchDecision.java            # (file, line, direction)
│   ├── AvoidPoint.java                # → Witness Spurious if reached
│   ├── TargetPoint.java               # → Witness Correct if reached
│   └── CorrectnessInvariant.java      # Loop invariant for correctness check
│
├── filter/
│   ├── JvmScopeParser.java            # assumption.scope → (className, methodName, descriptor)
│   ├── ValueExtractor.java            # assumption string → concrete value
│   └── WitnessFilter.java             # Drop java.lang.*, cprover, etc.
│
├── violation/
│   ├── ViolationValidator.java        # Orchestrates the JDI session
│   ├── JdiLauncher.java               # LaunchingConnector + JavaCompiler (no shell)
│   ├── EventLoop.java                 # MethodEntry / Breakpoint / Exception / VMDeath
│   ├── ForceReturnSteerer.java        # ThreadReference.forceEarlyReturn
│   ├── VariableSteerer.java           # StackFrame.setValue
│   └── ValidatorLogger.java           # Structured stderr logging
│
└── correctness/
    ├── CorrectnessValidator.java      # Orchestrates jLISA induction checks
    ├── LoopLocator.java               # Find CFG node at (file, line)
    ├── InvariantParser.java           # java_expression → ParsedInvariant
    └── InductionChecker.java          # Initiation / inductiveness / safety
```

---

## How violation validation works

The witness specifies which `Verifier.nondet*()` calls should return which values.
The validator:

1. Compiles the benchmark (if only `.java` sources are present) using `javax.tools.JavaCompiler` with `--release 8 -g`.
2. Launches the benchmark under JDI (`LaunchingConnector`) with `-ea` and the target classpath.
3. Registers `MethodEntryRequest`s for each intercepted method.
4. When the method is entered: `ThreadReference.forceEarlyReturn(vm.mirrorOf(value))` injects the witness-specified return value.
5. Registers `BreakpointRequest`s for target, avoid, and branching waypoints.
6. A target breakpoint or an uncaught exception after all interceptions → **Witness Correct**.
7. An avoid-point breakpoint, or the program exits without hitting the target → **Witness Spurious**.

### Partial witnesses (e.g. GDart)

GDart witnesses record only the calls that return `true`, omitting `false` returns.
Once the explicit interceptions are consumed, their `MethodEntryRequest` is disabled
and remaining calls run nondeterministically through the real JVM.

---

## How correctness validation works

1. Parse the benchmark sources with jLISA (`JavaFrontend`).
2. Run the LiSA analysis with the interval domain (`JavaNumericInterval`).
3. For each loop invariant in the witness, check three conditions:
   - **Initiation**: the abstract state at the loop head implies the invariant.
   - **Inductiveness**: executing the loop body preserves the invariant.
   - **Safety**: the invariant at loop exit implies the property.

**Current status**: initiation check is partially implemented; inductiveness and
safety require jLISA's per-variable interval extraction API (planned).  Until then,
these return `Could not validate`.

---

## Supported witness formats

| Format | Violation | Correctness |
|---|---|---|
| v1.0 (GraphML) | Yes | No (planned) |
| v2.0 (YAML) | Yes | Yes (partial) |

---

## Logging

All log output goes to **stderr**; only the verdict goes to **stdout** (required by BenchExec).

```
[PARSE]   Loaded v1 witness: 12 interception types
[FILTER]  Dropped 8 noise edges (java.lang.*, cprover)
[PLAN]    Interception #1: org.sosy_lab.sv_benchmarks.Verifier.nondetInt()I → return 0 (1 remaining)
[JDI]     Launching JVM: vmexec=java main=Main -ea
[JDI]     Connected to target VM (1.8.0_392)
[EVENT]   MethodEntry: Verifier.nondetInt() — forcing return value: 0
[EVENT]   Breakpoint: Main.java:17 — TARGET REACHED
[RESULT]  Witness Correct
```

Enable verbose logging with `--verbose`; default level is WARN for BenchExec runs.

---

## Known limitations

- String counterexamples: `Could not validate` (JDI can set String values; expression parsing needed)
- v1 correctness witnesses (GraphML automata): deferred to future work
- Object field invariants in correctness witnesses: deferred (linear arithmetic only)
- Multi-threaded programs: deferred
- Full inductiveness / safety checks: require jLISA per-variable interval API (in progress)

---

## References

- Ayaziová et al., *Software Verification Witnesses 2.0*, SPIN 2024
- SV-COMP: https://sv-comp.sosy-lab.org/
- BenchExec: https://github.com/sosy-lab/benchexec
- jLISA: https://github.com/lisa-analyzer/jlisa
- LiSA: https://github.com/lisa-analyzer/lisa
- Wit4Java (related work): https://github.com/wit4java/wit4java
