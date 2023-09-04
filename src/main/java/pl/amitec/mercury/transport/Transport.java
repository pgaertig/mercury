package pl.amitec.mercury.transport;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

public interface Transport {
    Transport subdir(String path, Charset charset);

    Transport subdir(String path);

    List<String> listFiles() throws IOException;

    String read(String path, String mode);

    String read(String path);

    void write(String path, String content);

    List<String> readlines(String path);

    Reader reader(String path);

    boolean exists(String path);

    void delete(String path);
}
