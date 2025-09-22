package pl.amitec.mercury.integrators.polsoft;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.net.FTPHelper;
import pl.amitec.mercury.transport.FilesystemTransport;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.amitec.mercury.util.Utils.sha1HexDigest;

/**
 * Synchronizes data with FTP server with PS2000 structure
 */
public class PolsoftFtp {

    public static final String STATE_JSON = "state.json";
    private static final Logger LOG = LoggerFactory.getLogger(PolsoftFtp.class);

    private static final String[] REQUIRED_FILES = {
            "grupy.txt", "klienci.txt", "produc.txt", "rabaty.txt",
            "rozrach.txt", "stany.txt", "toppc.txt", "towary.txt", "zestawy.txt", "zestpoz.txt"
    };
    private Boolean rerunLast;

    private ContentState prevState;
    private String lastRootDigest = "none";
    private String lastExportDigest = "none";
    private String lastContentDigest = "none";
    private String host;
    private int port;
    private String user;
    private String password;
    private final String cacheDir;
    private final Boolean readonly;

    public static PolsoftFtp configure(Map<String, String> properties) {
        var name = properties.get("name");
        return new PolsoftFtp(
            properties.getOrDefault("polsoft.ftp.host", "localhost"),
            Integer.parseInt(properties.getOrDefault("polsoft.ftp.port", "21")),
            properties.getOrDefault("polsoft.ftp.user", "anonymous"),
            properties.getOrDefault("polsoft.ftp.password", "anonymous"),
            properties.getOrDefault("polsoft.ftp.cacheDir", String.format("data/%s/ftp", name)),
            Boolean.parseBoolean(properties.getOrDefault("polsoft.ftp.readonly", "false")),
            Boolean.parseBoolean(properties.getOrDefault("polsoft.ftp.rerun-last", "false"))
        );
    }

    public PolsoftFtp(PolsoftFtpConfig config) {
        this(
            config.host(),
            config.port(),
            config.user(),
            config.password(),
            config.cacheDir(),
            config.readonly(),
            config.rerunLast()
        );
    }

    public PolsoftFtp(String host, int port, String user, String password, String cacheDir, Boolean readonly,
                      Boolean rerunLast) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.cacheDir = cacheDir;
        this.readonly = readonly;
        this.rerunLast = rerunLast;
        this.prevState = new ContentState();
    }

    public void withConnected(Consumer<FTPHelper> action) throws Exception {
        FTPHelper ftp = new FTPHelper(host, port, user, password);
        ftp.connect();
        action.accept(ftp);
        ftp.disconnect();
    }
    public void successCleanup(ContentState state) {
        // Saving state
        state.setTransport(null);
        prevState = state;

        List<String> toDelete = Stream.concat(
                Arrays.stream(REQUIRED_FILES),
                Stream.of(STATE_JSON, ".")).toList();

        for (String path : toDelete) {
            Path filePath = state.getExportCacheDir().resolve(path);
            try {
                Files.delete(filePath);
                LOG.debug("Successfully deleted: {}", filePath);
            } catch (IOException x) {
                LOG.warn("Can't delete {}", filePath, x);
            }
        }
    }

    public void failureCleanup(ContentState state) {
        // Saving state
        state.setTransport(null);
        this.prevState = state;
    }

    public Optional<ContentState> waitForContentPull(String ftpDir) {
        return findAndReadLastState()
            .map((lastState) -> {
                prevState = lastState;
                if(rerunLast) {
                    LOG.info("rerun-last=true, skipping ftp pull, running {}", lastState.getExportDigest());
                    rerunLast = false;
                    return lastState;
                }
                return null; //do not return last state
            })
            .or(() -> pullNewContentFromFtp(prevState, ftpDir))
            .map((newState) -> {
                newState.setTransport(
                        FilesystemTransport.configure(
                                newState.getExportCacheDir().toString(), true, "iso-8859-2"));
                return newState;
            });
    }

    private Optional<ContentState> pullNewContentFromFtp(ContentState prevState, String ftpDir)  {
        FTPHelper ftp = new FTPHelper(host, port, user, password);
        try {
            ftp.connect();
            FTPFile[] rootFTPFiles = ftp.listFiles("/");
            List<String> rootFiles = Arrays.stream(rootFTPFiles)
                    .map(FTPFile::getRawListing)
                    .collect(Collectors.toList());
            String rootDigest = sha1HexDigest(String.join("\n", rootFiles));

            if (rootDigest.equals(prevState.getRootDigest())) {
                LOG.info("No change in root folder ({})", rootDigest);
                return Optional.empty();
            }

            FTPFile[] exportFTPFiles = ftp.listFiles(ftpDir); //TODO dept
            List<String> exportFiles = Arrays.stream(exportFTPFiles)
                    .map(FTPFile::getRawListing)
                    .collect(Collectors.toList());
            String exportDigest = sha1HexDigest(String.join("\n", exportFiles));

            if (exportDigest.equals(prevState.getExportDigest())) {
                LOG.info("No change in export folder ({})", exportDigest);
                return Optional.empty();
            }

            List<String> exportFileNames = Arrays.stream(exportFTPFiles)
                    .map(FTPFile::getName)
                    .toList();

            List<String> missing = Arrays.stream(REQUIRED_FILES)
                    .filter(filename -> !exportFileNames.contains(filename))
                    .toList();

            if (!missing.isEmpty()) {
                LOG.warn("Missing files in export folder: {}", missing);
                return Optional.empty();
            }

            Path dir = Files.createDirectories(
                    Path.of(
                            String.format("%s/%s-%s", cacheDir, ftpDir, getSnapshotTimestamp())));

            List<String> hashes = new ArrayList<>();
            for (String file : REQUIRED_FILES) {
                String src = String.format("%s/%s", ftpDir, file);
                Path dest = dir.resolve(file);
                String hash = ftp.downloadFile(src, dest.toString());
                hashes.add(hash);
                LOG.debug("Download: {} -> {}: {}", src, dest, hash);
            }

            String contentDigest = sha1HexDigest(String.join(",", hashes));

            ContentState state = ContentState.builder()
                    .rootDigest(rootDigest)
                    .exportDigest(exportDigest)
                    .contentDigest(contentDigest)
                    .exportCacheDir(dir)
                    .build();

            Files.writeString(dir.resolve(STATE_JSON), new ObjectMapper().writeValueAsString(state));

            if (contentDigest.equals(prevState.getContentDigest())) {
                LOG.info("Same content as before, cleaning");
                successCleanup(state);
                return Optional.empty();
            }
            return Optional.of(state);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                ftp.disconnect();
            } catch (IOException e) {
                // ignore in finally
            }
        }
    }

    /**
     * Returns last state from cache directory
     * @return last state or empty if no state.json found
     */
    private Optional<ContentState> findAndReadLastState() {
        try (Stream<Path> paths = Files.walk(Paths.get(cacheDir))) {
            List<Path> dirs = paths.filter(Files::isDirectory).sorted().toList();
            if(dirs.isEmpty()) {
                LOG.debug("No directories in {}", cacheDir);
                return Optional.empty();
            }
            Path lastDir = dirs.getLast();
            Path stateJson = lastDir.resolve(STATE_JSON);
            if(!Files.exists(stateJson)) {
                LOG.warn("No state.json in {}", lastDir);
                return Optional.empty();
            }
            LOG.debug("Reading state from {}", stateJson);
            return Optional.of(new ObjectMapper().readValue(stateJson.toFile(), ContentState.class));
        } catch (IOException e) {
            LOG.error("Failed to read last state", e);
            return Optional.empty();
        }
    }

    public void syncDirToRemote(String localDir, String localDirDone, String remoteDir) throws IOException {
        syncDirToRemote(Path.of(localDir), Path.of(localDirDone), remoteDir);
    }

    public void syncDirToRemote(Path localDir, Path localDirDone, String remoteDir) throws IOException {
        ensureDirectoryExists(localDir);
        if(!hasFilesInDirectory(localDir)) {
            LOG.debug("No new files to sync in local:{} -> ftp:{}", localDir, remoteDir);
            return;
        }
        ensureDirectoryExists(localDirDone);
        FTPHelper ftp = new FTPHelper(host, port, user, password);
        try {
            ftp.connect();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(localDir)) {
                for (Path filePath : stream) {
                    if (Files.isRegularFile(filePath)) {
                        try (FileInputStream inputStream = new FileInputStream(filePath.toFile())) {
                            String remoteFile = remoteDir + "/" + filePath.getFileName().toString();
                            if(readonly) {
                                LOG.warn("readonly=true, ignoring upload local:{} to ftp:{}", filePath, remoteFile);
                                continue;
                            }
                            LOG.info("Uploading local:{} to ftp:{}", filePath, remoteFile);
                            boolean done = ftp.uploadFile(inputStream, remoteFile);
                            if (done) {
                                Path donePath = localDirDone.resolve(filePath.getFileName());
                                LOG.debug("Upload done, moving local:{} to local:{}", filePath, donePath);
                                Files.move(filePath, donePath, StandardCopyOption.REPLACE_EXISTING);  // move the file after successful upload
                            } else {
                                LOG.error("Failed to upload local:" + filePath);
                            }
                        }
                    }
                }
            }
        } finally {
            ftp.disconnect();
        }
    }


    private static final DateTimeFormatter SNAPSHOT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private String getSnapshotTimestamp() {
        LocalDateTime dateTime = LocalDateTime.now();
        return dateTime.format(SNAPSHOT_TIME_FORMATTER);
    }

    private boolean hasFilesInDirectory(Path directory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            Iterator<Path> iterator = stream.iterator();
            return iterator.hasNext();
        }
    }

    public void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            if(readonly) {
                LOG.warn("readonly=true, not creating directories {}", directory);
            } else {
                Files.createDirectories(directory);
            }
        }
    }
}
