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
    String output_1 = Long.toBinaryString(input_1_0);
    String output_2 = Long.toBinaryString(input_2_0);

    // Assert the expected outputs
    assert !(output_1.equals("100100101110000100100001011101000100")
        && output_2.equals(
            "1111111111111111111111111111111100000000011001110010010000000101")); // desired values
  }
}
