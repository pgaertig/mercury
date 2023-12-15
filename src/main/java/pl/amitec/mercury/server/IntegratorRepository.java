package pl.amitec.mercury.server;


import org.springframework.stereotype.Service;
import pl.amitec.mercury.Integrator;
import pl.amitec.mercury.integrators.Integrators;

import java.util.List;
import java.util.Optional;

/**
 * Loads integrators from classpath
 * TODO use ServiceLoader and Maven multi-module projects
 */
@Service
public class IntegratorRepository {

    public Optional<Class<? extends Integrator>> getIntegrator(String integratorName) {
        return Optional.ofNullable(Integrators.list.get(integratorName));
    }

    public List<String> getAllNames() {
        return Integrators.list.keySet().stream().toList();
    }
}
