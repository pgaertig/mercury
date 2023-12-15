package pl.amitec.mercury.integrators.dynamics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListValue<T>(
    @JsonProperty("value") List<T> list
) {}
