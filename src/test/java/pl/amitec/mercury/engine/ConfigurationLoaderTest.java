package pl.amitec.mercury.engine;

import org.junit.jupiter.api.Test;
import pl.amitec.mercury.providers.polsoft.PsConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigurationLoaderTest {

    @Test
    void testSimpleConfig() throws Exception {
        PsConfig psConfig = ConfigurationLoader.loadConfiguration(PsConfig.class, "classpath:/configs/ps1.properties");
        assertEquals("mm", psConfig.getTenant());
    }
}
