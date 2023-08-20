package pl.amitec.mercury.persistence;

import java.util.function.Consumer;

public class NullCache implements Cache {
    @Override
    public boolean hit(String tenant, String source, String resource, String key, String data,
                       Consumer<String> dataConsumer) {
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
