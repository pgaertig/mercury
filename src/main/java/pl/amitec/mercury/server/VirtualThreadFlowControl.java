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

    public VirtualThreadFlowControl(@Value("${mercury.plan.logs.path}") String logsPath) {
        this.logsPath = logsPath;
    }

    public void run(String name, Runnable runnable) {
        String plan = MDC.get("plan");
        String planLog = MDC.get("plan-log");
        if (plan == null || planLog == null) {
            plan = name;
            planLog = String.format("%s/%s/", logsPath, name);
            LOG.info("Logging plan {} to {}", plan, planLog);
        }
        String finalPlan = plan;
        String finalPlanLog = planLog;
        Thread.ofVirtual().name(name).start(() -> {
            MDC.put("plan", finalPlan);
            MDC.put("plan-log", finalPlanLog);
            runnable.run();
        });
    }

    public TaskExecutor getExecutorService(String name) {
        throw new UnsupportedOperationException();
    }
}
