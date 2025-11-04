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
    Long input_1_1 = Verifier.nondetLong();
    Long input_2_0 = Verifier.nondetLong();
    Long input_2_1 = Verifier.nondetLong();

    // Perform computation
    Long output_1 = Long.max(input_1_0, input_1_1);
    Long output_2 = Long.max(input_2_0, input_2_1);

    // Assert the expected outputs
    assert !(output_1 == 217954712855209564L && output_2 == 77L); // desired values
  }
}
