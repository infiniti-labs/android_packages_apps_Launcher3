/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.launcher3.util;

import android.util.Log;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public class TaskSchedule {

    static final String TAG = "TaskSchedule";

    public static void runTasks(List<SafelyRunnable> runnableList, Executor executor,
            int parallelTasks) {
        ConcurrentLinkedQueue<SafelyRunnable> queue = new ConcurrentLinkedQueue<>(runnableList);
        CountDownLatch remaining = new CountDownLatch(queue.size());
        Runnable drain = () -> {
            SafelyRunnable runnable;
            while ((runnable = queue.poll()) != null) {
                runnable.run();
                remaining.countDown();
            }
        };

        int helperCount = Math.min(queue.size() - 1, parallelTasks);
        for (int i = 0; i < helperCount; ++i) {
            executor.execute(drain);
        }
        drain.run();

        boolean interrupted = false;
        while (true) {
            try {
                remaining.await();
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public static abstract class SafelyRunnable implements Runnable {

        @Override
        public final void run() {
            try {
                onTaskRun();
            } catch (Throwable e) {
                Log.e(TAG, "SafelyRunnable-Throwable:" + e.getMessage());
            }
        }

        public abstract void onTaskRun();
    }
}
