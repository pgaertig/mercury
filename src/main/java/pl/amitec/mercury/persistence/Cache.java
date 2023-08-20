package pl.amitec.mercury.persistence;

import java.io.Closeable;
import java.util.function.Consumer;

public interface Cache extends Closeable {

    boolean hit(String tenant, String source, String resource, String key, String data, Consumer<String> dataConsumer);

    void close();

    void drop();
}
