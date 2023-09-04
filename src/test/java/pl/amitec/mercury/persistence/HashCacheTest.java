package pl.amitec.mercury.persistence;

import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HashCacheTest {

    @Mock
    Consumer<String> consumer;
    private HashCache cache;

    @BeforeEach
    public void setup() throws IOException {
        cache = new HashCache(Files.createTempFile("test-hash-cache", ".tmp").toString());
    }

    @Test
    public void testHitMiss() {

        //first time
        cache.hit("mm", "ps123", "p", "1", "{test: test}", consumer);
        verify(consumer, times(1)).accept(eq("{test: test}"));

        //should be cached
        cache.hit("mm", "ps123", "p", "1", "{test: test}", (data) -> fail());


        //should be cached
        cache.hit("mm", "ps123", "p", "1", "{test: test2}", consumer);
        verify(consumer, times(1)).accept(eq("{test: test2}"));

        //is updated
        cache.hit("mm", "ps123", "p", "1", "{test: test2}", (data) -> fail());

    }

    @Test
    public void testCollision() {

        //first time
        cache.hit("mm", "ps123", "p", "1", "{test: test}", consumer);
        verify(consumer, times(1)).accept(eq("{test: test}"));

        cache.hit("mm", "ps123", "p", "2", "{test: test}", consumer);
        verify(consumer, times(2)).accept(eq("{test: test}"));

        cache.hit("mm", "ps123", "p2", "1", "{test: test}", consumer);
        verify(consumer, times(3)).accept(eq("{test: test}"));

        cache.hit("mm", "ps123-2", "p", "1", "{test: test}", consumer);
        verify(consumer, times(4)).accept(eq("{test: test}"));

        cache.hit("mm2", "ps123", "p", "1", "{test: test}", consumer);
        verify(consumer, times(5)).accept(eq("{test: test}"));

        //each should be cached separately
        cache.hit("mm", "ps123", "p", "1", "{test: test}", (data) -> fail());
        cache.hit("mm", "ps123", "p", "2", "{test: test}", (data) -> fail());
        cache.hit("mm", "ps123", "p2", "1", "{test: test}", (data) -> fail());
        cache.hit("mm", "ps123-2", "p", "1", "{test: test}", (data) -> fail());
        cache.hit("mm2", "ps123", "p", "1", "{test: test}", (data) -> fail());
        verify(consumer, times(5)).accept(eq("{test: test}"));

    }

    @AfterEach
    public void teardown(){
        cache.close();
        cache.drop();
    }
}
