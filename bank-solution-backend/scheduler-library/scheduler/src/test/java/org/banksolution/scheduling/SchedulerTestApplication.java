package org.banksolution.scheduling;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.banksolution.scheduling.probe.SingleExecutionProbe;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SchedulerTestApplication {

    public static final String SINGLE_EXECUTION_PROBE_TASK_NAME = "single-execution-probe";

    @Bean
    public RecurringTask<Void> singleExecutionProbeTask() {
        return Tasks.recurring(SINGLE_EXECUTION_PROBE_TASK_NAME, FixedDelay.ofMillis(200))
                .execute((_, executionContext) -> SingleExecutionProbe.recordExecution(executionContext.getExecution()));
    }
}
