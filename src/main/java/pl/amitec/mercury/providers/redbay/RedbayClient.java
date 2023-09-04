package pl.amitec.mercury.providers.redbay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static pl.amitec.mercury.util.StringUtils.truncate;

public class RedbayClient {

    private static final Logger LOG = LoggerFactory.getLogger(RedbayClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String uri;
    private final String host;
    private final String apikey;
    private final boolean dryRun;
    private final HttpClient client;
    private final String auth1;
    private String token;

    public RedbayClient(Map<String, String> config) {
        this(config.get("redbay.url"), config.get("redbay.apikey"),
                config.get("redbay.auth_id"), config.get("redbay.auth_pass"),
                Boolean.parseBoolean(config.getOrDefault("redbay.dry_run", "false")));
        if(dryRun) {
            LOG.warn("Dry-run client");
        }
    }

    public RedbayClient(String uri, String apikey, String authId, String authPass, boolean dryRun) {
        this.uri = uri;
        this.host = URI.create(uri).getHost();
        this.apikey = apikey;
        this.dryRun = dryRun;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.auth1 = Base64.getEncoder().encodeToString((authId + ":" + authPass).getBytes());
    }

    public Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Api-Key", apikey);
        headers.put("Authorization", "Bearer " + token);
        return headers;
    }

    public void session(Runnable block) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "/authorization/token"))
                .headers("Accept", "application/json",
                        "Api-Key", apikey,
                        "Authorization", "Basic " + auth1)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        RbAuthTokenResponse tokenResponse = OBJECT_MAPPER.readValue(response.body(), RbAuthTokenResponse.class);

        if (response.statusCode() == 200) {
            token = tokenResponse.token();
        }
        block.run();
    }

    public boolean test() {
        // Implement the 'login' method as necessary, currently it is missing from the provided code
        // login();
        return (token != null && !token.isEmpty());
    }

    public void importVariant(String json) {
        postJson("import/variant", json);
    }

    // Companies

    public void importCompany(String json) {
        postJson("import/company", json);
    }

    public JsonNode getCompanies(String source, String sourceId) {
        return getJson("companies", Map.of("source", source, "source_id", sourceId));
    }

    public JsonNode getCompanyBySourceId(String source, String sourceId) {
        var result = getCompanies(source, sourceId);
        return result.path("list").get(0);
    }

    // Invoices

    public void addInvoice(String json) {
        postJson("invoice", json);
    }

    public void editInvoice(String id, String json) {
        putJson("invoice/" + id, json);
    }

    public JsonNode getInvoiceByNumber(String number) {
        var result = getJson("invoices", Map.of("number", number));
        return result.path("list").get(0);
    }

    // Orders

    public JsonNode getOrder(String id) {
        return getJson("order/" + id, new HashMap<>()).get("object");
    }

    public void confirmOrder(String id) {
        postJson("order/" + id + "/confirm", "");
    }

    // Journal

    public JsonNode getJournal(String type, boolean includeConfirmed, Integer limit) {
        var params = new HashMap<String, Object>();
        params.put("type", type);
        if(includeConfirmed) {
            params.put("include_confirmed", true);
        }
        if(limit != null) {
            params.put("limit", limit);
        }
        return getJson("journal", params);
    }

    public JsonNode getOrdersJournal() {
        return getJournal("order", false, 100);
    }

    public void confirmJournalItem(String id) {
        putJson("journal/" + id + "/confirm", "");
    }

    public JsonNode getWarehouse(String source, String sourceId) {
        var result = getJson("warehouses", Map.of("source", source, "source_id", sourceId));
        return result.path("list").get(0);
    }

    public Optional<String> getWarehouseId(String source, String sourceId) {
        return Optional.ofNullable(getWarehouse(source, sourceId)).map(
                (json) -> json.get("id").asText());
    }

    public void addWarehouse(String json) {
        postJson("warehouse", json);
    }

    // Other

    public JsonNode getTaxes() {
        return getJson("taxes");
    }

    public JsonNode getShopInfo() {
        return getJson("shop/info");
    }

    protected void postJson(String apiCall, String json) {
        String url = uri + "/" + apiCall;
        if(dryRun) {
            LOG.info("Dry-run, ignore POST to {} with body {}", url, truncate(json, 120, true));
            return;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers("Accept", "application/json",
                        "Api-Key", apikey,
                        "Authorization", "Bearer " + token,
                        "Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOG.debug("+Redbay: POST " + url + " success " + response.statusCode() + ": " + response.body());
            } else {
                throw new RuntimeException("!Redbay: POST " + url + " failure " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    protected void putJson(String apiCall, String json) {
        String url = uri + "/" + apiCall;
        if(dryRun) {
            LOG.info("Dry-run, ignore POST to {} with body {}", url, truncate(json, 40, true));
            return;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers("Accept", "application/json",
                        "Api-Key", apikey,
                        "Authorization", "Bearer " + token,
                        "Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 300) {
                 System.out.println("+Redbay: POST " + url + " success " + response.statusCode() + ": " + response.body());
            } else {
                throw new RuntimeException("!Redbay: POST " + url + " failure " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


    protected JsonNode getJson(String apiCall) {
        return getJson(apiCall, Map.of());
    }
    protected JsonNode getJson(String apiCall, Map<String, Object> params) {
        String paramString = "";
        if (params != null && !params.isEmpty()) {
            StringBuilder paramBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (paramBuilder.length() > 0) {
                    paramBuilder.append('&');
                }
                paramBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                paramBuilder.append('=');
                paramBuilder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
            }
            paramString = "?" + paramBuilder.toString();
        }

        String url = uri + "/" + apiCall + paramString;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers("Accept", "application/json",
                        "Api-Key", apikey,
                        "Authorization", "Bearer " + token,
                        "Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() == 200) {
            LOG.debug("+Redbay: GET " + url + " success " + response.statusCode() + ": " + response.body());
            try {
                return OBJECT_MAPPER.readTree(response.body());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("!Redbay: GET " + url + " failure " + response.statusCode() + ": " + response.body());
        }
    }

}