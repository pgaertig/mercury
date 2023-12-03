package pl.amitec.mercury;


import org.springframework.stereotype.Service;
import pl.amitec.mercury.providers.polsoft.PolsoftToBitbeeIntegrator;
import pl.amitec.mercury.providers.redbay.RedbayToBitbeeIntegrator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads plans from classpath
 */
@Service
public class IntegratorDiscovery {

    private Map<String, Class<? extends Integrator>> integrators = new HashMap<>();
    public IntegratorDiscovery() {
        // TODO use ServiceLoader and Maven multi-module projects
        integrators.put("polsoft-bitbee", PolsoftToBitbeeIntegrator.class);
        integrators.put("redbay-bitbee", RedbayToBitbeeIntegrator.class);
    }

    public Optional<Class<? extends Integrator>> getIntegrator(String integratorName) {
        return Optional.ofNullable(integrators.get(integratorName));
    }
}
