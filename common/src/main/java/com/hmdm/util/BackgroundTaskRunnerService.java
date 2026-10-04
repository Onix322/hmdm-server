/*
 *
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hmdm.util;

import com.google.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service used for running the standalone tasks in background.
 *
 * @author isv
 */
@Singleton
public class BackgroundTaskRunnerService {

  private static final Logger logger = LoggerFactory.getLogger(BackgroundTaskRunnerService.class);

  /** An executor for the tasks to be executed in background. */
  private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

  /** An executor for the repeatable tasks to be executed in background. */
  private final ScheduledThreadPoolExecutor scheduledExecutor =
      (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);

  /**
   * Constructs new <code>BackgroundTaskRunnerService</code> instance. This implementation does
   * nothing.
   */
  public BackgroundTaskRunnerService() {
    Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
    Runtime.getRuntime().addShutdownHook(new Thread(scheduledExecutor::shutdown));
  }

  /**
   * Submits the specified task for execution in the background thread.
   *
   * @param task a task to be executed in background.
   */
  public void submitTask(Runnable task) {
    logger.debug(
        "Submitting task for execution: {}. The current state of executor: active tasks: {}, "
            + "tasks count: {}, queue size: {}",
        task,
        executor.getActiveCount(),
        executor.getTaskCount(),
        executor.getQueue().size());
    this.executor.submit(task);
  }

  /**
   * Submits the specified task for repeatable execution in the background thread at specified
   * periods.
   *
   * @param task a task to be executed in background.
   * @param initialDelay the time to delay first execution.
   * @param period the period between successive executions.
   * @param unit the time unit of the initialDelay and period parameters.
   * @return a ScheduledFuture representing pending completion of the series of repeated tasks. The
   *     future's {@link Future#get() get()} method will never return normally, and will throw an
   *     exception upon task cancellation or abnormal termination of a task execution.
   */
  public Future<?> submitRepeatableTask(
      Runnable task, long initialDelay, long period, TimeUnit unit) {
    logger.debug(
        "Submitting task for repeatable execution: {}. The current state of executor: active tasks:"
            + " {}, tasks count: {}, queue size: {}",
        task,
        executor.getActiveCount(),
        executor.getTaskCount(),
        executor.getQueue().size());
    return this.scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
  }

  public void shutdown() {
    scheduledExecutor.shutdownNow();
    executor.shutdownNow();

    awaitTermination(scheduledExecutor);
    awaitTermination(executor);
  }

  private void awaitTermination(ExecutorService executor) {
    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        logger.warn("Executor did not terminate within timeout");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.warn("Interrupted while waiting for executor termination", e);
    }
  }
}
