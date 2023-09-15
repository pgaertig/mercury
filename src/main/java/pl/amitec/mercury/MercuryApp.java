package pl.amitec.mercury;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import pl.amitec.mercury.providers.polsoft.PsFlow;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static pl.amitec.mercury.util.StructUtils.propertiesToMap;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@Component
@Configuration
public class MercuryApp implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MercuryApp.class);

    public MercuryApp() {
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
        }
                );*/
        var source = "mm_ps_1";
        var pipeline = Thread.ofVirtual().name(source).unstarted(() -> {
            MDC.put("tenant", source);
            MDC.put("tenant-log", String.format("log/sources/%s/", source));
            LOG.info("Polsoft plugin for tenant: mm");
            Properties props = new Properties();
            try {
                props.load(new FileReader(
                        String.format("data/sources/%s.properties", source)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            PsFlow.configure(propertiesToMap(props)).watch();
        });
        pipeline.start();
    }
}