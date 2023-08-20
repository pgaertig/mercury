package pl.amitec.mercury.providers.polsoft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.dict.PostCodes;
import pl.amitec.mercury.transport.Transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static pl.amitec.mercury.util.Utils.*;

public class ClientSync implements PsCommonSync {

    private static final Logger LOG = LogManager.getLogger(ClientSync.class);

    public ClientSync() {
    }

    public void sync(JobContext jobContext,
            Transport deptDir, int dept, List<String> variantSourceIds, List<String> selectedSourceIds) throws IOException {
        //stats
        if(!isComplete(deptDir)) {
            return;
        }

        if(!deptDir.exists("klienci.txt")) {
            LOG.warn("Complete but klienci.txt doesn't exist, deptDir={}", deptDir);
            return;
        }

        if(!deptDir.exists("rabaty.txt")) {
            LOG.warn("Complete but rabaty.txt doesn't exist, deptDir={}", deptDir);
        }

        try(
                var clientsReader = deptDir.reader("klienci.txt");
                var discountsReader = deptDir.reader("rabaty.txt")) {

            var clientDiscountsStream = new ClientDiscountStreamer().stream(clientsReader, discountsReader);

            clientDiscountsStream.forEach(clientWithDiscounts -> {
                var client = clientWithDiscounts.getClient();
                var id = client.get("kt_numer");

                if(selectedSourceIds != null && !selectedSourceIds.isEmpty() &&
                    !selectedSourceIds.contains(id)){
                    return; //continue
                }
                //build json

                Supplier<Map.Entry<String, List<Map<String, Object>>>> discountsEntry = () -> {
                    var discounts = clientWithDiscounts.getDiscounts();
                    if(discounts.isEmpty()) {
                        return null;
                    } else {
                        List<Map<String, Object>> discountsMapped = discounts.entrySet().stream().map(entry ->
                                orderedMapOfStrings(
                                        "source_id", String.format("%s:%s", dept, entry.getKey()),
                                        "price", entry.getValue()
                                )).toList();
                        return entry("stock_discounts", discountsMapped);
                    }
                };

                var struct = orderedMapOfEntries(
                        entry("source_id", id),
                        entry("source", "polsoft"), //TODO multitenant/multisource
                        entry("name", String.format("%s %s", client.get("kt_nazwa"), client.get("kt_nazwa_pom")).trim()),
                        entry("email", client.get("kt_email")),
                        entry("phone", client.getOrDefault("kt_telefon","").trim()),
                        entry("street", client.get("kt_ulica")),
                        entry("postcode", client.get("kt_kod_pocztowy")),
                        entry("city", client.get("kt_miasto")),
                        entry("province", PostCodes.getInstance().codeToProvince(client.get("kt_kod_pocztowy"))),
                        entry("nip", client.get("kt_nip")),
                        entry("country", "PL"),

                        entry("properties", orderedMapOfStrings(
                                "iph_department", dept,
                                "iph_pricetype", client.get("kt_rodzaj_ceny"),
                                "iph_discount", client.get("kt_rabat_auto"),
                                "iph_debt", client.get("kt_zadluzenie"),
                                "iph_sector", client.get("kategoria_1"))),

                        discountsEntry.get()
                );


                try {
                    var jsonMapper = new ObjectMapper();
                    jsonMapper.configure(MapperFeature.ALLOW_COERCION_OF_SCALARS, false);
                    String json = new ObjectMapper().writeValueAsString(struct);
                    jobContext.getHashCache().hit(
                            jobContext.getTenant(), "ps", "c", String.format("%s:%s", dept, id), json, (data) -> {
                                LOG.debug("JSON: {}", json);
                                jobContext.getRedbayClient().importCompany(json);
                            });
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e); //TODO
                } catch (IOException e) {
                    throw new RuntimeException(e); //TODO
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
