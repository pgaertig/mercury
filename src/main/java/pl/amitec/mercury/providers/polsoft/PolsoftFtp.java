package pl.amitec.mercury.providers.polsoft;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.amitec.mercury.net.FTPHelper;
import pl.amitec.mercury.transport.FilesystemTransport;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static pl.amitec.mercury.util.Utils.sha1HexDigest;

public class PolsoftFtp {

    private static final Logger LOG = LogManager.getLogger(PolsoftFtp.class);

    private static final String[] REQUIRED_FILES = {
            "grupy.txt", "klienci.txt", "produc.txt", "rabaty.txt",
            "rozrach.txt", "stany.txt", "toppc.txt", "towary.txt", "zestawy.txt", "zestpoz.txt"
    };

    private String lastRootDigest = "none";
    private String lastExportDigest = "none";
    private String lastContentDigest = "none";
    private String host;
    private int port;
    private String user;
    private String password;
    private final String cacheDir;

    public static PolsoftFtp configure(Map<String, String> properties) {
        return new PolsoftFtp(
            properties.getOrDefault("ftp.host", "localhost"),
            Integer.parseInt(properties.getOrDefault("ftp.port", "21")),
            properties.getOrDefault("ftp.user", "anonymous"),
            properties.getOrDefault("ftp.password", "anonymous"),
            properties.getOrDefault("ftp.cacheDir", "data/ftp")
        );
    }

    public PolsoftFtp(String host, int port, String user, String password, String cacheDir) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.cacheDir = cacheDir;
    }

    public void withConnected(Consumer<FTPHelper> action) throws Exception {
        FTPHelper ftp = new FTPHelper(host, port, user, password);
        ftp.connect();
        action.accept(ftp);
        ftp.disconnect();
    }
    public void successCleanup(State state) {
        // Saving state
        this.lastContentDigest = state.contentDigest;
        this.lastRootDigest = state.rootDigest;
        this.lastExportDigest = state.exportDigest;
        // Cleaning
        for (String filename : REQUIRED_FILES) {
            Path filePath = state.exportCacheDir.resolve(filename);
            try {
                Files.delete(filePath);
                // logger.debug("Successfully deleted: " + filePath.toString());
            } catch (NoSuchFileException x) {
                System.err.format("%s: no such file or directory%n", filePath);
            } catch (DirectoryNotEmptyException x) {
                System.err.format("%s not empty%n", filePath);
            } catch (IOException x) {
                // File permission problems are caught here.
                System.err.println(x);
            }
        }

        // Deleting the directory
        try {
            Files.delete(state.exportCacheDir);
            // logger.debug("Successfully deleted directory: " + state.exportCacheDir.toString());
        } catch (NoSuchFileException x) {
            System.err.format("%s: no such file or directory%n", state.exportCacheDir);
        } catch (DirectoryNotEmptyException x) {
            System.err.format("%s not empty%n", state.exportCacheDir);
        } catch (IOException x) {
            // File permission problems are caught here.
            System.err.println(x);
        }

    }

    public void failureCleanup(State state) {
        // Saving state
        this.lastContentDigest = state.contentDigest;
        this.lastRootDigest = state.rootDigest;
        this.lastExportDigest = state.exportDigest;
    }

    // A private class to represent the state
    private static class State {
        public String rootDigest;
        public String exportDigest;
        public String contentDigest;
        public Path exportCacheDir;
        public FilesystemTransport transport;
    }

    public State waitForContentPull(String ftpDir) throws Exception {
        FTPHelper ftp = new FTPHelper(host, port, user, password);
        try {
            ftp.connect();
            FTPFile[] rootFTPFiles = ftp.listFiles("/");
            List<String> rootFiles = Arrays.stream(rootFTPFiles)
                    .map(FTPFile::getRawListing)
                    .collect(Collectors.toList());
            String rootDigest = sha1HexDigest(String.join("\n", rootFiles));

            if (rootDigest.equals(lastRootDigest)) {
                LOG.info("No change in root folder ({})", rootDigest);
                return null;
            }

            FTPFile[] exportFTPFiles = ftp.listFiles(ftpDir); //TODO dept
            List<String> exportFiles = Arrays.stream(exportFTPFiles)
                    .map(FTPFile::getRawListing)
                    .collect(Collectors.toList());
            String exportDigest = sha1HexDigest(String.join("\n", exportFiles));

            if (exportDigest.equals(lastExportDigest)) {
                LOG.info("No change in export folder ({})", exportDigest);
                return null;
            }

            List<String> exportFileNames = Arrays.stream(exportFTPFiles)
                    .map(FTPFile::getName)
                    .toList();

            List<String> missing = Arrays.stream(REQUIRED_FILES)
                    .filter(filename -> !exportFileNames.contains(filename))
                    .toList();

            if (!missing.isEmpty()) {
                LOG.warn("Missing files in export folder: {}", missing);
                return null;
            }

            Path dir = Files.createDirectories(
                    Path.of(
                            String.format("%s/%s-%s", cacheDir, ftpDir, getSnapshotTimestamp())));

            List<String> hashes = new ArrayList<>();
            for (String file : REQUIRED_FILES) {
                String src = String.format("%s/%s", ftpDir, file);
                Path dest = dir.resolve(file);
                String hash = ftp.downloadFile(src, dest.toString());  // The method returns hash now
                hashes.add(hash);
                LOG.debug("{} -> {}: {}", src, dest, hash);
            }

            String contentDigest = sha1HexDigest(String.join(",", hashes));

            State state = new State();
            state.rootDigest = rootDigest;
            state.exportDigest = exportDigest;
            state.contentDigest = contentDigest;
            state.exportCacheDir = dir;

            if (contentDigest.equals(lastContentDigest)) {
                LOG.info("Same content as before, cleaning");
                successCleanup(state);
                return null;
            }

            state.transport =
                    FilesystemTransport.configure(dir.toString(), true, "iso-8859-2");

            return state;
        } finally {
            ftp.disconnect();
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
            Files.createDirectories(directory);
        }
    }
}
