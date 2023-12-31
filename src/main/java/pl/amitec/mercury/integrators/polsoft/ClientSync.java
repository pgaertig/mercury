package pl.amitec.mercury.integrators.polsoft;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.dict.PostCodes;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.clients.bitbee.types.ImportClient;
import pl.amitec.mercury.clients.bitbee.types.StockDiscount;
import pl.amitec.mercury.transport.Transport;

import java.util.List;

import static pl.amitec.mercury.util.Utils.*;

public class ClientSync implements PsCommonSync {

    private static final Logger LOG = LoggerFactory.getLogger(ClientSync.class);

    public ClientSync() {
    }

    public void sync(JobContext jobContext,
            Transport deptDir, String dept, List<String> selectedSourceIds) {

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
                try {
                    if(syncClient(jobContext, dept, selectedSourceIds, clientWithDiscounts)) {
                        LOG.debug("Polsoft client sync: {}", jobContext.syncStats());
                    };
                } catch (Exception e) {
                    LOG.error(String.format("Failed client sync: %s", clientWithDiscounts), e);
                }
            }
            );
            LOG.info("Polsoft client sync done: {}", jobContext.syncStats());
        } catch (Exception e) {
            LOG.error("Failed clients sync.", e);
        }
    }

    public static boolean syncClient(JobContext jobContext, String dept, List<String> selectedSourceIds, ClientWithDiscounts clientWithDiscounts) {
        jobContext.syncStats().incEntries();
        var client = clientWithDiscounts.getClient();
        var id = client.get("kt_numer");

        if(selectedSourceIds != null && !selectedSourceIds.isEmpty() &&
            !selectedSourceIds.contains(id)){
            return false;
        }

        var clientDto = ImportClient.builder()
                .sourceId(id)
                .source("polsoft")
                .name(String.format("%s %s", client.get("kt_nazwa"), client.get("kt_nazwa_pom")).trim())
                .email(client.get("kt_email"))
                .phone(client.getOrDefault("kt_telefon","").trim())
                .street(client.get("kt_ulica"))
                .postcode(client.get("kt_kod_pocztowy"))
                .city(client.get("kt_miasto"))
                .province(PostCodes.getInstance().codeToProvince(client.get("kt_kod_pocztowy")))
                .nip(client.get("kt_nip"))
                .country("PL")
                .properties(orderedMapOfStrings(
                        "iph_department", dept,
                        "iph_pricetype", client.get("kt_rodzaj_ceny"),
                        "iph_discount", client.get("kt_rabat_auto"),
                        "iph_debt", client.get("kt_zadluzenie"),
                        "iph_sector", client.get("kategoria_1")
                ))
                .stockDiscounts(
                        clientWithDiscounts.getDiscounts().entrySet().stream().map(entry ->
                                StockDiscount.builder()
                                        .sourceId(String.format("%s:%s", dept, entry.getKey()))
                                        .price(entry.getValue()).build()
                                ).toList()
                ).build();

        try {
            String json = BitbeeClient.JSON_MAPPER.writeValueAsString(clientDto);
            var hit = jobContext.hashCache().hit(
                    jobContext.getTenant(), "ps", "c", String.format("%s:%s", dept, id), json, (data) -> {
                        LOG.debug("JSON: {}", json);
                        jobContext.bitbeeClient().importCompany(json);
                    });
            if(hit) {
                jobContext.syncStats().incCached();
            } else {
                jobContext.syncStats().incSucceed();
            }
        } catch (JsonProcessingException e) {
            jobContext.syncStats().incFailed();
            LOG.error(String.format("Failed processing Client: %s", id), e);
            throw new RuntimeException(e);
        }
        return true;
    }
}
