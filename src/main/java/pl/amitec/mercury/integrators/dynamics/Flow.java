package pl.amitec.mercury.integrators.dynamics;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.PlanExecution;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.clients.bitbee.types.ImportClient;
import pl.amitec.mercury.dict.PostCodes;
import pl.amitec.mercury.integrators.dynamics.model.WorkflowCustomer;

import java.time.Duration;

import static pl.amitec.mercury.util.Utils.orderedMapOfStrings;

public class Flow {

    private static final Logger LOG = LoggerFactory.getLogger(Flow.class);
    private final OAuth2Session session;
    private final DynamicsBCAPI dynamicsAPI;
    private final BitbeeClient bitbeeClient;
    private final PlanExecution planExecution;
    private Config config;
    private DynamicsClient client;

    public Flow(PlanExecution execution) {
        planExecution = execution;
        config = execution.loadConfig(Config.class);
        var authConfig = config.getDynamics().getAuth();
        session = new OAuth2Session(
                authConfig.getAccessTokenUrl(),
                authConfig.getClientId(),
                authConfig.getClientSecret(),
                authConfig.getScope()
        );
        client = new DynamicsClient(config.getDynamics().getApiUrl(), session);
        dynamicsAPI = client.getDynamicsBCAPI();
        bitbeeClient = new BitbeeClient(config.getBitbee());
    }

    public void loop() {
        while (true) {
            try {
                LOG.info("Clients sync");
                dynamicsAPI.getWorkflowCustomers().list().forEach(
                        customer -> {
                            LOG.info("Found customer: {}", customer.name());
                            syncClient(customer);
                        });



                Thread.sleep(Duration.ofMinutes(1)); //virtual-thread yield
            } catch (InterruptedException e) {
                LOG.error("Loop - interrupted", e);
                break;
            }
        }
    }

    private void syncClient(WorkflowCustomer customer) {
        ImportClient clientDto = ImportClient.builder()
                .sourceId(customer.id())
                .source(planExecution.getPlan().name())
                .name(customer.name())
                .email(customer.email())
                .phone(customer.phoneNumber())
                .street((customer.address() + " " + customer.address2()).trim())
                .postcode(customer.postCode())
                .city(customer.city())
                .nip(customer.number())
                .country(customer.countryRegionCode())
                .properties(orderedMapOfStrings(
                        "iph_prices_including_vat", Boolean.toString(customer.pricesIncludingVat())
                )).build();
        String json = null;
        try {
            json = BitbeeClient.JSON_MAPPER.writeValueAsString(clientDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        bitbeeClient.importCompany(json);
    }

    public boolean test() {
        boolean pass = true;
        try {
            LOG.debug("Test passed - Dynamics oAuth 2.0, access_token=" + session.getAccessToken());
            try {
                LOG.info("Test passed - Dynamics API, found tenant: {}", dynamicsAPI.getTenant().name());
            } catch (Exception e) {
                LOG.error("Test failed - Dynamics API, failed", e);
                pass = false;
            }
        } catch (Exception e) {
            LOG.error("Test failed - Dynamics oAuth 2.0, failed", e);
            pass = false;
        }
        try {
            LOG.info("Test passed - bitbee client, shop info: {}", bitbeeClient.getShopInfo());
        } catch (Exception e) {
            LOG.error("Test failed - bitbee client, failed", e);
            pass = false;
        }
        return pass;
    }
}
