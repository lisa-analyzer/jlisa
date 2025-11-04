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
    Integer input_1_1 = Verifier.nondetInt();
    Long input_2_0 = Verifier.nondetLong();
    Integer input_2_1 = Verifier.nondetInt();

    // Perform computation
    String output_1 = Long.toString(input_1_0, input_1_1);
    String output_2 = Long.toString(input_2_0, input_2_1);

    // Assert the expected outputs
    assert !(output_1.equals("47009447931686273") && output_2.equals("2")); // desired values
  }
}
