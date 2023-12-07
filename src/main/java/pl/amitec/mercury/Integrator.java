package pl.amitec.mercury;

public interface Integrator {
    String getName();

    boolean configure(PlanExecution planExecution);

    boolean test() throws MercuryException, InterruptedException;

    void run() throws MercuryException, InterruptedException;
}
