package pl.amitec.mercury.providers.polsoft;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.transport.Transport;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderSync {
    public static final DateTimeFormatter ADDED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final Logger LOG = LoggerFactory.getLogger(OrderSync.class);

    private static final Pattern UNIQUE_NUMBER_PATTERN = Pattern.compile("^RB0*(\\d+)-0*(\\d+)$");
    public void sync(JobContext ctx, Transport transport, String dept) {
        Transport importDir = transport.subdir(String.format("IMPORT_ODDZ_%s", dept));
        ArrayNode journalOrders = (ArrayNode) ctx.redbayClient().getOrdersJournal().path("list");
        if(!journalOrders.isArray()) {
            throw new RuntimeException("Orders is not array");
        }
        if(journalOrders.isEmpty()) {
            LOG.info("No orders");
            return;
        }
        journalOrders.forEach((item) -> {
            processOrder(ctx, item, importDir);
        });
    }

    private static boolean processOrder(JobContext ctx, JsonNode item, Transport importDir) {
        var journalId = item.get("id").asText();
        var orderId = item.get("objectId").asText();
        var order = ctx.redbayClient().getOrder(orderId);

        if(order == null) {
            LOG.info("Order {} not found in backend", orderId);
            return false;
        }

        Matcher matcher = UNIQUE_NUMBER_PATTERN.matcher(order.get("uniqueNumber").asText());
        if (!matcher.find()) {
            throw new IllegalStateException("Invalid unique number format");
        }

        String prefix = matcher.group(1);
        String sequenceNo = matcher.group(2);
        String orderNo = "S" + prefix + "-" + sequenceNo;

        CSVFormat outputFormat = CSVFormat.newFormat('\t').builder()
                .setRecordSeparator('\n')
                .setQuote(null)
                .build();

        StringBuilder headerData = new StringBuilder();
        LocalDateTime addedAt = LocalDateTime.parse(order.get("added").get("date").asText(), ADDED_DATE_FORMAT);
        try(CSVPrinter headerCsv = outputFormat.print(headerData)) {
            headerCsv.printRecord("nrfak", "rodzdok", "nrodb", "idhandl", "datasp", "uwagi");
            headerCsv.printRecord(
                    orderNo,
                    "ZA",
                    order.get("contact").get("company").get("sourceid").asText(),
                    "0",
                    addedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    order.get("comment").asText().replaceAll("[\n\r\t]", " ")
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Set<String> sources = new HashSet<>();
        StringBuilder positionsData = new StringBuilder();

        try(var positionsCsv = outputFormat.print(positionsData)) {
            positionsCsv.printRecord("nrfak", "rodzdok", "nrtow", "vat", "ilosc", "cenan", "uwagi_do_lini", "zestaw");
            for (JsonNode position : order.get("positions")) {
                positionsCsv.printRecord(
                        orderNo,
                        "ZA",
                        position.get("variantSourceId").asText(),
                        "0",
                        position.get("quantity").asText(),
                        position.get("price").asText(),
                        position.get("comment").asText(),
                        "0"
                );
                sources.add(position.get("variantSource").asText());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (sources.contains("polsoft")) { // TODO change to dynamic source
            System.out.println(headerData.toString());
            System.out.println(positionsData.toString());

            String marker = addedAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "." + sequenceNo;
            importDir.write("N" + marker + ".txt", headerData.toString());
            importDir.write("P" + marker + ".txt", positionsData.toString());
            importDir.write("f" + marker + ".txt", "");
        } else {
            LOG.debug("Order {} has no positions for source {}", orderId, "polsoft"); // TODO multisource
        }

        ctx.redbayClient().confirmJournalItem(journalId);
        return true;
    }


}
