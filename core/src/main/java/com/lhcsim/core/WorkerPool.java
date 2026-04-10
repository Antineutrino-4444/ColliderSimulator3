package com.lhcsim.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated thread pool for event generation, off the GL thread.
 * <p>
 * Uses a {@link ForkJoinPool} with 2 worker threads. Results are placed
 * in a thread-safe queue that the render/physics thread can drain.
 * <p>
 * Queue is capped at 200 elements; oldest entries are dropped when full.
 *
 * @param <T> the result type (typically {@code GeneratedEvent})
 */
public class WorkerPool<T> {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);

    private static final int POOL_SIZE = 2;
    private static final int MAX_QUEUE_SIZE = 200;

    private final ForkJoinPool pool;
    private final BlockingQueue<T> resultQueue;

    public WorkerPool() {
        this.pool = new ForkJoinPool(POOL_SIZE);
        this.resultQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    }

    /**
     * Submits a task to the worker pool.
     *
     * @param task the callable to execute
     * @return a Future representing the pending result
     */
    public Future<T> submit(java.util.concurrent.Callable<T> task) {
        return pool.submit(() -> {
            try {
                T result = task.call();
                if (result != null) {
                    if (!resultQueue.offer(result)) {
                        // Queue full: drop oldest and retry
                        resultQueue.poll();
                        resultQueue.offer(result);
                    }
                }
                return result;
            } catch (Exception e) {
                log.error("Worker task failed", e);
                return null;
            }
        });
    }

    /**
     * Drains all available results from the queue.
     *
     * @param dest the list to drain results into
     * @return number of results drained
     */
    public int drain(java.util.List<T> dest) {
        return resultQueue.drainTo(dest);
    }

    /**
     * Polls for a single result without blocking.
     *
     * @return a result, or null if the queue is empty
     */
    public T poll() {
        return resultQueue.poll();
    }

    /**
     * Returns the number of results currently in the queue.
     */
    public int queueSize() {
        return resultQueue.size();
    }

    /**
     * Shuts down the worker pool gracefully.
     */
    public void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
