package pl.amitec.mercury;

public interface FlowControl {
    void run(String name, Runnable runnable);

    TaskExecutor getExecutorService(String name);
}
