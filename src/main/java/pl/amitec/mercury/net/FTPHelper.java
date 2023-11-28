package pl.amitec.mercury.net;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.util.Utils;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

public class FTPHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FTPHelper.class);

    private final FTPClient ftp;
    private String host;
    private int port;
    private String user;
    private String password;

    public FTPHelper(String host, int port, String user, String password) {
        ftp = new FTPClient();
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        /*var writer = IoBuilder.forLogger("ftp-wire")
                .setLevel(Level.DEBUG)
                .setMarker(MarkerManager.getMarker("FTP-WIRE"))
                .buildPrintWriter();
        ftp.addProtocolCommandListener(new PrintCommandListener(writer, true, (char) 0, true));*/
    }

    public void connect() throws IOException {
        LOG.debug("Connecting to %s@%s:%d".formatted(user, host, port));
        ftp.connect(host, port);
        ftp.login(user, password);
        ftp.enterLocalPassiveMode();
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
    }
    public void disconnect() throws IOException {
        if(ftp.isConnected()) {
            ftp.logout();
            ftp.disconnect();
        }
    }

    public FTPFile[] listFiles(String path) throws IOException {
        return ftp.listFiles(path);
    }

    public String rawFileListing(List<FTPFile> files) {
        return files.stream().map(ftpFile -> ftpFile.getRawListing()).collect(Collectors.joining("\n"));
    }

    /**
     * Downloads remote file to local path
     * @param remotePath
     * @param localPath
     * @return
     * @throws IOException
     */
    public String downloadFile(String remotePath, String localPath) throws IOException {
        DigestInputStream input;
        OutputStream output;
        try {
            input = new DigestInputStream(ftp.retrieveFileStream(remotePath), MessageDigest.getInstance("SHA-1"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        output  = new FileOutputStream(localPath);
        if(!FTPReply.isPositivePreliminary(ftp.getReplyCode())) {
            input.close();
            output.close();
            ftp.logout();
            ftp.disconnect();
            throw new IOException("File transfer initiation failed %s".formatted(remotePath));
        }
        Util.copyStream(input, output, 4096);
        input.close();
        output.close();
        // Must call completePendingCommand() to finish command.
        if(!ftp.completePendingCommand()) {
            ftp.logout();
            ftp.disconnect();
            throw new IOException(String.format("File transfer finalization failed %s", remotePath));
        }
        return Utils.bytesToHex(input.getMessageDigest().digest());
    }

    public void uploadFile(String localPath, String remotePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(localPath)) {
            ftp.storeFile(remotePath, fis);
        }
    }

    public boolean uploadFile(InputStream dataStream, String remotePath) throws IOException {
        return ftp.storeFile(remotePath, dataStream);
    }
}
