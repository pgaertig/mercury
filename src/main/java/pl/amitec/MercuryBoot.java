package pl.amitec;

import org.apache.commons.net.ftp.FTPClient;

public class MercuryBoot {
    public MercuryBoot() {
        var ftpClient = new FTPClient();
    }

    public void init() {
        System.out.println("! Mercury Boot 2");
    }

}