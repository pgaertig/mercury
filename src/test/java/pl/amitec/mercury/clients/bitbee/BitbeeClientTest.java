package pl.amitec.mercury.clients.bitbee;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.amitec.mercury.TestUtil;
import pl.amitec.mercury.clients.bitbee.types.JournalItem;
import pl.amitec.mercury.clients.bitbee.types.Order;
import pl.amitec.mercury.clients.bitbee.types.Warehouse;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static pl.amitec.mercury.TestUtil.readFile;

@ExtendWith(MockitoExtension.class)
public class BitbeeClientTest {
    private static final int PORT = 8085;
    private BitbeeClient cli;

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void setupWireMockServer() {
        wireMockServer = new WireMockServer(WireMockConfiguration
                .wireMockConfig().port(PORT)
                .notifier(new ConsoleNotifier(true)));
        wireMockServer.start();
    }

    @BeforeEach
    public void setup() {
        cli = new BitbeeClient(
                Map.of("bitbee.url", "http://localhost:8085/api",
                       "bitbee.apikey", "testkey",
                       "bitbee.auth_id", "testid",
                       "bitbee.auth_pass",  "testpass",
                       "bitbee.readonly", "false"));
        wireMockServer.stubFor(get(urlPathEqualTo("/api/authorization/token")
                ).withHeader("Accept", containing("application/json"))
                .willReturn(aResponse().withBody(readFile("bitbee/authorizationToken.json"))));
        wireMockServer.stubFor(post(urlPathEqualTo("/api/users/login")
        ).withHeader("Accept", containing("application/json"))
                .willReturn(aResponse()
                        .withHeader("refreshed-token", "testtoken")
                        .withBody(readFile("bitbee/usersLogin.json"))));
    }

    @Test
    public void test_getTaxes() {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/taxes"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse().withBody("""
                        {"list":[
                        {"id":5,"shop":2,"name":"0%","percent":0},
                        {"id":2,"shop":2,"name":"23%","percent":23},
                        {"id":7,"shop":2,"name":"5%","percent":5},
                        {"id":8,"shop":2,"name":"8%","percent":8},
                        {"id":1,"shop":2,"name":"n/d","percent":0},
                        {"id":6,"shop":2,"name":"zw","percent":0}]
                        ,"validate":true,"redirect":"","itemsCount":6,"result":null,"errors":[],"encoding":"UTF-8",
                        "compress":true,"exception":null,"messages":[],"code":200,
                        "timestamp":{"date":"2023-10-25 10:15:00.838835","timezone_type":3,"timezone":"Europe/Warsaw"}}
                        """
                )));
        var taxes = cli.getTaxes();
        assertEquals(6, taxes.size());
        var tax23 = taxes.get(1);
        assertEquals(BigDecimal.valueOf(23), tax23.percent());
        assertEquals(2, tax23.id());
    }

    @Test
    public void test_getWarehouses() {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/warehouses"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse().withBody(
                        """
                                {"list":[{"id":3,"name":"Magazyn 1","availability":24,
                                "address":"","postcode":"","city":"","phone":"","email":"",
                                "country":null,"source":"ps","sourceid":"1"}],
                                "validate":true,"redirect":"","itemsCount":1,"result":null,
                                "errors":[],"encoding":"UTF-8","compress":true,
                                "exception":null,"messages":[],"code":200,
                                "timestamp":{"date":"2023-10-24 12:41:31.578871","timezone_type":3,"timezone":"Europe/Warsaw"}}
                                """
                )));
        var warehouses = cli.getWarehouses();
        var warehouse = warehouses.getFirst();
        assertEquals(3, warehouse.id());
        assertEquals("Magazyn 1", warehouse.name());
        assertEquals("ps", warehouse.source());
        assertEquals("1", warehouse.sourceId());
    }

    @Test
    public void test_createWarehouse() {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/warehouse"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson("""
                                {"name":"Main warehouse","availability": 24, "source":"test","sourceid":"15"}"""))
                .willReturn(aResponse().withBody("""
                         {"object":
                           {"id":58847,"name":"Main warehouse","availability":0,"address":"",
                           "postcode":"","city":"","phone":"","email":"","country":null,
                           "source":"test","sourceid":"15"},
                          "validate":true,"redirect":"","itemsCount":0,"result":null,"errors":[],
                          "encoding":"UTF-8","compress":true,"exception":null,"messages":[],
                          "code":200,"timestamp":{"date":"2023-10-25 09:29:42.816023",
                          "timezone_type":3,"timezone":"Europe/Warsaw"}}"""
                )));
        Warehouse result = cli.createWarehouse(Warehouse.builder()
                .name("Main warehouse")
                .source("test")
                .sourceId("15")
                .availability(24)
                .build());
        assertNotNull(result);
        assertEquals(58847, result.id());
        assertEquals("Main warehouse", result.name());
    }

    @Test
    public void test_getOrderJournalItems() throws URISyntaxException, IOException {
        String journalItemsJson = new String(Files.readAllBytes(Paths.get(getClass().getResource("/bitbee/journalItems.json").toURI())));

        wireMockServer.stubFor(get(urlPathEqualTo("/api/journal"))
                        //.withQueryParam("limit", equalTo("100"))
                        .withQueryParam("type", equalTo("order"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse().withBody(journalItemsJson)));
        List<JournalItem> orders = cli.getOrdersJournal();
        assertEquals(3, orders.size());
    }

    @Test
    public void test_getOrder() {
        String journalItemsJson = TestUtil.readFile("bitbee/order.json");

        wireMockServer.stubFor(get(urlPathEqualTo("/api/order/400"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse().withBody(journalItemsJson)));
        Order order = cli.getOrder("400").get();
        assertEquals(400, order.id());
        assertEquals(3, order.positions().size());
    }

}
