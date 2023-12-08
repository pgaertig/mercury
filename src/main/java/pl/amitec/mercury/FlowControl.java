package pl.amitec.mercury;

import java.time.Duration;

public interface FlowControl {
    void run(String name, Runnable runnable);

}
