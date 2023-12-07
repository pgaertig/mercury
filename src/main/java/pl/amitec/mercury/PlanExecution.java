package pl.amitec.mercury;

public interface PlanExecution {

    Plan getPlan();
    <T extends Configurable> T loadConfig(Class<T> object);
}
