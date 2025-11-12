// SPDX-FileCopyrightText: 2021 Falk Howar falk.howar@tu-dortmund.de
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

import java.io.IOException;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import org.sosy_lab.sv_benchmarks.Verifier;
import java.io.PrintWriter;
import securibench.micro.basic.Basic5;

public class Main {

  public static void main(String[] args) {
    HttpServletResponse res = new HttpServletResponse();
    PrintWriter p = res.getWriter();
    p.println("hello");
  }
}