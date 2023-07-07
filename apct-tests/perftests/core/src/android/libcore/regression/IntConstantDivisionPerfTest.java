/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.libcore.regression;

import android.perftests.utils.BenchmarkState;
import android.perftests.utils.PerfStatusReporter;
import android.test.suitebuilder.annotation.LargeTest;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class IntConstantDivisionPerfTest {
    @Rule public PerfStatusReporter mPerfStatusReporter = new PerfStatusReporter();

    @Test
    public void timeDivideIntByConstant2() {
        int result = 1;
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            result /= 2;
        }
    }

    @Test
    public void timeDivideIntByConstant8() {
        int result = 1;
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            result /= 8;
        }
    }

    @Test
    public void timeDivideIntByConstant10() {
        int result = 1;
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            result /= 10;
        }
    }

    @Test
    public void timeDivideIntByConstant100() {
        int result = 1;
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            result /= 100;
        }
    }

    @Test
    public void timeDivideIntByConstant100_HandOptimized() {
        int result = 1;
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            result = (int) ((0x51eb851fL * result) >>> 37);
        }
    }

    @Test
    public void timeDivideIntByConstant2048() {
        int result = 1;
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            result /= 2048;
        }
    }

    @Test
    public void timeDivideIntByVariable2() {
        int result = 1;
        int factor = 2;
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            result /= factor;
        }
    }

    @Test
    public void timeDivideIntByVariable10() {
        int result = 1;
        int factor = 10;
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            result /= factor;
        }
    }
}
