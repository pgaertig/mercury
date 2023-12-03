package pl.amitec.mercury;

public interface Integrator {
    String getName();

    boolean testPlan(Plan plan) throws MercuryException, InterruptedException;

    void runPlan(Plan plan) throws MercuryException, InterruptedException;
}
