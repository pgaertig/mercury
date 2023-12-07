package pl.amitec.mercury.integrators.dynamics;

import lombok.Data;

@Data
public class AuthConfig {
    private String accessTokenUrl;
    private String clientId;
    private String clientSecret;
    private String scope;
}
