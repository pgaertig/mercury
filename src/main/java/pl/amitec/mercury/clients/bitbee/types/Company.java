package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Company(
        String source,
        @JsonProperty("sourceid") String sourceId,
        Long id,
        String nip,
        String regon,
        String fullname,
        String street,
        String flat,
        String postcode,
        String city,
        @JsonProperty("country") Long countryId,
        @JsonProperty("province") Long provinceId,
        String phone,
        String email,
        @JsonProperty("type") Long typeId,
        Map<String, String> properties
)
{}
