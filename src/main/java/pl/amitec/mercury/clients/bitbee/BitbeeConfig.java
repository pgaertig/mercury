package pl.amitec.mercury.clients.bitbee;

import lombok.Data;

@Data
public class BitbeeConfig {
    private String source;
    private String url;
    private String apiKey;
    private String authId;
    private String authPass;
    private String email;
    private String pass;
    private boolean readonly;
}
