package pl.amitec.mercury.integrators.dynamics;

import pl.amitec.mercury.Integrator;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.PlanExecution;

public class BusinessCentralToBitbeeIntegrator implements Integrator {

    public static final String NAME = "dynamics-bitbee";
    private PlanExecution planExecution;

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Microsoft Dynamics 365 Business Central to Bitbee";
    }

    public boolean configure(PlanExecution planExecution) {
        this.planExecution = planExecution;
        return true;
    }

    public boolean test() throws MercuryException, InterruptedException {
        return new Flow(planExecution).test();
    }

    public void run() throws MercuryException, InterruptedException {
        new Flow(planExecution).loop();
    }
}
