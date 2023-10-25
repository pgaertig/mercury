package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.Objects;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Warehouse(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("availability") Integer availability,
        @JsonProperty("source") String source,
        @JsonProperty("sourceid") String sourceId,
        @JsonProperty("address") String address,
        @JsonProperty("postcode") String postcode,
        @JsonProperty("city") String city,
        @JsonProperty("email") String email,
        @JsonProperty("country") String countryId
) {
    public Warehouse {
        // TODO switch to Validators https://www.baeldung.com/java-object-validation-deserialization
        Objects.requireNonNull(name, "Warehouse `name` cannot be null");
        Objects.requireNonNull(source, "Warehouse `source` cannot be null");
        Objects.requireNonNull(sourceId, "Warehouse `sourceId` cannot be null");
    }
}