package pl.amitec.mercury;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mercury.flow-control", havingValue = "serial")
public class SerialFlowControl implements FlowControl {
    public void run(String name, Runnable runnable) {
        runnable.run();
    }
}
