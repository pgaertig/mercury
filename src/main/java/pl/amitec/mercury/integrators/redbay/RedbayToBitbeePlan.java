package pl.amitec.mercury.integrators.redbay;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.clients.bitbee.types.Producer;
import pl.amitec.mercury.clients.bitbee.types.Resource;
import pl.amitec.mercury.clients.bitbee.types.Stock;
import pl.amitec.mercury.clients.bitbee.types.Tax;
import pl.amitec.mercury.clients.bitbee.types.Warehouse;
import pl.amitec.mercury.clients.bitbee.types.*;
import pl.amitec.mercury.engine.MercuryPlanConfigurator;
import pl.amitec.mercury.engine.MercuryPlanRun;
import pl.redbay.ws.client.GizaAPIPortType;
import pl.redbay.ws.client.RedbayCxfClient;
import pl.redbay.ws.client.types.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RedbayToBitbeePlan implements MercuryPlanConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(RedbayToBitbeePlan.class);

    private BitbeeClient bitbeeClient;

    @Override
    public MercuryPlanRun configure(Map<String, String> config) throws MercuryException {

        RedbayCxfClient redbayClient = new RedbayCxfClient(config.get("redbay.url"));
        bitbeeClient = new BitbeeClient(config);

        return () -> {
            while(true) {
                try {
                    var resumeLoop = syncLoop(config, redbayClient, bitbeeClient);
                    if (resumeLoop) {
                        try {
                            Thread.sleep(Duration.ofSeconds(60));
                        } catch (InterruptedException e) {
                            break;
                        }
                    } else {
                        break;
                    }
                } catch (Exception ex) {
                    LOG.error("Exception:", ex);
                    try {
                        Thread.sleep(Duration.ofMinutes(5));
                    } catch (InterruptedException iex) {
                        throw new RuntimeException(ex); //last error
                    }
                }
            }
        };
    }

    private boolean syncLoop(Map<String, String> config, RedbayCxfClient redbayClient, BitbeeClient bitbeeClient) throws Exception {
        String source = config.get("source");
        try {
            GizaAPIPortType rbApi = redbayClient.getAPI();
            Ticket ticket = rbApi.createTicket(
                    config.get("redbay.service_key"),
                    Long.parseLong(config.get("redbay.app_id")),
                    config.get("redbay.auth_pass")
            );

            processOrders(config, bitbeeClient, rbApi, ticket);

            ArrayOfChanges productsChanges = rbApi.getProductsChanges(ticket, LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0));
            List<Change> changes = productsChanges.getItems();
            if (changes.isEmpty()) {
                LOG.debug("No product changes.");
                return true;
            }
            LOG.debug("Found {} changed products.", changes.size());

            processChangedProducts(config, bitbeeClient, rbApi, ticket, changes);
        } catch (InterruptedException iex) {
            LOG.debug("Interrupted");
            return false;
        }
        return true;
    }

    private void processOrders(Map<String, String> config, BitbeeClient bitbeeClient, GizaAPIPortType rbApi, Ticket ticket) throws Exception {
        LOG.info("Processing orders");
        var ordersJournalItems = bitbeeClient.getOrdersJournal();
        if(ordersJournalItems.isEmpty()) {
            LOG.info("No orders to process");
            return;
        } else {
            LOG.info("Found {} orders to process", ordersJournalItems.size());
            List<Tax> taxes = bitbeeClient.getTaxes();
            Map<Long, BigDecimal> taxToValue = bitbeeClient.getTaxes().stream().collect(
                    Collectors.toMap(Tax::id,Tax::percent));
            rbApi.getJournal(ticket, LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0));

            //TODO extract source from plan execution
            String source = config.getOrDefault("bitbee.source", "rb");

            ordersJournalItems.forEach(
                    journalItem -> {
                        processOrder(journalItem, source, taxes);
                        bitbeeClient.confirmJournalItem(journalItem);
                    });
        }
    }

    private void processOrder(JournalItem journalItem, String source, List<Tax> taxes) {
   /*     Optional<Order> order = bitbeeClient.getOrder(journalItem.objectId());
        List<OrderItem> orderItems = order.items();
        List<ImportOrderItem> importOrderItems = orderItems.stream().map(orderItem -> {
            return ImportOrderItem.builder()
                    .source(source)
                    .sourceId(orderItem.sourceId())
                    .quantity(orderItem.quantity())
                    .price(orderItem.price())
                    .tax(taxes.get(orderItem.taxId()))
                    .build();
        }).collect(Collectors.toList());

        ImportOrder importOrder = ImportOrder.builder()
                .source(source)
                .sourceId(order.sourceId())
                .items(importOrderItems)
                .build();

        bitbeeClient.importOrder(importOrder);

    */
    }

    private void processChangedProducts(Map<String, String> config, BitbeeClient bitbeeClient, GizaAPIPortType rbApi, Ticket ticket, List<Change> changes) throws Exception {
        String source = config.getOrDefault("bitbee.source", "rb");
        String lang = config.getOrDefault("bitbee.lang", "pl");

        CategoryMapper categoryMapper = new CategoryMapper(rbApi.takeCategoriesTree(ticket));

        bitbeeClient.session(() -> {
            final Map<String, Warehouse> bbWarehouseMap = new HashMap<>();
            changes.forEach(rbChange -> {
                LOG.debug("Change: " + rbChange);
                Product rbProduct = rbApi.takeProduct(ticket, rbChange.getId(), "");
                Optional<ImportVariant> bbProductVariant = rbProduct.getVariants().getItems().stream().map(variant -> {
                    var bbVariant = ImportVariant.builder()
                        .code(variant.getCode())
                        .productCode(rbProduct.getCode())
                        .source(source)
                        .sourceId(variant.getId().toString())
                        .ean(variant.getEan1())
                        .unit(rbProduct.getUnit().getName())
                        .tax(rbProduct.getTax().getName())
                        //.status(Optional.of(rbProduct.getActive()))
                        .status(Optional.of("Y"))
                        .lang(getLanguage(rbProduct))
                        //.debug(Optional.empty())
                        .producer(makeProducer(rbProduct, source))
                        .name(getName(rbProduct, lang))
                        .categories(List.of(categoryMapper.getBitbeeCategoryPath(rbProduct.getCategories())))
                        .attrs(makeAttributes(variant, lang))
                        .stocks(makeStocks(bitbeeClient, source, variant, bbWarehouseMap))
                        .build();

                    try {
                        Optional<ImportVariant> result = bitbeeClient.importVariant(bbVariant);
                        var variantId = result.get().id();
                        if(bbVariant.categories().isEmpty()) {
                            LOG.debug("Variant no categories: {}", bbVariant);
                        } else {
                            LOG.debug("Variant imported: {}", bbVariant);
                        }
                        return result;
                    } catch (Exception e) {
                        LOG.error("Variant processing error: ", e);
                        return null;
                    }
                }).findFirst().orElse(Optional.empty());

                bbProductVariant.ifPresent(importVariant -> {
                    Stream<Resource> resources = rbProduct.getPhotos().getItems().stream().map(rbPhoto ->
                        bitbeeClient.getResourceBySourceAndSourceId(source, rbPhoto.getId().toString()).orElse(
                                Resource.builder()
                                        .source(source)
                                        .sourceId(rbPhoto.getId().toString())
                                        .url(rbPhoto.getResource().getFilename())
                                        .fileSize(-1L)
                                        .fileName(rbPhoto.getResource().getFilename())
                                        .fileType(rbPhoto.getResource().getFiletype())
                                        .type("P")
                                        .title(rbPhoto.getResource().getTitle())
                                        .build()
                        )
                    );
                    bitbeeClient.assignProductPictures(importVariant.productId(),
                            resources.map(bitbeeClient::createOrUpdateResource).toList());
                });

            });
        });
    }

    private static Producer makeProducer(Product product, String source) {
        return Optional.ofNullable(product.getProducer()).map(producer ->
                Producer.builder()
                        .source(source)
                        .sourceId(producer.getId().toString())
                        .name(producer.getName())
                        .build()
        ).orElse(null);
    }

    public List<Stock> makeStocks(BitbeeClient bbc, String source, Variant variant, final Map<String, Warehouse> bbWarehouseMap) {
        return variant.getStocks().getItems().stream().map((rbStock) -> {
            var rbWarehouse = rbStock.getWarehouse();
            var bbWarehouse = bbWarehouseMap.computeIfAbsent(rbWarehouse.getId().toString(), (sourceId) ->
                bbc.getWarehouseBySourceAndSourceId(source, sourceId).orElseGet(() ->
                        bbc.createWarehouse(Warehouse.builder()
                                .name(rbWarehouse.getName())
                                .source(source)
                                .sourceId(sourceId)
                                .availability(24)
                                .build()
                        ))
            );
            return Stock.builder()
                    .source(source).sourceId(String.format("%s:%s", rbWarehouse.getId(), variant.getId()))
                    .warehouseId(bbWarehouse.id().toString())
                    .quantity(rbStock.getQuantity().toString())
                    .price(rbStock.getPrice().toString())
                    .build();
        }).collect(Collectors.toList());
    }

    private List<VariantAttr> makeAttributes(Variant variant, String lang) {
        return variant.getAttributes().getItems().stream().map((rbAttr) ->
                VariantAttr.builder()
                        .name(rbAttr.getProperty().getName())
                        .value(rbAttr.getValue())
                        .lang(lang)
                        .build()
        ).collect(Collectors.toList());
    }

    private TranslatedName getName(Product product, String defaultLanguage) {
        return product.getTranslations().getItems().stream().filter(t -> "N".equals(t.getType())).findFirst().map(t ->
                TranslatedName.of(t.getLanguage().getIso(), t.getValue())
        ).orElse(TranslatedName.of(defaultLanguage, product.getCode() + " [brak nazwy w Redbay]"));
    }

    private String getLanguage(Product product) {
        return product.getTranslations().getItems().getFirst().getLanguage().getIso();
    }

}
