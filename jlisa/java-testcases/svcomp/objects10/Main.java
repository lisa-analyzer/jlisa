/*
 * Contributed to SV-COMP by Marvin Lazar and Falk Howar
 * License: MIT (see /java/objects/LICENSE-MIT)
 *
 * SPDX-FileCopyrightText: 2025 Marvin Lazar and Falk Howar, TU Dortmund University
 * SPDX-FileCopyrightText: 2025 The SV-Benchmarks Community
 * SPDX-License-Identifier: MIT
 */

import org.sosy_lab.sv_benchmarks.Verifier;
import svcomp.objects.*;

public class Main {

  public static void main(String[] args) {
    Any o = Verifier.nondetObject(Any.class, new Factories.AnyFactory());
    // assertion violation reachable for type D
    if (!(o instanceof A)) {
      assert false;
    }
  }
}
