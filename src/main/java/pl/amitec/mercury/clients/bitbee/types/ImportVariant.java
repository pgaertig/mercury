package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.util.List;
import java.util.Optional;

@JsonInclude(Include.NON_ABSENT)
@JsonPropertyOrder({"code", "product_code", "source", "source_id", "ean", "unit", "tax", "status", "lang",
        "debug", "producer", "name", "categories", "attrs", "stock"})
@Builder
public record ImportVariant(
        String code,
        @JsonProperty("product_code") String productCode,
        String source,
        @JsonProperty("source_id") String sourceId,
        String ean,
        String unit,
        String tax,
        Optional<String> status,
        String lang,
        Optional<String> debug,
        Optional<Producer> producer,
        TranslatedName name,
        List<List<Category>> categories,
        List<VariantAttr> attrs,
        List<Stock> stocks
) {}
