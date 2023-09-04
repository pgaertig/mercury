package pl.amitec.mercury.transport;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class FilesystemTransport implements Transport {

    private final Path directory;
    private final boolean readonly;
    private final Charset charset;

    public FilesystemTransport(String directory, boolean readonly, Charset charset) {
        Objects.requireNonNull(directory);
        this.directory = Paths.get(directory).toAbsolutePath();
        this.readonly = readonly;
        this.charset = charset;
    }

    public static FilesystemTransport configure(Map<String, String> config, boolean readonly, String mode) {
        return new FilesystemTransport(config.get("filesystem.path"), readonly, Charset.forName(mode));
    }

    public static FilesystemTransport configure(Map<String, String> config, boolean readonly, Charset charset) {
        return new FilesystemTransport(config.get("filesystem.path"), readonly, charset);
    }

    public static FilesystemTransport configure(String path, boolean readonly, String mode) {
        return new FilesystemTransport(path, readonly, Charset.forName(mode));
    }

    @Override
    public FilesystemTransport subdir(String path, Charset charset) {
        return new FilesystemTransport(directory.resolve(path).toString(), readonly, charset);
    }

    @Override
    public FilesystemTransport subdir(String path) {
        return subdir(path, charset);
    }

    @Override
    public List<String> listFiles() {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new TransportException(this, e);
        }
    }

    @Override
    public String read(String path, String mode) {
        try {
            return Files.readString(directory.resolve(path));
        } catch (IOException e) {
            throw new TransportException(this, e);
        }
    }

    @Override
    public String read(String path)  {
        try {
            return Files.readString(directory.resolve(path), charset);
        } catch (IOException e) {
            throw new TransportException(this, e);
        }
    }

    @Override
    public void write(String path, String content) {
        if (readonly) {
            throw new TransportException("Read only " + directory);
        }
        try {
            Files.writeString(directory.resolve(path), content, charset);
        } catch (IOException e) {
            throw new TransportException(this, e);
        }
    }

    @Override
    public List<String> readlines(String path) {
        try {
            return Files.readAllLines(directory.resolve(path), charset);
        } catch (IOException e) {
            throw new TransportException(this, e);
        }
    }

    @Override
    public Reader reader(String path) {
        try {
            return Files.newBufferedReader(directory.resolve(path), charset);
        } catch (IOException e) {
            throw new TransportException(this, e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(directory.resolve(path));
    }

    @Override
    public void delete(String path) {
        if (readonly) {
            throw new TransportException("Read-only transport - ignore delete of " + path);
        }
        // TODO implement
    }

    @Override
    public String toString() {
        return "FilesystemTransport{" +
                "directory=" + directory +
                ", readonly=" + readonly +
                ", charset=" + charset +
                '}';
    }
}