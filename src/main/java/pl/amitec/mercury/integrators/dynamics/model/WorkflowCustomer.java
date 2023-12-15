package pl.amitec.mercury.integrators.dynamics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowCustomer (
    String id,
    @JsonProperty("@odata.etag") String etag,
    String number,
    String name,
    String address,
    String address2,
    String city,
    String contact,
    String phoneNumber,
    ZonedDateTime lastModifiedDateTime,
    String countryRegionCode,
    String postCode,
    @JsonProperty("eMail") String email,
    String contactType,
    boolean pricesIncludingVat//,
    //@JsonAnySetter Map<String, Object> unknownFields
) {
}
