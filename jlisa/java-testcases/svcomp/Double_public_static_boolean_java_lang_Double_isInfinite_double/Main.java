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
    Double input_1_0 = Verifier.nondetDouble();
    Double input_2_0 = Verifier.nondetDouble();

    // Perform computation
    Boolean output_1 = Double.isInfinite(input_1_0);
    Boolean output_2 = Double.isInfinite(input_2_0);

    // Assert the expected outputs
    assert !(output_1 == false && output_2 == true); // desired values
  }
}
