package pl.amitec.mercury.parallel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class ExponentialBackoff {

        private static final Logger LOG = LoggerFactory.getLogger(ExponentialBackoff.class);

        private static final Random random = new Random();

        public static void run(int maxTries, int initialDelay, Runnable task) throws Exception {
            long curDelay = initialDelay;
            int curTries = 1;

            while (curTries <= maxTries) {
                try {
                    task.run();
                    return;  // Exit if the task is successful
                } catch (Exception e) {
                    if (curTries == maxTries) {
                        throw e; //rethrow last exception
                    }

                    curDelay = curDelay * 2 + 1;  // 1, 3, 7, 15, 31, 63

                    // Add 10% randomness
                    long randomAddition = (long) (curDelay * 0.10 * random.nextDouble());
                    curDelay += randomAddition;

                    LOG.warn(String.format("Attempt %s in %s s because of: %s", curTries + 1, curDelay, e.getMessage()), e);
                    Thread.sleep(curDelay * 1000);
                    curTries++;
                }
            }
        }
}
