package pl.amitec.mercury.clients.bitbee;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.amitec.mercury.clients.bitbee.types.Warehouse;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertEquals(23, tax23.percent());
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



}
