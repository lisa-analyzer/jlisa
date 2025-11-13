// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2024 The SV-Benchmarks Community
//
// SPDX-License-Identifier: Apache-2.0

import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    // Fetch inputs using Verifier.nondet* methods
    Long input_1_0 = Verifier.nondetLong();
    Long input_2_0 = Verifier.nondetLong();

    // Perform computation
    Float output_1 = input_1_0.floatValue();
    Float output_2 = input_2_0.floatValue();

    // Assert the expected outputs
    assert !(output_1 == -6.0 && output_2 == 9.223372e+18); // desired values
  }
}
