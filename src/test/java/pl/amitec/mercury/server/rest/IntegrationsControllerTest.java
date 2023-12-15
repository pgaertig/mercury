package pl.amitec.mercury.server.rest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pl.amitec.mercury.server.IntegratorRepository;

import java.util.Arrays;
import java.util.List;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.Mockito.when;

//@SpringBootTest
//@AutoConfigureMockMvc
public class IntegrationsControllerTest {

    @Mock
    private IntegratorRepository integratorDiscovery;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        IntegrationsController controller = new IntegrationsController(integratorDiscovery);
        RestAssuredMockMvc.standaloneSetup(controller);
    }
    @Test
    public void shouldReturnAllIntegrators() throws Exception {
        List<String> mockResponse = Arrays.asList("Integrator1", "Integrator2");
        when(integratorDiscovery.getAllNames()).thenReturn(mockResponse);

        MockMvcResponse response = given()
                .when()
                .get("/integrators");

        response.then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("$", org.hamcrest.Matchers.hasSize(2))
                .body("[0]", org.hamcrest.Matchers.equalTo("Integrator1"))
                .body("[1]", org.hamcrest.Matchers.equalTo("Integrator2"));
    }

}
