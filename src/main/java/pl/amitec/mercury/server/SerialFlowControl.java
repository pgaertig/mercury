package pl.amitec.mercury.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.amitec.mercury.FlowControl;
import pl.amitec.mercury.TaskExecutor;

@Component
@ConditionalOnProperty(name = "mercury.flow-control", havingValue = "serial")
public class SerialFlowControl implements FlowControl {
    public void run(String name, Runnable runnable) {
        runnable.run();
    }

    @Override
    public TaskExecutor getExecutorService(String name) {
        //TODO implement
        throw new UnsupportedOperationException("Not implemented");
    }
}
