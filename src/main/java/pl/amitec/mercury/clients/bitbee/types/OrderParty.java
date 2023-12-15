package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderParty(
        Long id,
        String forname,
        String surname,
        String phone,
        String email,
        String street,
        String flat,
        String postcode,
        String city,
        Long country,
        Long province,
        Company company
) {
}
