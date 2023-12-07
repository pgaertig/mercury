package pl.amitec.mercury.integrators.dynamics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Stores OAuth2 access token and refreshes it when it expires.
 * Thread-safe.
 */
public class OAuth2Session {

    private final String accessTokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String scope;

    private String accessToken;
    private long accessTokenExpiresAt;

    private final ReentrantLock refreshLock = new ReentrantLock();

    public OAuth2Session(String accessTokenUrl, String clientId, String clientSecret, String scope) {
        this.accessTokenUrl = accessTokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
    }

    public String getAccessToken() {
        if (accessToken == null || accessTokenExpiresAt < System.currentTimeMillis()) {
            refreshLock.lock();
            try {
                if (accessToken == null || accessTokenExpiresAt < System.currentTimeMillis()) {
                    requestOAuth2AccessToken();
                }
            } finally {
                refreshLock.unlock();
            }
        }
        return accessToken;
    }

    private void requestOAuth2AccessToken() {
        var body = Map.of(
                "grant_type", "client_credentials",
                "client_id", clientId,
                "client_secret", clientSecret,
                "scope", scope);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(accessTokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(buildFormDataFromMap(body))
                .build();

        HttpResponse<String> response;

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        String responseBody = response.body();
        if (response.statusCode() != 200) {
            throw new RuntimeException("Error getting OAuth2 access token: " + responseBody);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode map = mapper.readTree(responseBody);
            accessToken = map.get("access_token").textValue();
            accessTokenExpiresAt = System.currentTimeMillis() + map.get("expires_in").longValue() * 1000;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error getting OAuth2 access token: " + responseBody, e);
        }
    }

    private static HttpRequest.BodyPublisher buildFormDataFromMap(Map<String, String> data) {
        String formData = data.entrySet().stream()
                .map(entry -> encodeValue(entry.getKey()) + "=" + encodeValue(entry.getValue()))
                .collect(Collectors.joining("&"));
        return HttpRequest.BodyPublishers.ofString(formData);
    }

    private static String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
