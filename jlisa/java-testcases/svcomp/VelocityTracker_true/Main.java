// SPDX-FileCopyrightText: Copyright (C) 2011 The Android Open Source Project
// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright (C) 2025 The ARG-V Project
/*
 * Copyright (C) 2006 The Android Open Source Project
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

/**
 * Helper for tracking the velocity of touch events, for implementing flinging and other such
 * gestures. Use obtain to retrieve a new instance of the class when you are going to begin
 * tracking, put the motion events you receive into it with addMovement(MotionEvent), and when you
 * want to determine the velocity call computeCurrentVelocity(int) and then getXVelocity() and
 * getXVelocity().
 */
/** filtered by ARG-V */
import org.sosy_lab.sv_benchmarks.Verifier;

public final class Main {

  static final int num_past = 10;
  static final int longestPastTime = 200;

  static final float mPastX[] = new float[num_past];
  static final float mPastY[] = new float[num_past];
  static final long mPastTime[] = new long[num_past];

  static float mYVelocity;
  static float mXVelocity;

  public static void addPoint(float x, float y, long time) {
    assert (time > 0);
    int drop = -1;
    int i;
    final long[] pastTime = mPastTime;
    for (i = 0; i < num_past; i++) {
      if (pastTime[i] == 0) {
        break;
      } else if (pastTime[i] < time - longestPastTime) {
        drop = i;
      }
    }
    assert (i <= num_past);

    if (i == num_past && drop < 0) {
      drop = 0;
    }

    if (drop == i) drop--;
    assert (drop == 0 || drop == i - 1);

    final float[] pastX = mPastX;
    final float[] pastY = mPastY;
    if (drop >= 0) {
      final int start = drop + 1;
      i -= (drop + 1);
    }
    pastX[i] = x;
    pastY[i] = y;
    pastTime[i] = time;
    i++;
    if (i < num_past) {
      pastTime[i] = 0;
    }
    assert (i < num_past && pastTime[i] == 0);
  }

  /**
   * Compute the current velocity based on the points that have been collected. Only call this when
   * you actually want to retrieve velocity information, as it is relatively expensive. You can then
   * retrieve the velocity with {@link #getXVelocity()} and {@link #getYVelocity()}.
   *
   * @param units The units you would like the velocity in. A value of 1 provides pixels per
   *     millisecond, 1000 provides pixels per second, etc.
   */
  public static void computeCurrentVelocity(int units) {
    assert (units != 0);
    final float[] pastX = mPastX;
    final float[] pastY = mPastY;
    final long[] pastTime = mPastTime;
    // todo : generate random values for past time and try to check last assertion
    // Kind-of stupid.
    final float oldestX = pastX[0];
    final float oldestY = pastY[0];
    final long oldestTime = pastTime[0];
    float accumX = 0;
    float accumY = 0;
    int n = 0;
    while (n < num_past) {
      if (pastTime[n] == 0) {
        break;
      }
      n++;
    }
    assert (n < num_past);

    // Skip the last received event, since it is probably pretty noisy.
    if (n > 3) n--;
    assert (n <= 3);
    for (int i = 1; i < n; i++) {
      final int dur = (int) (pastTime[i] - oldestTime);
      if (dur == 0) {
        continue;
      }
      float dist = pastX[i] - oldestX;
      float vel = (float) (dist / dur) * units; // pixels/frame.
      if (accumX == 0) accumX = vel;
      else accumX = (accumX + vel) * .5f;

      dist = pastY[i] - oldestY;
      vel = (dist / dur) * units; // pixels/frame.
      if (accumY == 0) accumY = vel;
      else accumY = (accumY + vel) * .5f;
    }
    mXVelocity = accumX;
    mYVelocity = accumY;
    assert (accumX != 0 || accumY != 0);
  }

  public static void main(String[] args) {
    float x = Verifier.nondetFloat();
    float y = Verifier.nondetFloat();
    long time = Verifier.nondetLong();
    int units = Verifier.nondetInt();

    // updated

    if (time > 0 && units > 0) {
      addPoint(x, y, time);
      computeCurrentVelocity(units);
    }
  }
}