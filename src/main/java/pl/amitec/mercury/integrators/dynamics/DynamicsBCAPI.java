package pl.amitec.mercury.integrators.dynamics;

import feign.Headers;
import pl.amitec.mercury.integrators.dynamics.model.Tenant;
import pl.amitec.mercury.integrators.dynamics.model.WorkflowCustomersResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public interface DynamicsBCAPI {
    @GET
    Tenant getTenant();

    @GET
    @Path("/WorkflowCustomers")
    WorkflowCustomersResponse getWorkflowCustomers();
}
