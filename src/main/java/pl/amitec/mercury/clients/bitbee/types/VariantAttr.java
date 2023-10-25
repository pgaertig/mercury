package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

@JsonPropertyOrder({"name", "value", "lang"})
@Builder
public record VariantAttr(String name, String value, String lang) {}
