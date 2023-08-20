package pl.amitec.mercury.transport;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

public interface Transport {
    FilesystemTransport subdir(String path, Charset charset);

    FilesystemTransport subdir(String path);

    List<String> listFiles() throws IOException;

    String read(String path, String mode) throws IOException;

    String read(String path) throws IOException;

    void write(String path, String content) throws IOException;

    List<String> readlines(String path) throws IOException;

    Reader reader(String path) throws IOException;

    boolean exists(String path);

    void delete(String path);
}
