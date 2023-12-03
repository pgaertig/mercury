package pl.amitec.mercury.providers.redbay;

import pl.amitec.mercury.Integrator;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.Plan;

public class RedbayToBitbeeIntegrator implements Integrator{
    public String getName() {
        return "redbay-to-bitbee";
    }

    public boolean testPlan(Plan plan) throws MercuryException, InterruptedException {
        return true;
    }

    public void runPlan(Plan plan) throws MercuryException, InterruptedException {
        new RedbayToBitbeePlan().configure(plan.config()).run();
    }
}
