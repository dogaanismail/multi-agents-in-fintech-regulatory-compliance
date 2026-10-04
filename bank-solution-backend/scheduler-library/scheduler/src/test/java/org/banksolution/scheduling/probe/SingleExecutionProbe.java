package org.banksolution.scheduling.probe;

import com.github.kagkarlsson.scheduler.task.Execution;

import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class SingleExecutionProbe {

    private static final Queue<ProbeExecution> PROBE_EXECUTIONS = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger RUNNING_EXECUTIONS = new AtomicInteger();
    private static final AtomicInteger MAX_CONCURRENT_EXECUTIONS = new AtomicInteger();

    private SingleExecutionProbe() {
    }

    public static void recordExecution(Execution execution) {
        int runningExecutions = RUNNING_EXECUTIONS.incrementAndGet();
        MAX_CONCURRENT_EXECUTIONS.accumulateAndGet(runningExecutions, Math::max);
        try {
            PROBE_EXECUTIONS.add(new ProbeExecution(execution.executionTime, execution.pickedBy));
            TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } finally {
            RUNNING_EXECUTIONS.decrementAndGet();
        }
    }

    public static List<ProbeExecution> findProbeExecutions() {
        return List.copyOf(PROBE_EXECUTIONS);
    }

    public static int findMaxConcurrentExecutions() {
        return MAX_CONCURRENT_EXECUTIONS.get();
    }

    public record ProbeExecution(Instant executionTime, String pickedBy) {
    }
}
