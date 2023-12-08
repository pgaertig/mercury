package pl.amitec.mercury.clients.bitbee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.clients.bitbee.impl.BitbeeFormatsModule;
import pl.amitec.mercury.clients.bitbee.types.*;
import pl.amitec.mercury.clients.bitbee.types.AuthTokenResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static pl.amitec.mercury.util.StringUtils.truncate;

public class BitbeeClient {

    public static final ObjectMapper JSON_MAPPER = JsonMapper.builder()
            .configure(MapperFeature.ALLOW_COERCION_OF_SCALARS, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .addModule(new Jdk8Module())
            .addModule(new BitbeeFormatsModule())
            .build();

    private static final Logger LOG = LoggerFactory.getLogger(BitbeeClient.class);

    private final String uri;
    private final String host;
    private final String apikey;
    private final String email;
    private final String pass;
    private final boolean dryRun;
    private final HttpClient client;
    private final String auth1;
    private final String authId;
    private final String authPass;
    private String token;
    private String userPublicKey;

    public BitbeeClient(Map<String, String> config) {
        this(config.get("bitbee.url"),
                config.get("bitbee.apikey"),
                config.get("bitbee.auth_id"),
                config.get("bitbee.auth_pass"),
                config.get("bitbee.email"),
                config.get("bitbee.pass"),
                Boolean.parseBoolean(config.getOrDefault("bitbee.readonly", "false")));
        if (dryRun) {
            LOG.warn("Dry-run client");
        }
    }

    public BitbeeClient(BitbeeConfig config) {
        this(config.getUrl(),
                config.getApiKey(),
                config.getAuthId(),
                config.getAuthPass(),
                config.getEmail(),
                config.getPass(),
                config.isReadonly());
        if (dryRun) {
            LOG.warn("Dry-run client");
        }
    }

    public BitbeeClient(String uri, String apikey, String authId, String authPass, String email, String pass, boolean dryRun) {
        this.uri = uri;
        this.host = URI.create(uri).getHost();
        this.apikey = apikey;
        this.email = email;
        this.pass = pass;
        this.dryRun = dryRun;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.authId = authId;
        this.authPass = authPass;
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
        getToken();
        login();
        block.run();
    }

    protected void getToken() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "/authorization/token"))
                .headers("Accept", "application/json",
                        "Api-Key", apikey,
                        "Authorization", "Basic " + auth1)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        AuthTokenResponse tokenResponse = JSON_MAPPER.readValue(response.body(), AuthTokenResponse.class);

        if (response.statusCode() == 200) {
            token = tokenResponse.token();
        } else {
            throw new BitbeeClientException(STR. "Authorization failed (apikey=\{ apikey }, auth_id=\{ authId }, auth_pass=\{ authPass }): \{ response.body() }" );
        }
    }

    protected void login() throws IOException, InterruptedException {
        var userLogin = UserLogin.builder().email(email).password(pass).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "/users/login"))
                .headers("Accept", "application/json",
                        "Api-Key", apikey,
                        "Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(JSON_MAPPER.writeValueAsString(userLogin)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        UserLoginResponse loginResponse = JSON_MAPPER.readValue(response.body(), UserLoginResponse.class);

        if (response.statusCode() == 200) {
            token = response.headers().firstValue("refreshed-token").get();
            userPublicKey = loginResponse.publicKey();
        } else {
            throw new BitbeeClientException(STR. "Authorization failed (apikey=\{ apikey }, auth_id=\{ authId }, auth_pass=\{ authPass }): \{ response.body() }" );
        }
    }

    public boolean test() {
        return (token != null && !token.isEmpty());
    }

    public void importVariant(String json) {
        postJson("import/variant", json);
    }

    public Optional<ImportVariant> importVariant(ImportVariant importVariant) {
        return post("import/variant", importVariant);
    }

    // Companies

    public void importCompany(String json) {
        postJson("import/company", json);
    }

    public List<Company> getCompanies(Map<String, Object> params) {
        return getList("companies", new TypeReference<>() {}, params);
    }

    public Optional<Company> getCompanyBySourceId(String source, String sourceId) {
        return first(getCompanies(Map.of("source", source, "source_id", sourceId)));
    }

    // Invoices

    public void addInvoice(Invoice invoice) {
        post("invoice", invoice);
    }

    public void updateInvoice(Invoice invoice) {
        put("invoice/" + invoice.id(), invoice);
    }

    public Optional<InvoiceListElement> getInvoiceBySourceId(String source, String sourceId) {
        return first(getInvoices(Map.of("source", source, "source_id", sourceId)));
    }

    public Optional<Invoice> getInvoiceById(String id) {
        return get("invoice/" + id, Invoice.class, Map.of());
    }

    public List<InvoiceListElement> getInvoices(Map<String, Object> params) {
        return getList("invoices", new TypeReference<>(){}, params);
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
        if (includeConfirmed) {
            params.put("include_confirmed", true);
        }
        if (limit != null) {
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

    public List<Tax> getTaxes() {
        return getList("taxes", new TypeReference<>() {});
    }

    public List<Warehouse> getWarehouses() {
        return getList("warehouses", new TypeReference<>() {});
    }

    public Optional<Warehouse> getWarehouseBySourceAndSourceId(String source, String sourceId) {
        List<Warehouse> warehouses = getList("warehouses",
                new TypeReference<>() {},
                Map.of("source", source, "source_id", sourceId));

        if (warehouses.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(warehouses.getFirst());
        }
    }

    public Warehouse createWarehouse(Warehouse warehouse) {
        return post("warehouse", warehouse).orElseThrow();
    }


    // Other

    public JsonNode getShopInfo() {
        return getJson("shop/info");
    }

    protected <E> Optional<E> first(List<E> list) {
        if (list.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(list.getFirst());
        }
    }

    protected <T> List<T> getList(String entity, TypeReference<ListContainer<T>> type, Map<String, Object> params) {
        try {
            return JSON_MAPPER.readValue(
                    getJsonString(entity, params),
                    type).getList();
        } catch (JsonProcessingException e) {
            throw new BitbeeClientException(e);
        }
    }

    protected <T> List<T> getList(String entity, TypeReference<ListContainer<T>> type) {
        return getList(entity, type, null);
    }

    protected <T> Optional<T> post(String entity, T inObject) {
        try {
            String payload = JSON_MAPPER.writeValueAsString(inObject);
            JsonNode root = JSON_MAPPER.readTree(postJson(entity, payload));
            JsonNode object = root.get("object");
            if (object == null || object.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(JSON_MAPPER.readerFor(inObject.getClass()).readValue(object));
        } catch (IOException e) {
            throw new BitbeeClientException(e);
        }
    }

    protected <T> Optional<T> put(String entity, T inObject) {
        try {
            String payload = JSON_MAPPER.writeValueAsString(inObject);
            JsonNode root = JSON_MAPPER.readTree(putJson(entity, payload));
            JsonNode object = root.get("object");
            if (object == null || object.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(JSON_MAPPER.readerFor(inObject.getClass()).readValue(object));
        } catch (IOException e) {
            throw new BitbeeClientException(e);
        }
    }


    protected <T> Optional<T> get(String entity, Class<T> returnClass, Map<String, Object> params) {
        try {
            JsonNode root = getJson(entity, params);
            JsonNode object = root.get("object");
            if (object.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(JSON_MAPPER.readerFor(returnClass).readValue(object));
        } catch (IOException e) {
            throw new BitbeeClientException(e);
        }
    }

    protected String postJson(String apiCall, String json) {
        String url = uri + "/" + apiCall;
        if (dryRun) {
            LOG.info("Dry-run, ignore POST to {} with body {}", url, truncate(json, 120, true));
            return null;
        }

        LOG.debug("To send: " + json);

        HttpRequest request = authorizedRequestBuilder(url)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOG.debug("+Bitbee: POST " + url + " success " + response.statusCode() + ": " + response.body());
                return response.body();
            } else {
                throw new RuntimeException("!Bitbee: POST " + url + " failure, req: " + json + "\n resp: " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    protected String putJson(String apiCall, String json) {
        String url = uri + "/" + apiCall;
        if (dryRun) {
            LOG.info("Dry-run, ignore POST to {} with body {}", url, truncate(json, 40, true));
            return null;
        }
        HttpRequest request = authorizedRequestBuilder(url)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 300) {
                System.out.println("+Bitbee: POST " + url + " success " + response.statusCode() + ": " + response.body());
                return response.body();
            } else {
                throw new RuntimeException("!Bitbee: POST " + url + " failure " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    protected JsonNode getJson(String apiCall) {
        return getJson(apiCall, Map.of());
    }

    protected JsonNode getJson(String apiCall, Map<String, Object> params) {
        try {
            return JSON_MAPPER.readTree(getJsonString(apiCall, params));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getJsonString(String apiCall, Map<String, Object> params) {
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

        HttpRequest request = authorizedRequestBuilder(url).GET().build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() == 200) {
            LOG.debug("+Bitbee: GET " + url + " success " + response.statusCode() + ": " + response.body());
            return response.body();
        } else {
            LOG.debug("+Bitbee: GET " + url + " success " + response.statusCode() + ": " + response.body());
            throw new RuntimeException("!Bitbee: GET " + url + " failure " + response.statusCode() + ": " + response.body());
        }
    }

    protected HttpRequest.Builder authorizedRequestBuilder(String url) {
        if(token == null || userPublicKey == null) {
            try {
                session(()->{});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .headers("Accept", "application/json",
                        "Api-Key", apikey,
                        "Authorization", "Bearer " + token,
                        "Content-Type", "application/json",
                        "User-Public-Key", userPublicKey);
    }

}