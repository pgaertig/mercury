package pl.amitec.mercury.integrators.polsoft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.clients.bitbee.types.*;
import pl.amitec.mercury.formats.CSVHelper;
import pl.amitec.mercury.transport.Transport;

import java.io.IOException;
import java.util.*;

import static pl.amitec.mercury.util.Utils.*;

public class VariantSync {
    private static final Logger LOG = LoggerFactory.getLogger(VariantSync.class);

    public VariantSync() {
    }

    public Set<String> sync(JobContext jobContext,
                                Transport deptDir, String dept, Set<String> selectedSourceIds) {
        var csvHelper = new CSVHelper();
        String source = "polsoft";

        try(var stocksReader = deptDir.reader("stany.txt");
            var producersReader = deptDir.reader("produc.txt");
            var groupsReader = deptDir.reader("grupy.txt");
            var productReader = deptDir.reader("towary.txt")
        ) {
            var stocks = csvHelper.mapCSV(stocksReader, "towar_numer", "towar_ilosc");
            var producers = csvHelper.mapCSV(producersReader, "prd_numer", "prd_nazwa");
            var groups = csvHelper.mapCSV(groupsReader, "categories_id", "categories_name");

            Warehouse warehouse = getOrCreateWarehouse(jobContext, dept, source);

            var variantSourceIds = new HashSet<String>();

            csvHelper.streamCSV(productReader).forEach(product -> {
                syncProduct(jobContext, dept, product, selectedSourceIds,
                        producers, groups, warehouse.id().toString(), stocks)
                        .ifPresent(variantSourceIds::add);
            });

            return variantSourceIds;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Warehouse getOrCreateWarehouse(JobContext jobContext, String dept, String source) {
        Warehouse warehouse = jobContext.bitbeeClient()
                .getWarehouseBySourceAndSourceId(source, dept)
                .orElseGet(() ->
                    jobContext.bitbeeClient().createWarehouse(
                            Warehouse.builder()
                                    .name(STR."Magazyn \{ dept }")
                                    .source(source)
                                    .sourceId(dept)
                                    .availability(24)
                                    .build()
                    )
                );
        return warehouse;
    }

    private static Optional<String> syncProduct(
            JobContext jobContext,
            String dept, Map<String, String> product, Set<String> selectedSourceIds,
            Map<String, String> producers,
            Map<String, String> groups,
            String warehouseId, Map<String, String> stocks) {
        var sourceId = product.get("towar_numer");
        if(selectedSourceIds != null && !selectedSourceIds.contains(product.get("towar_numer"))) {
            return Optional.empty();
        }
        //stats.add(FAILED);
        var code = product.get("towar_kod");
        if(code == null || code.isEmpty()) {
            //stats.add(FAILED);
            LOG.warn("Product with no code {}", sourceId); //TODO row/lineno
            return Optional.empty();
        }

        var variant = ImportVariant.builder()
                .code(code)
                .productCode(code)
                .source("polsoft") //TODO tenant
                .sourceId(sourceId)
                .ean(product.get("towar_ean_sztuka"))
                .unit(product.get("tw_jm"))
                .tax(String.format("%s%%", product.get("towar_vat")))
                .status(Optional.empty())
                .lang("pl")
                .debug(Optional.empty())
                .producer(
                        Optional.ofNullable(product.get("towar_producent")).flatMap(producerId ->
                                Optional.ofNullable(producers.get(producerId)).map(producer ->
                                        new Producer(producerId, producer, "polsoft")
                                )
                        ))
                .name(TranslatedName.of("pl", product.get("towar_nazwa")))
                .categories(List.of(List.of(Category.builder()
                                .sourceId(product.get("nr_grupy"))
                                .name(TranslatedName.of("pl", groups.get(product.get("nr_grupy"))))
                                .build()
                        )
                ))
                .attrs(compactListOf(
                        new VariantAttr("GRATIS", product.get("towar_gratis"), "pl"),
                        new VariantAttr("ZBIORCZE", product.get("towar_ilosc_opak_zb"), "pl"),
                        Optional.ofNullable(product.get("substancja_czynna")).map(value ->
                                new VariantAttr("SUBSTANCJA CZYNNA", value, "pl")
                        ).orElse(null)
                ))
                .stocks(List.of(Stock.builder()
                                .sourceId(String.format("%s:%s", dept, sourceId))
                                .source("polsoft")
                                .warehouseId(warehouseId)
                                .quantity(stocks.getOrDefault(sourceId, "0"))
                                .price(product.get("towar_cena1")).build()))
                .build();


        try {
            // TODO stats
            String json = BitbeeClient.JSON_MAPPER.writeValueAsString(variant);
            jobContext.hashCache().hit(
                    jobContext.getTenant(), "ps", "p", String.format("%s:%s", dept, sourceId), json, (data) -> {
                        LOG.debug("JSON: {}", json);
                        jobContext.bitbeeClient().importVariant(json);
                    });
            jobContext.syncStats().incEntries();

            return Optional.of(sourceId);
        } catch (Exception e) {
            jobContext.syncStats().incFailed();
            LOG.error(String.format("Failed processing Variant: %s", code), e);
            return Optional.empty();
        }
    }


}