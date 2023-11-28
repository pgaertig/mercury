package pl.amitec.mercury.clients.bitbee.types;


import lombok.Builder;

@Builder
public record UserLogin(
        String email,
        String password) {
}
