package pl.amitec.mercury.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.amitec.mercury.*;

@Service
public class PlanExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(PlanExecutor.class);
    private final FlowControl flowControl;
    private final ConfigMapper configMapper;
    private final IntegratorDiscovery integratorDiscovery;

    public PlanExecutor(FlowControl flowControl,
                        IntegratorDiscovery integratorDiscovery,
                        ConfigMapper configMapper) {
        this.flowControl = flowControl;
        this.integratorDiscovery = integratorDiscovery;
        this.configMapper = configMapper;
    }

    public PlanExecution execute(Plan plan) {
        PlanExecution planExecution = new PlanExecution(){
            @Override
            public Plan getPlan() {
                return plan;
            }

            @Override
            public <T extends Configurable> T loadConfig(Class<T> object) {
                return configMapper.map(plan.config(), object);
            }
        };

        flowControl.run(plan.name(), () -> {
            Class<? extends Integrator> integratorClass = integratorDiscovery.getIntegrator(plan.integrator()).orElseThrow(
                    () -> new RuntimeException("Integrator not found: " + plan.integrator())
            );
            try {
                Integrator integrator = integratorClass.getConstructor().newInstance();
                integrator.configure(planExecution);
                if(integrator.test()) {
                    integrator.run();
                } else {
                    LOG.warn("Plan {} failed test", plan.name());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return planExecution;
    }
}
