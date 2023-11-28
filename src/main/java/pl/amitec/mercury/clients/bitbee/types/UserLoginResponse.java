package pl.amitec.mercury.clients.bitbee.types;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserLoginResponse(
    //User user,
    String publicKey
) {
}
