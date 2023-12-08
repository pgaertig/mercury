package pl.amitec.mercury.integrators.dynamics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.http2client.Http2Client;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.jaxrs2.JAXRS2Contract;
import feign.slf4j.Slf4jLogger;

public class DynamicsClient {
    private DynamicsBCAPI dynamicsAPI;
    private ObjectMapper objectMapper;

    public DynamicsClient(String baseUrl, OAuth2Session authSession) {
        OAuth2FeignRequestInterceptor oAuth2FeignRequestInterceptor = new OAuth2FeignRequestInterceptor(authSession);
        objectMapper = JsonMapper.builder()
                .addModule(new Jdk8Module())
                .addModule(new JavaTimeModule())
                .build();

        dynamicsAPI = Feign.builder()
                .contract(new JAXRS2Contract())
                .client(new Http2Client())
                .encoder(new JacksonEncoder(objectMapper))
                .decoder(new JacksonDecoder(objectMapper))
                .errorDecoder(new DynamicsErrorDecoder(authSession))
                .logger(new Slf4jLogger(DynamicsBCAPI.class))
                .requestInterceptor(oAuth2FeignRequestInterceptor)
                .target(DynamicsBCAPI.class, baseUrl);
    }

    public DynamicsBCAPI getDynamicsBCAPI() {
        return dynamicsAPI;
    }
}
