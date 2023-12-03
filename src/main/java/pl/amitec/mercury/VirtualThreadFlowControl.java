package pl.amitec.mercury;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mercury.flow-control", havingValue = "virtual-thread")
public class VirtualThreadFlowControl implements FlowControl {

    private String logsPath;

    public VirtualThreadFlowControl(@Value("${mercury.plan.logs.path}") String logsPath) {
        this.logsPath = logsPath;
    }

    public void run(String name, Runnable runnable) {
        Thread.ofVirtual().name(name).start(() -> {
            MDC.put("plan", name);
            MDC.put("plan-log", String.format("%s/%s/", logsPath, name));
            runnable.run();
        });
    }
}
