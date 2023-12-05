package pl.amitec.mercury.integrators.polsoft;

import pl.amitec.mercury.Integrator;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.Plan;

public class PolsoftToBitbeeIntegrator implements Integrator {
    public String getName() {
        return "polsoft-bitbee";
    }

    public boolean testPlan(Plan plan) throws MercuryException, InterruptedException {
        // TODO implement
        return false;
    }

    public void runPlan(Plan plan) throws MercuryException, InterruptedException {
        PsFlow.configure(plan.config()).watch();
    }
}
