package pl.amitec.mercury;

import lombok.Builder;

import java.util.Map;

@Builder
public record Plan(
    String planSource,
    Boolean enabled,
    String name,
    String integrator,
    Map<String, String> config){

}
