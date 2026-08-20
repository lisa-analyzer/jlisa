// SPDX-FileCopyrightText: Copyright (C) 2011 The Android Open Source Project
// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright (C) 2025 The ARG-V Project
/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static int read(byte[] b, int count) throws Exception {
    int windowUpdateThreshold = Verifier.nondetInt();
    int unacknowledgedBytes = Verifier.nondetInt();
    int limit = Verifier.nondetInt();

    if (limit >= 0) {
      int pos = Verifier.nondetInt();
      if (pos == -1) {
        return -1;
      }
      if (pos >= 0) {
        assert (pos >= 0);
        int copied = 0;
        int bytesToCopy = Verifier.nondetInt();
        if (bytesToCopy >= 0) {
          //         drain from [pos..buffer.length)
          if (limit <= pos) {
            pos += bytesToCopy;
            copied += bytesToCopy;
            if (pos == Verifier.nondetInt()) {
              pos = 0;
            }
          }
          // drain from [pos..limit)
          if (copied < count) {
            pos += bytesToCopy;
            copied += bytesToCopy;
          }
        }
        if (pos == limit) {
          pos = -1;
          limit = 0;
        }
        assert (copied >= 0); // make note in document
        return copied;
      } else {
        return -1;
      }
    }
    return -1;
  }

  public static void main(String[] args) {
    int n = Verifier.nondetInt();
    if (n >= 0) {
      byte[] b = new byte[n];
      int count = Verifier.nondetInt();
      if (count > 0) {
        try {
          int copies = read(b, count);

        } catch (Exception e) {
          System.out.println(e);
        }
      }
    }
  }
}
