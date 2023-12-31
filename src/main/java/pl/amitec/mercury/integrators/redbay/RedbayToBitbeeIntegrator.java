package pl.amitec.mercury.integrators.redbay;

import pl.amitec.mercury.Integrator;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.Plan;
import pl.amitec.mercury.PlanExecution;

public class RedbayToBitbeeIntegrator implements Integrator{

    public static final String NAME = "redbay-bitbee";
    private PlanExecution planExecution;

    public String getName() {
        return NAME;
    }

    public boolean configure(PlanExecution planExecution) throws MercuryException {
        this.planExecution = planExecution;
        return true;
    }

    public boolean test() throws MercuryException, InterruptedException {
        return true;
    }

    public void run() throws MercuryException, InterruptedException {
        new RedbayToBitbeePlan().configure(planExecution.getPlan().config()).run();
    }
}
