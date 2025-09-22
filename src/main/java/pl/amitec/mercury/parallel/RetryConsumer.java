package pl.amitec.mercury.parallel;

import pl.amitec.mercury.MercuryException;

import java.util.function.Consumer;

public class RetryConsumer {
    public static <T> Consumer<T> retry(int maxRetries, int initialDelaySec, Consumer<T> consumer) {
        return item -> {
            try {
                ExponentialBackoff.run(maxRetries, initialDelaySec, () -> consumer.accept(item));
            } catch (Exception e) {
                throw new MercuryException("Task failed after max retries", e);
            }
        };
    }
}