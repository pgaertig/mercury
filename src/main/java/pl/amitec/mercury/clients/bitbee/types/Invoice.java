package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"source", "source_id", "id", "number", "generated", "payment", "paid", "deposit", "netto", "brutto", "company"})
@Builder
public record Invoice(
        String source,
        @JsonProperty("sourceid") String sourceId,
        Long id,
        String number,
        LocalDate generated,
        LocalDate payment,
        LocalDate paid,
        BigDecimal deposit,
        BigDecimal netto,
        BigDecimal brutto,

        //Company needs to be nested in Invoice like "{company: {id: 123}}" for creation and update
        @JsonProperty("company") Company company

) {
}
