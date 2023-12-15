package pl.amitec.mercury.integrators.dynamics;

import pl.amitec.mercury.integrators.dynamics.model.*;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

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
    ListValue<SalesOrder> postSalesOrders(SalesOrder salesOrders);
}
