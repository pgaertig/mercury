package pl.amitec.mercury.integrators.dynamics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.PlanExecution;

public class Flow {

    private static final Logger LOG = LoggerFactory.getLogger(Flow.class);
    private Config config;

    public Flow(PlanExecution execution) {
        config = execution.loadConfig(Config.class);
    }

    public void loop() {
    }

    public boolean test() {
        var authConfig = config.getDynamics().getAuth();
        OAuth2Session session = new OAuth2Session(
                authConfig.getAccessTokenUrl(),
                authConfig.getClientId(),
                authConfig.getClientSecret(),
                authConfig.getScope()
        );
        LOG.debug("access_token=" + session.getAccessToken());
        return true;
    }
}
