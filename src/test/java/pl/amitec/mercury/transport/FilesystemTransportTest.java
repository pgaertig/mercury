package pl.amitec.mercury.transport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import pl.amitec.mercury.formats.Charsets;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FilesystemTransportTest {
    private FilesystemTransport transport;

    @BeforeEach
    public void setUp(@TempDir(cleanup = CleanupMode.ALWAYS) Path tempDir) {
        Path tempDirPath = tempDir.resolve("deptDir");
        assertThat(tempDirPath.toFile().mkdirs(), is(true));
        transport = new FilesystemTransport(tempDirPath.toString(), false, Charsets.ISO_8859_2);
    }

    @Test
    public void testWrite() {
        transport.write("test.txt", "test");
        assertThat(transport.read("test.txt"), is("test"));
    }

    @Test
    public void testWriteUnmappableCharacters() {
        // Character \u203c\ufe0f is not mapped in ISO-8859-2
        transport.write("test2.txt", "Wa\u017cne \u203c\ufe0f");
        assertThat(transport.read("test2.txt"), is("Wa\u017cne __"));
    }
}
