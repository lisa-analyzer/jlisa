# Witness validator examples

A small SV-COMP task, with two violation witnesses of it, to try the witness validator (jlisa-wit) from the
sources. The task is `nondet-assert/Main.java`: the assertion fails when `Verifier.nondetInt()` returns 0.

- `common/`: the `Verifier` and `ObjectFactory` classes of [SV-Benchmarks](https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks) (Apache-2.0)
- `nondet-assert/Main.java`: the program
- `valid-assert.prp`: the property
- `witness-correct.graphml`: a violation witness in the GraphML format (v1)
- `witness-correct.yml`: the violation witness (format 2.4) that jlisa writes in bug-finding mode

Run from the `jlisa` folder (the one with `build.gradle.kts`), since the paths below, and those inside
`witness-correct.yml`, are relative to it:

```bash
./gradlew runWitnessValidator --args="--witness witness-validator-examples/witness-correct.graphml --svcomp-property witness-validator-examples/valid-assert.prp --source witness-validator-examples/common witness-validator-examples/nondet-assert"
./gradlew runWitnessValidator --args="--witness witness-validator-examples/witness-correct.yml --svcomp-property witness-validator-examples/valid-assert.prp --source witness-validator-examples/common witness-validator-examples/nondet-assert"
```

Both witnesses are correct, so the expected verdict is `Witness Correct`. The validator does not read the
2.x layout yet (a list of entries), so for now the second one gives `Could not validate` ("YAML witness root
is not a mapping").

`--source` takes all the following arguments, hence it comes last.

The bug-finding mode (`runBugFinder`, that is jlisa-wit `--find-bug`) needs no witness: it looks for values of
the `Verifier.nondet*()` calls that violate the property, prints `Violation found` or `Could not validate`,
and writes the violation witness of the run it found (here to `build/`, so that the example witness stays as
it is; `--time-limit SECONDS`, default 60):

```bash
./gradlew runBugFinder --args="--witness-output build/witness-found.yml --svcomp-property witness-validator-examples/valid-assert.prp --source witness-validator-examples/common witness-validator-examples/nondet-assert"
```

`witness-correct.yml` was written this way.
