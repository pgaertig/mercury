package pl.amitec.mercury.server;


import org.springframework.stereotype.Service;
import pl.amitec.mercury.Integrator;
import pl.amitec.mercury.integrators.polsoft.PolsoftToBitbeeIntegrator;
import pl.amitec.mercury.integrators.redbay.RedbayToBitbeeIntegrator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads integrators from classpath
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

    public List<String> getAllNames() {
        return integrators.keySet().stream().toList();
    }
}
