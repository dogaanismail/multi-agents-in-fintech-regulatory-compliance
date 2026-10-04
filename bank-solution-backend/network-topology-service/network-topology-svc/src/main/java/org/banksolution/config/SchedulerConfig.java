package org.banksolution.config;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.banksolution.service.GraphAlgorithmScheduledService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerConfig {

    public static final String GRAPH_ALGORITHM_COMPUTATION_TASK_NAME = "graph-algorithm-computation";

    @Bean
    public RecurringTask<Void> graphAlgorithmComputationTask(
            GraphAlgorithmScheduledService graphAlgorithmScheduledService,
            @Value("${app.graph.algorithm.interval-ms}") long graphAlgorithmComputationIntervalMs) {

        return Tasks.recurring(GRAPH_ALGORITHM_COMPUTATION_TASK_NAME, FixedDelay.ofMillis(graphAlgorithmComputationIntervalMs))
                .execute((_, _) -> graphAlgorithmScheduledService.computeAllMetrics());
    }
}
