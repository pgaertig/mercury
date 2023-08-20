package pl.amitec.mercury.providers.polsoft;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ClientDiscountStreamer {

    private static final Logger LOG = LogManager.getLogger(ClientDiscountStreamer.class);

    /**
     * Returns wrapper stream of clients with discounts from CSV formatted reader input.
     * Equivalent of:
     * <pre>
     *     SELECT clients.*, TABLE(discounts.towar_numer, discount.towar_cena_kontrah)
     *     FROM clients LEFT JOIN discounts ON clients.kt_numer = discounts.kt_numer
     *     GROUP BY discounts.kt_numer
     * </pre>
     * The clients stream is preloaded and the discounts streams is lazily scanned as it is bigger.
     * The discounts stream is ordered by kt_numer, however is not sorted by it.
     * @param clientsCSVReader
     * @param discountsCSVReader
     * @return stream of ClientWithDiscounts
     * @throws IOException
     */
    public Stream<ClientWithDiscounts> stream(Reader clientsCSVReader, Reader discountsCSVReader) throws IOException {
        CSVFormat csvFormat = CSVFormat.TDF.builder().setQuote(null).setHeader().setTrim(true).setAllowMissingColumnNames(true).build();

        CSVParser clientsParser = csvFormat.parse(clientsCSVReader);
        Map<String, Map<String,String>> clientsMap = clientsParser.stream().map(CSVRecord::toMap).collect(
                Collectors.toMap(
                        client -> client.get(ClientWithDiscounts.KT_NUMER), //key - company ID
                        client -> client, // company - value
                        (existing, replacement) -> replacement // in case of conflict
                )
        );

        CSVParser discountParser = csvFormat.parse(discountsCSVReader);

        Iterator<CSVRecord> discountIterator = discountParser.stream().iterator();

        return StreamSupport.stream(new Spliterators.AbstractSpliterator<ClientWithDiscounts>(Long.MAX_VALUE, Spliterator.ORDERED) {
            private CSVRecord buffer = null;
            @Override
            public boolean tryAdvance(Consumer<? super ClientWithDiscounts> action) {
                if (buffer == null && !discountIterator.hasNext()) {
                    if(!clientsMap.isEmpty()) {
                        LOG.warn("Clients without discounts, count={}", clientsMap.size());
                        clientsMap.forEach((id, client) -> action.accept(new ClientWithDiscounts(client)));
                        clientsMap.clear(); //required as tryAdvance will attempt one more time
                    }
                     return false;
                }

                ClientWithDiscounts clientWithDiscounts = null;

                if (buffer != null) {
                    // first in group, consumed by previous group scan
                    clientWithDiscounts = new ClientWithDiscounts(
                            clientsMap.remove(buffer.get(ClientWithDiscounts.KT_NUMER)));
                    clientWithDiscounts.addDiscount(buffer);
                    buffer = null;
                }

                while (discountIterator.hasNext()) {
                    CSVRecord discount = discountIterator.next();
                    if (clientWithDiscounts == null) {
                        // first in group
                        clientWithDiscounts = new ClientWithDiscounts(
                                clientsMap.remove(discount.get(ClientWithDiscounts.KT_NUMER)));
                        clientWithDiscounts.addDiscount(discount);
                    } else if (discount.get(ClientWithDiscounts.KT_NUMER).equals(
                            clientWithDiscounts.getClient().get(ClientWithDiscounts.KT_NUMER))) {
                        // next in group matching
                        clientWithDiscounts.addDiscount(discount);
                    } else {
                        // found non-matching from the next group, however it is consumed so pass to the next
                        buffer = discount;
                        break;
                    }
                }

                action.accept(clientWithDiscounts);
                return true;
            }
        }, false);
    }
}
