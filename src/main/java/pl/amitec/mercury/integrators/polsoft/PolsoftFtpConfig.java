package pl.amitec.mercury.integrators.polsoft;

import lombok.Data;

@Data
public class PolsoftFtpConfig {
    private String host;
    private int port;
    private String user;
    private String password;
    private String cacheDir;
    private boolean readonly;
    private boolean rerunLast;
}
