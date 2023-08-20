package pl.amitec.mercury.providers.polsoft;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.formats.CSVHelper;
import pl.amitec.mercury.transport.Transport;

import java.io.IOException;
import java.util.*;

import static pl.amitec.mercury.util.Utils.*;

public class VariantSync {
    private static final Logger LOG = LogManager.getLogger(VariantSync.class);

    public VariantSync() {
    }

    public void sync(JobContext jobContext,
                     Transport deptDir, String dept, List<String> selectedSourceIds) {
        var csvHelper = new CSVHelper();

        Map<String, String> stocks = null;
        try(var stocksReader = deptDir.reader("stany.txt")) {
            stocks = csvHelper.mapCSV(stocksReader, "towar_numer", "towar_ilosc");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, String> producers = null;
        try(var producersReader = deptDir.reader("produc.txt")) {
            producers = csvHelper.mapCSV(producersReader, "prd_numer", "prd_nazwa");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, String> groups = null;
        try(var producersReader = deptDir.reader("grupy.txt")) {
            groups = csvHelper.mapCSV(producersReader, "categories_id", "categories_name");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JsonNode warehouse = jobContext.getRedbayClient().getWarehouse("polsoft", dept); //TODO create warehouses
        String warehouseId = warehouse.get("id").asText();

        var variantSourceIds = new HashSet<String>();
        try(var productReader = deptDir.reader("towary.txt")) {
            Map<String, String> finalProducers = producers;
            Map<String, String> finalGroups = groups;
            Map<String, String> finalStocks = stocks;
            csvHelper.streamCSV(productReader).forEach((product) -> {
                //stats.add(FAILED);
                var code = product.get("towar_kod");
                var sourceId = product.get("towar_numer");
                if(code == null || code.isEmpty()) {
                    //stats.add(FAILED);
                    LOG.warn("Product with no code {}", sourceId); //TODO row/lineno
                    return;
                }
                variantSourceIds.add(sourceId);
                /*JsonNode jn = new JsonNode() {
                    .}*/
                var map = orderedMapOfEntries(
                        entry("code", code),
                        entry("product_code", code),
                        entry("source", "polsoft"), //TODO multi tenant
                        entry("source_id", sourceId),
                        entry("ean", product.get("towar_ean_sztuka")),
                        entry("unit", product.get("tw_jm")),
                        entry("tax", String.format("%s%%",product.get("towar_vat"))),
                        //entry("status", "Y"), //Active, or D - Deleted, or N - inactive
                        entry("lang", "pl"),
                        //entry("debug", timestamp)
                        entry("producer",
                            Optional.of(product.get("towar_producent")).map(producerId -> {
                                    var producer = finalProducers.get(producerId);
                                    if(producer == null) {
                                        return null;
                                    } else {
                                        return orderedMapOfStrings(
                                                "source_id", producerId,
                                                "name", producer,
                                                "source", "polsoft" //TODO multitenant
                                        );
                                    }
                                })),
                        entry("name", Map.of("pl", product.get("towar_nazwa"))),
                        entry("categories",
                                List.of(
                                        List.of(
                                                orderedMapOfEntries(
                                                        entry("source_id", product.get("nr_grupy")),
                                                        entry("name",
                                                                Map.of("pl", finalGroups.get(product.get("nr_grupy"))))
                                                )
                                        )
                                )),
                        entry("attrs",
                                List.of(
                                        orderedMapOfStrings(
                                                "name", "GRATIS",
                                                "value", product.get("towar_gratis"),
                                                "lang", "pl"
                                        ),
                                        orderedMapOfStrings(
                                                "name", "ZBIORCZE",
                                                "value", product.get("towar_ilosc_opak_zb"),
                                                "lang", "pl"
                                        ),
                                        Optional.ofNullable(product.get("substancja_czynna")).map(extAttr -> {
                                            if (extAttr.isEmpty()) {
                                                return null;
                                            } else {
                                                return orderedMapOfStrings(
                                                        "name", "SUBSTANCJA CZYNNA",
                                                        "value", extAttr,
                                                        "lang", "pl"
                                                );
                                            }
                                        })
                                )
                        ),
                        entry("stocks",
                                List.of(orderedMapOfStrings(
                                        "source_id", String.format("%s:%s", dept, sourceId),
                                        "source", "polsoft", //TODO multi-tenant
                                        "warehouse_id", warehouseId,
                                        "quantity", finalStocks.getOrDefault(sourceId, "0"),
                                        "price", product.get("towar_cena1")
                                )))
                );

            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /*try(
                var productReader = deptDir.reader("towary.txt");
                var stocksReader = deptDir.reader("stany.txt");
                var producersReader = deptDir.reader("produc.txt");
                var groups = deptDir.reader("grupy.txt");
                )*/
    }


}