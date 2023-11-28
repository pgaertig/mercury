package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

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
