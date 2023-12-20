package pl.amitec.mercury.server;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import pl.amitec.mercury.Plan;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@Component
@Configuration
@EnableScheduling
public class MercuryApp implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MercuryApp.class);

    private PlanLoader planLoader;
    private PlanExecutor planExecutor;

    public MercuryApp(
            PlanLoader planLoader,
            PlanExecutor planExecutor) {
        this.planLoader = planLoader;
        this.planExecutor = planExecutor;
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
            if(plan.enabled()) {
                try {
                    LOG.info("Starting plan: {}", plan.name());
                    planExecutor.execute(plan);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                LOG.info("Skipping plan {} as it is disabled", plan.name());
            }
        });
    }

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    //@Bean
    /* Startup locking issues */
    @Deprecated
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            LockSupport.park();
        };
    }

}