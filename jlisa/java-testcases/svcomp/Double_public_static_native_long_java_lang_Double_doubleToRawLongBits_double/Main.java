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
    Long output_1 = Double.doubleToRawLongBits(input_1_0);
    Long output_2 = Double.doubleToRawLongBits(input_2_0);

    // Assert the expected outputs
    assert !(output_1 == 2441243367942312480L
        && output_2 == 5809738314299276546L); // desired values
  }
}
