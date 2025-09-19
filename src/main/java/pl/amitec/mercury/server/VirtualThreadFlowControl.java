package pl.amitec.mercury.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.amitec.mercury.FlowControl;
import pl.amitec.mercury.TaskExecutor;

@Component
@ConditionalOnProperty(name = "mercury.flow-control", havingValue = "virtual-thread")
public class VirtualThreadFlowControl implements FlowControl {

    private static final Logger LOG = LoggerFactory.getLogger(VirtualThreadFlowControl.class);
    private String logsPath;
    private TaskExecutor taskExecutor;

    public VirtualThreadFlowControl(@Value("${mercury.plan.logs.path}") String logsPath) {
        this.logsPath = logsPath;
    }

    public void run(String name, Runnable runnable) {
        startLoggingThread(name, null, runnable);
    }

    private Thread startLoggingThread(String name, String subTask, Runnable runnable) {
        String plan = MDC.get("plan");
        String planLog = MDC.get("plan-log");
        if (plan == null || planLog == null) {
            plan = name;
            planLog = String.format("%s/%s/", logsPath, name);
            LOG.info("Logging plan {} to {}", plan, planLog);
        }
        final String finalPlan = plan;
        final String finalPlanLog = planLog;
        final String finalName = subTask == null ? name : name + "/" + subTask;
        return Thread.ofVirtual().name(finalName).start(() -> {
            MDC.put("plan", finalPlan);
            MDC.put("plan-log", finalPlanLog);
            LOG.info("Logging plan {} to {}", finalName, finalPlanLog);
            // TODO switch to scoped value
            // ScopedValue.where(ScopedValues.PLAN_SCOPE, new PlanScopeValue(finalPlan, finalPlanLog)).run(runnable);
            runnable.run();
        });
    }

    @Override
    public TaskExecutor getExecutorService(String name) {
        return new TaskExecutor() {
            @Override
            public void execute(String subTask, Runnable runnable) {
                startLoggingThread(name, subTask, runnable);
            }

            @Override
            public void join() {
                throw new UnsupportedOperationException("join");
            }
        };
    }
}
