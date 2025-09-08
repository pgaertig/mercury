package pl.amitec.mercury.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Lazy<T> {

    private final Supplier<T> sValue;

    private T value;

    private Lazy(Supplier<T> value) {
        this.sValue = value;
    }

    public T get() {
        // Note that the following code is not thread safe. Thread safety
        // is not implemented here to keep the code simple, but can be
        // added easily.
        if (value == null) {
            value = sValue.get();
        }
        return value;
    }

    public <U> Lazy<U> map(Function<T, U> f) {
        return new Lazy<>(() -> f.apply(this.get()));
    }

    public <U> Lazy<U> map(Function<T, U> f, U defaultValue) {
        return new Lazy<>(() -> {
            try {
                return f.apply(this.get());
            } catch (Exception e) {
                return defaultValue;
            }
        });
    }

    public <U> Lazy<Optional<U>> mapOption(Function<T, U> f) {
        return new Lazy<>(() -> {
            try {
                return Optional.of(f.apply(this.get()));
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    public <U> Lazy<U> flatMap(Function<T, Lazy<U>> f) {
        return new Lazy<>(() -> f.apply(get()).get());
    }

    public void forEach(Consumer<T> c) {
        c.accept(get());
    }

    public static <T> Lazy<T> of(Supplier<T> t) {
        return new Lazy<>(t);
    }

    public static <T> Lazy<T> of(T t) {
        return new Lazy<>(() -> t);
    }

}
