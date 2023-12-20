package pl.amitec.mercury.integrators.dynamics;

import pl.amitec.mercury.integrators.dynamics.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

public interface DynamicsBCAPI {
    @GET
    Tenant getTenant();

    @GET
    @Path("/WorkflowCustomers")
    ListValue<WorkflowCustomer> getWorkflowCustomers();

    @GET
    @Path("/WorkflowItems")
    ListValue<WorkflowItem> getWorkflowItems();

    @GET
    @Path("/SalesInvoices")
    ListValue<SalesInvoice> getSalesInvoices();

    @GET
    @Path("/SalesOrderImportWS")
    ListValue<SalesOrder> getSalesOrders();

    @POST
    @Path("/SalesOrderImportWS")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    ListValue<SalesOrder> postSalesOrders(SalesOrder salesOrders);
}