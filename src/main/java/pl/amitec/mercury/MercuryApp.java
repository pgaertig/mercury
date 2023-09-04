package pl.amitec.mercury;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.helpers.BasicMDCAdapter;
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

import static pl.amitec.mercury.util.Utils.orderedMapOfStrings;

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
        var pipeline = Thread.ofVirtual().name("ps_mm1").unstarted(() -> {
            MDC.put("tenant", "ps_mm1");
            LOG.info("Polsoft plugin for tenant: mm");
            PsFlow.configure(
                    orderedMapOfStrings(
                            "tenant", "mm",
                            "system", "polsoft",
                            "source", "filesystem",
                            "filesystem.path", "data",
                            //"redbay.url" => 'http://mm.luxor.aox.pl/api',
                            "redbay.url", "https://panel.b2b-online.pl/api",
                            "redbay.apikey", "***REMOVED***",
                            "redbay.auth_id", "***REMOVED***",
                            "redbay.auth_pass", "***REMOVED***",
                            "redbay.dry_run", "true",
                            "ftp.host", "ftp.redbay.pl",
                            "ftp.user", "minimaxi",
                            "ftp.password", "***REMOVED***",
                            "polsoft.department", "1"
                    )
            ).watch();
        });
        pipeline.start();
    }
}