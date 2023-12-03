package pl.amitec.mercury;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import pl.amitec.mercury.engine.PlanLoader;

import java.util.List;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@Component
@Configuration
public class MercuryApp implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MercuryApp.class);

    private PlanLoader planLoader;
    private IntegratorDiscovery integratorDiscovery;

    private FlowControl flowControl;

    public MercuryApp(
            PlanLoader planLoader,
            IntegratorDiscovery integratorDiscovery,
            FlowControl flowControl) {
        this.planLoader = planLoader;
        this.integratorDiscovery = integratorDiscovery;
        this.flowControl = flowControl;
    }

    public static void main(String[] args) {
        SpringApplication.run(MercuryApp.class, args);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        LOG.debug("Logger factory: {}", LoggerFactory.getILoggerFactory());
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        /*ctx.setMDCAdapter( new LogbackMDCAdapter() {
            delegate = new BasicMDCAdapter();
        });*/

        List<Plan> plans = planLoader.getAllPlans();
        LOG.info("Found {} plans", plans.size());
        plans.forEach(plan -> {
            try {
                LOG.info("Starting plan: {}", plan.name());
                flowControl.run(plan.name(), () -> {
                    Class<? extends Integrator> integratorClass = integratorDiscovery.getIntegrator(plan.integrator()).orElseThrow(
                            () -> new RuntimeException("Integrator not found: " + plan.integrator())
                    );
                    try {
                        Integrator integrator = integratorClass.getConstructor().newInstance();
                        integrator.testPlan(plan);
                        integrator.runPlan(plan);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}