package pl.amitec.mercury.persistence;

import java.io.Closeable;
import java.util.function.Consumer;

public interface Cache extends Closeable {

    <E> boolean hit(String tenant, String source, String resource, String key, E data, Consumer<E> dataConsumer);

    void close();

    void drop();
}
