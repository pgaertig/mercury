package pl.amitec.mercury.integrators.polsoft;

import pl.amitec.mercury.Integrator;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.PlanExecution;

public class PolsoftToBitbeeIntegrator implements Integrator {

    public static final String NAME = "polsoft-bitbee";
    private PlanExecution planExecution;

    public String getName() {
        return NAME;
    }

    public boolean configure(PlanExecution planExecution) {
        this.planExecution = planExecution;
        return true;
    }

    public boolean test() throws MercuryException, InterruptedException {
        // TODO implement
        return true;
    }

    public void run() throws MercuryException, InterruptedException {
        PsFlow.configure(planExecution.getPlan().config()).watch();
    }
}
