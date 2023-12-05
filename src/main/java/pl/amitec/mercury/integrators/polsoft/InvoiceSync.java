package pl.amitec.mercury.integrators.polsoft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.clients.bitbee.types.Invoice;
import pl.amitec.mercury.clients.bitbee.types.InvoiceListElement;
import pl.amitec.mercury.formats.CSVHelper;
import pl.amitec.mercury.persistence.Cache;
import pl.amitec.mercury.transport.Transport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InvoiceSync {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceSync.class);

    final static DateTimeFormatter DOT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    final static DateTimeFormatter DASH_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public InvoiceSync() {
    }

    public Set<String> sync(JobContext jobContext,
                            Transport deptDir, String dept, Set<String> selectedSourceIds) {
        var csvHelper = new CSVHelper();
        String source = "polsoft";
        var bitbeeClient = jobContext.bitbeeClient();
        var cache = jobContext.hashCache();

        try (var invoiceReader = deptDir.reader("rozrach.txt")) {
            var psInvoices = csvHelper.streamCSV(invoiceReader);
            psInvoices.forEach((psInvoice) -> {
                try {
                    syncInvoice(jobContext, dept, psInvoice, cache, source, bitbeeClient);
                } catch (Exception e) {
                    LOG.error("Failed to sync invoice", e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void syncInvoice(JobContext jobContext, String dept, Map<String, String> psInvoice, Cache cache, String source, BitbeeClient bitbeeClient) {
        var sourceId = psInvoice.get("nr_fakt");
        var deptSourceId = String.format("%s:%s", dept, sourceId);
        cache.hit(jobContext.getTenant(), source,"i", deptSourceId , psInvoice, inv -> {
            Optional<InvoiceListElement> existing = bitbeeClient.getInvoiceBySourceId(source, deptSourceId);
            var psCompanyId = psInvoice.get("kt_numer_platnik");
            var bbInvoice = Invoice.builder()
                    .id(existing.map(InvoiceListElement::id).orElse(null))
                    .source(source)
                    .sourceId(deptSourceId)
                    .number(psInvoice.get("symbol_fakt"))
                    .generated(asLocalDate(psInvoice.get("data_wyst"), DOT_DATE_FORMAT))
                    .payment(asLocalDate(psInvoice.get("data_platn"), DOT_DATE_FORMAT))
                    .paid(asLocalDate(psInvoice.get("data_ost_splaty"), DASH_DATE_FORMAT))
                    .deposit(new BigDecimal(psInvoice.get("dlug")))
                    .netto(new BigDecimal(psInvoice.get("wartosc_netto")))
                    .brutto(new BigDecimal(psInvoice.get("kwota")))
                    .company(bitbeeClient.getCompanyBySourceId(source, psCompanyId).orElseThrow()).build();
            if(existing.isEmpty()) {
                bitbeeClient.addInvoice(bbInvoice);
            } else {
                bitbeeClient.updateInvoice(bbInvoice);
            }
        });
    }

    public LocalDate asLocalDate(String str, DateTimeFormatter dateTimeFormatter) {
        if(str == null || str.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(str, dateTimeFormatter);
    }
}