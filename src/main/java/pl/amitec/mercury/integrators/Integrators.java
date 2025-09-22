package pl.amitec.mercury.integrators;

import pl.amitec.mercury.Integrator;
import pl.amitec.mercury.integrators.polsoft.PolsoftToBitbeeIntegrator;

import java.util.Map;

public interface Integrators {
    Map<String, Class<? extends Integrator>> list = Map.of(
        PolsoftToBitbeeIntegrator.NAME, PolsoftToBitbeeIntegrator.class
    );
}
