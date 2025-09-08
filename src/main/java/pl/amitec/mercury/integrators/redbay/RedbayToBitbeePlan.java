package pl.amitec.mercury.integrators.redbay;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.clients.bitbee.types.*;
import pl.amitec.mercury.engine.MercuryPlanConfigurator;
import pl.amitec.mercury.engine.MercuryPlanRun;
import pl.redbay.ws.client.GizaAPIPortType;
import pl.redbay.ws.client.RedbayCxfClient;
import pl.redbay.ws.client.types.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.amitec.mercury.parallel.RetryConsumer.retry;

public class RedbayToBitbeePlan implements MercuryPlanConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(RedbayToBitbeePlan.class);

    private BitbeeClient bitbeeClient;

    @Override
    public MercuryPlanRun configure(Map<String, String> config) throws MercuryException {

        RedbayCxfClient redbayClient = new RedbayCxfClient(config.get("redbay.url"));
        bitbeeClient = new BitbeeClient(config);

        return () -> {
            LocalDateTime lastSync = LocalDateTime.of(2020, 2, 24, 14, 37, 0, 0);
            while(true) {
                try {
                    lastSync = syncLoop(config, redbayClient, bitbeeClient, lastSync);
                    if (lastSync != null) {
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

    private LocalDateTime syncLoop(Map<String, String> config, RedbayCxfClient redbayClient, BitbeeClient bitbeeClient, LocalDateTime lastSync) throws Exception {
        String source = config.get("source");
        try {
            GizaAPIPortType rbApi = redbayClient.getAPI();
            RbTicket ticket = rbApi.createTicket(
                    config.get("redbay.service_key"),
                    Long.parseLong(config.get("redbay.app_id")),
                    config.get("redbay.auth_pass")
            );

            //processOrders(config, bitbeeClient, rbApi, ticket);

            RbArrayOfChanges productsChanges = rbApi.getProductsChanges(ticket, lastSync);
            List<RbChange> changes = productsChanges.getItems();
            if (changes.isEmpty()) {
                LOG.debug("No product changes.");
                return lastSync;
            }

            lastSync = LocalDateTime.now(ZoneId.of("Europe/Warsaw"));  //redbay is Europe/Warsaw

            LOG.debug("Found {} changed products.", changes.size());
            processChangedProducts(config, bitbeeClient, rbApi, ticket, changes);
        } catch (InterruptedException iex) {
            LOG.debug("Interrupted");
            return null;
        }
        return lastSync;
    }

    private void processOrders(Map<String, String> config, BitbeeClient bitbeeClient, GizaAPIPortType rbApi, RbTicket ticket) throws Exception {
        // TODO RB orders status to BB
        //rbApi.getJournal(ticket, LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0));

        LOG.info("BB orders to RB");
        var ordersJournalItems = bitbeeClient.getOrdersJournal();
        if(ordersJournalItems.isEmpty()) {
            LOG.debug("No orders to process");
            return;
        } else {
            LOG.info("Found {} orders to process", ordersJournalItems.size());

            //TODO cache taxes
            Map<Long, BigDecimal> taxToPercent = bitbeeClient.getTaxes().stream().collect(Collectors.toMap(Tax::id,Tax::percent));

            //TODO extract source from plan execution
            String source = config.getOrDefault("bitbee.source", "rb");

            Function<RbOrder, String> createOrder = (order) -> rbApi.createOrder(ticket, order);

            ordersJournalItems.forEach(
                    journalItem -> {
                        processOrder(createOrder, journalItem, source, taxToPercent);
                        bitbeeClient.confirmJournalItem(journalItem);
                    });
        }
    }

    private void processOrder(Function<RbOrder, String> createOrder,
                              JournalItem journalItem, String source, Map<Long, BigDecimal> taxToPercent) {
        bitbeeClient.getOrder(journalItem.objectId()).ifPresentOrElse(
                (order) -> createOrder.apply(new OrderMapper(taxToPercent).getRbOrder(order)),
                () -> LOG.error("Order not found but journal mentioned it: {}",
                        journalItem.objectId())
        );
    }



    private void processChangedProducts(Map<String, String> config, BitbeeClient bitbeeClient, GizaAPIPortType rbApi,
                                        RbTicket ticket, List<RbChange> changes) throws Exception {
        String source = config.getOrDefault("bitbee.source", "rb");
        String lang = config.getOrDefault("bitbee.lang", "pl");

        CategoryMapper categoryMapper = new CategoryMapper(rbApi.takeCategoriesTree(ticket));

        var activeChanges = changes.stream().filter(rbChange -> "Y".equals(rbChange.getActive())).toList();

        if(activeChanges.isEmpty()) {
            LOG.debug("No active changes.");
            return;
        } else {
            LOG.debug("Found {} active changes.", activeChanges.size());
        }

        Map<String, RbSpecialPrice> rbSpecialPriceMap = rbApi.getSpecialPrices(ticket, "b2b-online").getItems().stream()
                .collect(Collectors.toMap(RbSpecialPrice::getStockid, Function.identity()));

        bitbeeClient.session(() -> {
            final Map<String, Warehouse> bbWarehouseMap = new HashMap<>();
            AtomicInteger currentIndex = new AtomicInteger(0);
            activeChanges.forEach(retry(3,60, rbChange -> {
                if(!(rbChange.getId() > 584399869 && rbChange.getId() < 584399900)) {
                    return;
                }
                LOG.debug("Change {}/{}: {}", currentIndex.incrementAndGet(), activeChanges.size(), rbChange);
                RbProduct rbProduct = rbApi.takeProduct(ticket, rbChange.getId(), "");
                var baseBbVariant = ImportVariant.builder()
                        .productCode(rbProduct.getCode())
                        .source(source)
                        .unit(rbProduct.getUnit().getName())
                        .tax(rbProduct.getTax().getName())
                        //.status(Optional.of(rbProduct.getActive()))
                        .status(Optional.of("Y"))
                        //.debug(Optional.empty())
                        .lang(getLanguage(rbProduct))
                        .producer(makeProducer(rbProduct, source))
                        .name(getName(rbProduct, lang))
                        .description(getDescription(rbProduct, lang))
                        .categories(List.of(categoryMapper.getBitbeeCategoryPath(rbProduct.getCategories())))
                        .build();

                List<ImportVariant> bbProductVariants = rbProduct.getVariants().getItems().stream().map(variant -> baseBbVariant.toBuilder()
                        .code(variant.getCode())
                        .sourceId(variant.getId().toString())
                        .ean(variant.getEan1())
                        .attrs(makeAttributes(variant, lang))
                        .stocks(makeStocks(bitbeeClient, source, variant, bbWarehouseMap, rbSpecialPriceMap))
                        .build()).toList();

                if(bbProductVariants.isEmpty()) {
                    bbProductVariants = rbApi.takeProductVariants(ticket,
                            rbProduct.getId()).getItems().stream()
                            .filter(variant -> !variant.getCode().startsWith("DELETE"))
                            .map(rbSimpleVariant -> {
                                return baseBbVariant.toBuilder()
                                        .code(rbSimpleVariant.getCode())
                                        .sourceId(rbSimpleVariant.getId())
                                        .ean(rbSimpleVariant.getEan())
                                        .stocks(makeSimpleStocks(bitbeeClient, source, rbSimpleVariant))
                                        .build();
                            }).toList();
                }

                if(bbProductVariants.isEmpty()) {
                    LOG.warn("No variants for product: code={}, id={}", rbProduct.getCode(), rbProduct.getId());
                } else {
                    Optional<ImportVariant> bbProductVariant = bbProductVariants.stream().map(bbVariant -> {
                        try {
                            Optional<ImportVariant> result = bitbeeClient.importVariant(bbVariant);
                            if(bbVariant.categories() == null || bbVariant.categories().isEmpty()) {
                                LOG.debug("Variant no categories: {}", bbVariant);
                            } else {
                                LOG.debug("Variant imported: {}", bbVariant);
                            }
                            return result;
                        } catch (Exception e) {
                            LOG.error("Variant processing error: ", e);
                            //TODO append to retry queue
                            return Optional.<ImportVariant>empty();
                        }
                    }).findFirst().orElse(Optional.empty());

                    bbProductVariant.ifPresentOrElse(importVariant -> {
                            Stream<Resource> resources = rbProduct.getPhotos().getItems().stream().map(rbPhoto ->
                                    bitbeeClient.getResourceBySourceAndSourceId(source, rbPhoto.getId().toString()).orElse(
                                            Resource.builder()
                                                    .source(source)
                                                    .sourceId(rbPhoto.getId().toString())
                                                    .url(rbPhoto.getResource().getFilename())
                                                    .fileSize(-1L)
                                                    .fileName(rbPhoto.getResource().getFilename())
                                                    .fileType(getFiletype(rbPhoto))
                                                    .type("P")
                                                    .title(rbPhoto.getResource().getTitle())
                                                    .build()
                                    )
                            );
                            bitbeeClient.assignProductPictures(importVariant.productId(),
                                    resources.map(bitbeeClient::createOrUpdateResource).toList());
                        }, () -> LOG.error("Variants not imported: code={}", rbProduct.getCode()));
                }
            }));
        });
    }

    private static String getFiletype(RbProductPhoto rbPhoto) {
        String rbFileType = rbPhoto.getResource().getFiletype();
        if (rbFileType == null || rbFileType.isBlank()) {
            // discover by mime type by with SDK
            if(rbPhoto.getResource().getFilename().endsWith(".webp")) {
                return "image/webp";
            }
        }
        return rbFileType;
    }


    private static Producer makeProducer(RbProduct product, String source) {
        return Optional.ofNullable(product.getProducer()).map(producer ->
                Producer.builder()
                        .source(source)
                        .sourceId(producer.getId().toString())
                        .name(producer.getName())
                        .build()
        ).orElse(null);
    }

    public List<Stock> makeStocks(BitbeeClient bbc, String source, RbVariant variant,
                                  final Map<String, Warehouse> bbWarehouseMap,
                                  Map<String, RbSpecialPrice> rbSpecialPriceMap) {
        return variant.getStocks().getItems().stream().map((rbStock) -> {
            var rbWarehouse = rbStock.getWarehouse();

            RbSpecialPrice specialPrice = rbSpecialPriceMap.get(rbStock.getId().toString());

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

            var stockBuilder = Stock.builder()
                    .source(source).sourceId(String.format("%s:%s", rbWarehouse.getId(), variant.getId()))
                    .warehouseId(bbWarehouse.id().toString())
                    .retailPrice(rbStock.getPrice().toString());

            if (specialPrice == null) {
                LOG.debug("Special price not found for variant: {}", variant.getId());
                return stockBuilder.quantity("0").price(rbStock.getPrice().toString()).build();
            } else {
                return stockBuilder.quantity(rbStock.getQuantity().toString()).price(specialPrice.getPrice()).build();
            }
        }).collect(Collectors.toList());
    }

    public List<Stock> makeSimpleStocks(BitbeeClient bbc, String source, RbSimpleVariant variant) {
        var warehouse = bbc.getWarehouses().getFirst();
        //LOG.error("No default warehouse");
        if(variant.getQuantity() == null) {
            LOG.warn("No quantity for variant: {}", variant.getCode());
            return List.of();
        }
        if(variant.getPrice() == null ) {
            LOG.warn("No price for variant: {}", variant.getCode());
            return List.of();
        }
        return List.of(Stock.builder()
                .source(source).sourceId(String.format("%s:%s", warehouse.id(), variant.getId()))
                .warehouseId(warehouse.id().toString())
                .retailPrice(variant.getPrice().toString())
                .quantity(variant.getQuantity().toString())
                .price(variant.getPrice().toString())
                .build());
    }

    private List<VariantAttr> makeAttributes(RbVariant variant, String lang) {
        return variant.getAttributes().getItems().stream().map((rbAttr) ->
                VariantAttr.builder()
                        .name(rbAttr.getProperty().getName())
                        .value(rbAttr.getValue())
                        .lang(lang)
                        .build()
        ).collect(Collectors.toList());
    }

    private TranslatedName getName(RbProduct product, String defaultLanguage) {
        return product.getTranslations().getItems().stream().filter(t -> "N".equals(t.getType())).findFirst().map(t ->
                TranslatedName.of(t.getLanguage().getIso(), t.getValue())
        ).orElse(TranslatedName.of(defaultLanguage, String.format("%s [brak nazwy w Redbay]", product.getCode())));
    }

    private TranslatedName getDescription(RbProduct product, String defaultLanguage) {
        return product.getTranslations().getItems().stream().filter(t -> "D".equals(t.getType())).findFirst().map(t ->
                TranslatedName.of(t.getLanguage().getIso(), t.getValue())
        ).orElse(TranslatedName.of(defaultLanguage, String.format("%s [brak opisu w Redbay]", product.getCode())));
    }

    private String getLanguage(RbProduct product) {
        return product.getTranslations().getItems().getFirst().getLanguage().getIso();
    }

}
