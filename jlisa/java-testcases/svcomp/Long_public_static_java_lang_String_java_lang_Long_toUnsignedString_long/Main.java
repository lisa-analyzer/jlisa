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
    String output_1 = Long.toUnsignedString(input_1_0);
    String output_2 = Long.toUnsignedString(input_2_0);

    // Assert the expected outputs
    assert !(output_1.equals("18446744073709551615")
        && output_2.equals("1166379335717598")); // desired values
  }
}
