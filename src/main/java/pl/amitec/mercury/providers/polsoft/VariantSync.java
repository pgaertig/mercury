package pl.amitec.mercury.providers.polsoft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.JobContext;
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

        try(var stocksReader = deptDir.reader("stany.txt");
            var producersReader = deptDir.reader("produc.txt");
            var groupsReader = deptDir.reader("grupy.txt");
            var productReader = deptDir.reader("towary.txt")
        ) {
            var stocks = csvHelper.mapCSV(stocksReader, "towar_numer", "towar_ilosc");
            var producers = csvHelper.mapCSV(producersReader, "prd_numer", "prd_nazwa");
            var groups = csvHelper.mapCSV(groupsReader, "categories_id", "categories_name");

            Optional<String> warehouseId = jobContext.redbayClient().getWarehouseId("polsoft", dept);
            warehouseId.orElseThrow(); //TODO create warehouses

            var variantSourceIds = new HashSet<String>();

            csvHelper.streamCSV(productReader).forEach(product -> {
                syncProduct(jobContext, dept, product, selectedSourceIds, producers, groups, warehouseId.get(), stocks)
                        .ifPresent(variantSourceIds::add);
            });

            return variantSourceIds;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

        var root = jsonObject(
                "code", code,
                "product_code", code,
                "source", "polsoft", //TODO multi tenant
                "source_id", sourceId,
                "ean", product.get("towar_ean_sztuka"),
                "unit", product.get("tw_jm"),
                "tax", String.format("%s%%", product.get("towar_vat")),
                //"status", "Y", //Active, or D - Deleted, or N - inactive
                "lang", "pl"
                //"debug", timestamp
        );

        Optional.ofNullable(product.get("towar_producent")).ifPresent(producerId ->
                Optional.ofNullable(producers.get(producerId)).ifPresent(producer ->
                        root.set("producer", jsonObject(
                                        "source_id", producerId,
                                        "name", producer,
                                        "source", "polsoft"
                                )
                        )
                )
        );

        root.set("name", jsonObject("pl", product.get("towar_nazwa")));

        root.set("categories", jsonArray(jsonArray(jsonObject()
                        .put("source_id", product.get("nr_grupy"))
                        .set("name", jsonObject("pl", groups.get(product.get("nr_grupy")))))));

        root.set("attrs", jsonArrayWith(attrs -> {
            attrs.add(jsonObject(
                    "name", "GRATIS",
                    "value", product.get("towar_gratis"),
                    "lang", "pl"
            ));
            attrs.add(jsonObject(
                    "name", "ZBIORCZE",
                    "value", product.get("towar_ilosc_opak_zb"),
                    "lang", "pl"
            ));
            Optional.ofNullable(product.get("substancja_czynna")).ifPresent(value ->
                    attrs.add(jsonObject(
                            "name", "SUBSTANCJA CZYNNA",
                            "value", value,
                            "lang", "pl"
                    ))
            );
        }));

        root.set("stocks", jsonArray(jsonObject(
                            "source_id", String.format("%s:%s", dept, sourceId),
                            "source", "polsoft", //TODO multi-tenant
                            "warehouse_id", warehouseId,
                            "quantity", stocks.getOrDefault(sourceId, "0"),
                            "price", product.get("towar_cena1")
        )));


        try {
            // TODO stats
            var jsonMapper = JsonMapper.builder().configure(MapperFeature.ALLOW_COERCION_OF_SCALARS, false).build();
            String json = jsonMapper.writeValueAsString(root);
            jobContext.hashCache().hit(
                    jobContext.getTenant(), "ps", "p", String.format("%s:%s", dept, sourceId), json, (data) -> {
                        LOG.debug("JSON: {}", json);
                        jobContext.redbayClient().importVariant(json);
                    });
            jobContext.syncStats().entries();

            return Optional.of(sourceId);
        } catch (JsonProcessingException e) {
            jobContext.syncStats().failed();
            throw new RuntimeException(e); //TODO
        }

    }


}