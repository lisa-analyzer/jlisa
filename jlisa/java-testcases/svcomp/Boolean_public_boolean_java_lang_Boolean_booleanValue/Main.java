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
    Boolean input_1_0 = Verifier.nondetBoolean();
    Boolean input_2_0 = Verifier.nondetBoolean();

    // Perform computation
    Boolean output_1 = input_1_0.booleanValue();
    Boolean output_2 = input_2_0.booleanValue();

    // Assert the expected outputs
    assert !(output_1 == true && output_2 == false); // desired values
  }
}
