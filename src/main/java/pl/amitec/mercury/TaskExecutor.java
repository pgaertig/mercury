package pl.amitec.mercury;

public interface TaskExecutor {
    void execute(String name, Runnable runnable);
    void join();
}
