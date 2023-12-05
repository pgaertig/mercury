package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthTokenResponse(
        String token,
        boolean validate,
        String redirect,
        @JsonProperty("itemsCount") int itemsCount,
        Object result,  // Could be made more specific if the structure of "result" is known
        List<String> errors,
        String encoding,
        Object exception,  // Could be made more specific if the structure of "exception" is known
        List<String> messages,
        int code,
        Timestamp timestamp
) {
    public record Timestamp (
            String date,
            @JsonProperty("timezone_type") int timezoneType,
            String timezone
    ) {}
}
