package pl.amitec.mercury.integrators.dynamics;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.PlanExecution;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.clients.bitbee.types.*;
import pl.amitec.mercury.integrators.dynamics.model.SalesInvoice;
import pl.amitec.mercury.integrators.dynamics.model.SalesOrder;
import pl.amitec.mercury.integrators.dynamics.model.WorkflowCustomer;
import pl.amitec.mercury.integrators.dynamics.model.WorkflowItem;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                String source = planExecution.getPlan().name();

                LOG.info("Clients sync");
                dynamicsAPI.getWorkflowCustomers().list().forEach(
                        customer -> {
                            LOG.info("Dynamics customer: {}", customer.name());
                            syncClient(customer);
                        });

                LOG.info("Sync variants");
                String lang = "pl";
                Warehouse warehouse = bitbeeClient.getOrCreateWarehouse(Warehouse.builder()
                        .name("Magazyn")
                        .source(source)
                        .sourceId("1")
                        .availability(24)
                        .build());

                dynamicsAPI.getWorkflowItems().list().forEach(
                        item -> {
                            LOG.info("Dynamics variant: {}", item.description());
                            //TODO cache
                            syncVariant(item, source, lang, warehouse);
                        });

                Map<String, Company> companyCache = new ConcurrentHashMap<>();
                Function<String, Company> companyLookup = (String sourceId) -> companyCache.computeIfAbsent(sourceId, (id) -> {
                    var company = bitbeeClient.getCompanyBySourceId(source, id);
                    if (company.isEmpty()) {
                        throw new MercuryException("Company not found source=" + id);
                    }
                    return company.get();
                });

                dynamicsAPI.getSalesInvoices().list().forEach(
                        invoice -> {
                            LOG.info("Dynamics invoice: {}", invoice.no());
                            //TODO cache
                            syncInvoice(invoice, source, companyLookup);
                        });

                syncOrders();
                Thread.sleep(Duration.ofMinutes(1)); //virtual-thread yield
            } catch (InterruptedException e) {
                LOG.error("Loop - interrupted", e);
                break;
            }
        }
    }

    private void syncInvoice(SalesInvoice invoice, String source, Function<String, Company> companyLookup) {
        Optional<InvoiceListElement> existing = bitbeeClient.getInvoiceBySourceId(source, invoice.no());
        var bbInvoice = Invoice.builder()
                .id(existing.map(InvoiceListElement::id).orElse(null))
                .source(source)
                .sourceId(invoice.no())
                .number(invoice.no())
                .generated(invoice.postingDate())
                .netto(invoice.amount())
                .brutto(invoice.amountIncludingVat())
                .company(companyLookup.apply(invoice.sellToCustomerNo()))
                .build();
        if(existing.isEmpty()) {
            bitbeeClient.addInvoice(bbInvoice);
        } else {
            bitbeeClient.updateInvoice(bbInvoice);
        }
    }

    private void syncVariant(WorkflowItem item, String source, String lang, Warehouse warehouse) {
        ImportVariant variant = ImportVariant.builder()
                .code(item.number())
                .productCode(item.number())
                .name(TranslatedName.of(lang, item.description()))
                .source(source)
                .sourceId(item.id())
                //.ean()
                .unit(item.baseUnitOfMeasure())
                //.tax()
                .status(Optional.empty())
                .lang(lang)
                //.debug(Optional.empty())
                .categories(
                        List.of(
                                List.of(
                                        Category.builder()
                                                .sourceId(item.itemCategoryCode())
                                                .name(TranslatedName.of(lang, item.itemCategoryCode()))
                                                .build()
                                )
                        )
                )
                .producer(Producer.builder()
                        .sourceId(item.vendorNumber())
                        .name(item.vendorNumber())
                        .source(source)
                        .build())
                .stocks(List.of(
                        Stock.builder()
                                .source(source)
                                .sourceId(String.format("%s:%s", warehouse.id(), item.id()))
                                .warehouseId(warehouse.id().toString())
                                .quantity(item.inventory().toString())
                                .price(item.unitPrice().toString())
                                .build()
                ))
                .build();

        bitbeeClient.importVariant(variant);
    }

    private void syncClient(WorkflowCustomer customer) {
        ImportClient clientDto = ImportClient.builder()
                .sourceId(customer.number())
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

    private void syncOrders() {
        LOG.info("Sync orders");
        String source = planExecution.getPlan().name();
        var ordersJournalItems = bitbeeClient.getOrdersJournal();
        if(ordersJournalItems.isEmpty()) {
            LOG.info("No orders to sync");
            return;
        } else {
            LOG.info("Found {} orders to sync", ordersJournalItems.size());
            Map<Long, BigDecimal> taxes = bitbeeClient.getTaxes().stream().collect(
                    Collectors.toMap(Tax::id,Tax::percent));
            ordersJournalItems.forEach(
                    journalItem -> {
                            syncOrder(journalItem, source, taxes);
                            bitbeeClient.confirmJournalItem(journalItem);
                    });
        }

    }

    private void syncOrder(JournalItem journalItem, String source, Map<Long, BigDecimal> taxPercents) {
        LOG.info("Sync order {}", journalItem.objectId());
        Order bbOrder = bitbeeClient.getOrder(journalItem.objectId())
                .orElseThrow(() -> new RuntimeException(
                        String.format("Orphan Journal item has no Order: %s", journalItem.objectId())));
        AtomicLong lineNo = new AtomicLong(1);
        bbOrder.positions().stream().forEach(orderPosition -> {
                    var salesOrder = SalesOrder.builder()
                            .entryNo(orderPosition.id().toString())
                            .orderNo(bbOrder.uniqueNumber().replaceAll("^RB", "BB"))
                            .customerNo(bbOrder.contact().company().sourceId())
                            .customerName(bbOrder.contact().company().fullname())
                            .externalDocumentNo("")
                            .documentDate(bbOrder.added().toLocalDate())
                            .contact(String.format("%s %s", bbOrder.contact().forname(), bbOrder.contact().surname()))
                            .lineNo(lineNo.getAndIncrement())
                            .lineType("ITEM")
                            .no(orderPosition.code())
                            .description(orderPosition.variantName())
                            .quantity(orderPosition.quantity().longValue())
                            .unitOfMeasure("PSC") //TODO fix when BB API returns the unit
                            //.unitOfMeasure(orderPosition.unit()) //FIXME
                            .unitPrice(orderPosition.price())
                            //.vatPercent(taxPercents.get(orderPosition.tax()))
                            .lineAmount(orderPosition.price().multiply(orderPosition.quantity()))
                            .build();
                    //TODO: this should be done in one request for the whole order
                    dynamicsAPI.postSalesOrders(salesOrder);
                }
        );

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
