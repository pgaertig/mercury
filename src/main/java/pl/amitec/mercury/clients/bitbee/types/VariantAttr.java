package pl.amitec.mercury.clients.bitbee.types;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

@JsonPropertyOrder({"code", "name", "value", "lang"})
@Builder
public record VariantAttr(String code, String name, String value, String lang) {}
