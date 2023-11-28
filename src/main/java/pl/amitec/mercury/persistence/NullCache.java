package pl.amitec.mercury.persistence;

import java.util.function.Consumer;

public class NullCache implements Cache {
    @Override
    public <E> boolean hit(String tenant, String source, String resource, String key, E data,
                       Consumer<E> dataConsumer) {
        dataConsumer.accept(data);
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public void drop() {

    }
}
