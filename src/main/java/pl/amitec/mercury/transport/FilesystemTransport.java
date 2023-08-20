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

    public static class TransportException extends RuntimeException {
        public TransportException(String message) {
            super(message);
        }
    }

    public FilesystemTransport(String directory, boolean readonly, Charset charset) {
        if (directory == null) {
            throw new TransportException("No directory");
        }
        this.directory = Paths.get(directory).toAbsolutePath();
        this.readonly = readonly;
        this.charset = charset;
    }

    public static FilesystemTransport configure(Map<String, String> config, boolean readonly, String mode) {
        return new FilesystemTransport(config.get("filesystem.path"), readonly, Charset.forName(mode));
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
    public List<String> listFiles() throws IOException {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public String read(String path, String mode) throws IOException {
        return Files.readString(directory.resolve(path));
    }

    @Override
    public String read(String path) throws IOException {
        return Files.readString(directory.resolve(path), charset);
    }

    @Override
    public void write(String path, String content) throws IOException {
        if (readonly) {
            throw new TransportException("Read only " + directory);
        }
        Files.writeString(directory.resolve(path), content, charset);
    }

    @Override
    public List<String> readlines(String path) throws IOException {
        return Files.readAllLines(directory.resolve(path), charset);
    }

    @Override
    public Reader reader(String path) throws IOException {
        return Files.newBufferedReader(directory.resolve(path), charset);
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
        // Actually, the delete method is not deleting the file, so we're keeping it consistent.
    }

    // Main method for testing purposes
    public static void main(String[] args) {
        // You can add some tests here if you'd like
    }
}