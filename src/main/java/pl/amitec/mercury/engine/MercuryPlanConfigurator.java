package pl.amitec.mercury.engine;

import pl.amitec.mercury.MercuryException;

import java.util.Map;

public interface MercuryPlanConfigurator {
    MercuryPlanRun configure(Map<String, String> config) throws MercuryException, InterruptedException;
}
