#!/usr/bin/env python3
# jlisa-wit: SV-COMP wrapper for the jLISA Witness Validator.
#
# Bridges the BenchExec calling convention to the Java validator:
#
#   jlisa-wit --witness <witness.graphml|.yml> [--input-search] --property <file.prp> INPUT...
#   jlisa-wit --find-bug [--time-limit SECONDS] --property <file.prp> INPUT...
#
# The second form (bug-finding mode) has no witness: it looks for inputs that violate the
# property and prints "Violation found" or "Could not validate". jlisa's SV-COMP wrapper uses
# it when jlisa's analysis cannot decide.
#
# where INPUT... are the task's input files as BenchExec passes them, in the order of the
# task's input_files (for Java tasks: the shared common/ directory, the benchmark directory,
# and sometimes more, e.g. a category's own common/ or a subdirectory of the benchmark).
# The benchmark is the input that contains Main.java (the sv-benchmarks convention).
#
# The last line printed on stdout is always one of
#
#   Witness Correct        -> witness confirmed  (BenchExec: false)
#   Witness Spurious       -> witness rejected   (BenchExec: true)
#   Could not validate     -> no verdict         (BenchExec: unknown)
#   Violation found        -> (--find-bug) a run violates the property
#
# The property file is parsed here and passed to the validator by name (as JLiSA's
# SV-COMP wrapper does): valid-assert -> "assert", no-runtime-exception -> "runtime".
#
# A missing witness or property file, or an unsupported property, is an error:
# message on stderr, exit code 1.
# Only the Python 3 standard library is used.

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile

VERSION = "1.0.1"
MAIN_CLASS = "it.unive.jlisa.witness.validator.Main"
VERDICTS = ("Witness Correct", "Witness Spurious", "Could not validate", "Violation found")
BASE_DIR = getattr(sys, "_MEIPASS", os.path.dirname(os.path.realpath(__file__)))
LIB_DIR = os.path.join(BASE_DIR, "lib")

# LTL formula (whitespace removed) -> property name understood by the validator
PROPERTIES = {
    "Gassert": "assert",
    "G!uncaught(java.lang.RuntimeException)": "runtime",
}


def parse_arguments():
    parser = argparse.ArgumentParser(prog="jlisa-wit", description="jLISA Witness Validator")
    parser.add_argument("--version", action="version", version=VERSION)
    parser.add_argument("--witness")
    parser.add_argument("--input-search", action="store_true",
                        help="search for values of the nondet calls the witness does not fix")
    parser.add_argument("--find-bug", action="store_true",
                        help="no witness: look for inputs that violate the property")
    parser.add_argument("--time-limit", default="60", help="seconds for --find-bug (default 60)")
    parser.add_argument("--property", required=True)
    parser.add_argument("inputs", nargs="+")
    return parser.parse_args()


def parse_property(path):
    with open(path) as f:
        content = re.sub(r"\s+", "", f.read())
    match = re.match(r"^CHECK\(init\((\S+)\(\)\),LTL\((\S+)\)\)$", content)
    return PROPERTIES.get(match.group(2)) if match else None


def benchmark_inputs(paths):
    """The inputs to copy, and the benchmark among them (the input with Main.java).

    An input inside another input is left out: it is compiled with its parent, and a
    second copy would define its classes twice."""
    paths = [os.path.normpath(p) for p in paths]
    real = [os.path.realpath(p) for p in paths]
    inputs = [p for p, rp in zip(paths, real)
              if not any(rp != other and rp.startswith(other + os.sep) for other in real)]
    benchmark = next((p for p in inputs if os.path.isfile(os.path.join(p, "Main.java"))), inputs[-1])
    return inputs, benchmark


def main():
    args = parse_arguments()

    if not args.find_bug and not args.witness:
        print("--witness is required (or --find-bug)", file=sys.stderr)
        sys.exit(1)

    if not args.find_bug and not os.path.isfile(args.witness):
        print("witness file not found: " + args.witness, file=sys.stderr)
        sys.exit(1)
    
    if not os.path.isfile(args.property):
        print("property file not found: " + args.property, file=sys.stderr)
        sys.exit(1)

    prop = parse_property(args.property)
    if prop is None:
        print("unsupported property: " + args.property, file=sys.stderr)
        sys.exit(1)

    java = os.path.join(os.environ["JAVA_HOME"], "bin", "java") if "JAVA_HOME" in os.environ else "java"

    inputs, benchmark = benchmark_inputs(args.inputs)

    with tempfile.TemporaryDirectory(prefix="jlisa-wit-") as workdir:
        copies = []
        for i, path in enumerate(inputs):
            dest = os.path.join(workdir, str(i), os.path.basename(path))
            shutil.copytree(path, dest)
            copies.append(os.path.relpath(dest, workdir))
        # the validator finds the benchmark directory by the name of the task file
        task_file = os.path.join(workdir, os.path.basename(benchmark) + ".yml")
        with open(task_file, "w") as f:
            f.write("input_files:\n" + "".join("  - '%s/'\n" % c for c in copies))

        cmd = [java, "--add-modules", "jdk.jdi", "-cp", os.path.join(LIB_DIR, "*"), MAIN_CLASS,
               "--benchmark", task_file, "--property", prop]
        if args.find_bug:
            cmd += ["--find-bug", "--time-limit", args.time_limit]
        else:
            cmd += ["--witness", os.path.abspath(args.witness)]
            if args.input_search:
                cmd.append("--input-search")
        out = subprocess.run(cmd, stdout=subprocess.PIPE, universal_newlines=True).stdout

    verdicts = [line.strip() for line in out.splitlines() if line.strip() in VERDICTS]
    print(verdicts[-1] if verdicts else "Could not validate")


if __name__ == "__main__":
    main()
