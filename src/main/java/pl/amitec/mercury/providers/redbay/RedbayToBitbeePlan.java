package pl.amitec.mercury.providers.redbay;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.clients.bitbee.types.Producer;
import pl.amitec.mercury.clients.bitbee.types.Warehouse;
import pl.amitec.mercury.engine.MercuryPlan;
import pl.amitec.mercury.engine.MercuryPlanConfigurator;
import pl.amitec.mercury.engine.MercuryPlanRun;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.clients.bitbee.types.Category;
import pl.amitec.mercury.clients.bitbee.types.ImportVariant;
import pl.amitec.mercury.clients.bitbee.types.Stock;
import pl.amitec.mercury.clients.bitbee.types.TranslatedName;
import pl.amitec.mercury.clients.bitbee.types.VariantAttr;
import pl.redbay.ws.client.GizaAPIPortType;
import pl.redbay.ws.client.RedbayClient;
import pl.redbay.ws.client.RedbayCxfClient;
import pl.redbay.ws.client.types.*;

import javax.xml.datatype.DatatypeFactory;
import java.lang.Object;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@MercuryPlan(name="bitbee-redbay")
public class RedbayToBitbeePlan implements MercuryPlanConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(RedbayToBitbeePlan.class);

    @Override
    public MercuryPlanRun configure(Map<String, String> config) throws MercuryException {

        RedbayCxfClient redbayClient = new RedbayCxfClient(config.get("redbay.url"));
        BitbeeClient bitbeeClient = new BitbeeClient(config);

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

    private void processChangedProducts(Map<String, String> config, BitbeeClient bitbeeClient, GizaAPIPortType rbApi, Ticket ticket, List<Change> changes) throws Exception {
        String source = config.getOrDefault("bitbee.source", "rb");
        String lang = config.getOrDefault("bitbee.lang", "pl");

        bitbeeClient.session(() -> {
            final Map<String, Warehouse> bbWarehouseMap = new HashMap<>();
            changes.forEach(rbChange -> {
                LOG.debug("Change: " + rbChange);
                Product rbProduct = rbApi.takeProduct(ticket, rbChange.getId(), "");
                rbProduct.getVariants().forEach(variant -> {
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
                        .debug(Optional.empty())
                        .producer(makeProducer(rbProduct, source))
                        .name(getName(rbProduct, lang))
                        .categories(makeCategories(rbProduct))
                        .attrs(makeAttributes(variant, lang))
                        .stocks(makeStocks(bitbeeClient, source, variant, bbWarehouseMap))
                        .build();

                    try {
                        var result = bitbeeClient.importVariant(bbVariant);
                        result.get();
                    } catch (Exception e) {
                        LOG.error("Variant processing error: ", e);
                        return;
                    }
                });
            });
        });
    }

    private static Optional<Producer> makeProducer(Product product, String source) {
        return Optional.ofNullable(product.getProducer()).map(producer ->
                Producer.builder()
                        .source(source)
                        .sourceId(producer.getId().toString())
                        .name(producer.getName())
                        .build()
        );
    }

    public List<Stock> makeStocks(BitbeeClient bbc, String source, Variant variant, final Map<String, Warehouse> bbWarehouseMap) {
        return variant.getStocks().stream().map((rbStock) -> {
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
        return variant.getAttributes().stream().map((rbAttr) ->
                VariantAttr.builder()
                        .name(rbAttr.getProperty().getName())
                        .value(rbAttr.getValue())
                        .lang(lang)
                        .build()
        ).collect(Collectors.toList());
    }

    private List<List<Category>> makeCategories(Product product) {
        //product.getCategories().getFirst().
        //FIXME
        /*
        .getCategories().getFirst().
                List.of(List.of(pl.amitec.mercury.clients.bitbee.types.Category.builder()
                        .sourceId(product.get("nr_grupy"))
                        .name(TranslatedName.of("pl", groups.get(product.get("nr_grupy"))))
                        .build()
                )*/
        return List.of(List.of());
    }

    private TranslatedName getName(Product product, String defaultLanguage) {
        return product.getTranslations().stream().filter(t -> "N".equals(t.getType())).findFirst().map(t ->
                TranslatedName.of(t.getLanguage().getIso(), t.getValue())
        ).orElse(TranslatedName.of(defaultLanguage, product.getCode() + " [brak nazwy w Redbay]"));
    }

    private String getLanguage(Product product) {
        return product.getTranslations().getFirst().getLanguage().getIso();
    }

}
