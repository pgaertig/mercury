package pl.amitec.mercury.integrators.dynamics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowItem(
        String id,
        @JsonProperty("@odata.etag") String etag,
        String number,
        String description,
        String baseUnitOfMeasure,
        BigDecimal unitPrice,
        String vendorNumber,
        ZonedDateTime lastDatetimeModified,
        Long inventory,
        String itemCategoryCode) {
}
