package pl.amitec.mercury.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.amitec.mercury.FlowControl;

@Component
@ConditionalOnProperty(name = "mercury.flow-control", havingValue = "serial")
public class SerialFlowControl implements FlowControl {
    public void run(String name, Runnable runnable) {
        runnable.run();
    }
}
