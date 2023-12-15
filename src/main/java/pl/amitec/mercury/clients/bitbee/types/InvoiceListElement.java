package pl.amitec.mercury.clients.bitbee.types;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceListElement(
        Long id,
        String source,
        @JsonProperty("sourceid") String sourceId,
        String number,
        String company,
        BigDecimal netto,
        BigDecimal brutto,
        String resourceUrl,
        ZonedDateTime added,
        ZonedDateTime payment
) {}
