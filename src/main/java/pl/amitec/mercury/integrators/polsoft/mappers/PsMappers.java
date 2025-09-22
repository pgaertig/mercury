package pl.amitec.mercury.integrators.polsoft.mappers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.formats.CSVHelper;
import pl.amitec.mercury.integrators.polsoft.model.PsStock;
import pl.amitec.mercury.integrators.polsoft.model.PsStocks;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class PsMappers {

    public static final String SHORTEST_DATE_COL = "najkrotsza_data";

    public static PsStocks mapStocks(Reader inputFile) {
        try {
            CSVHelper csvHelper = new CSVHelper();
            CSVParser parser = CSVFormat.TDF.builder()
                    .setQuote(null)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setAllowMissingColumnNames(true)
                    .build()
                    .parse(inputFile);

            Map<String, PsStock> stockMap = new HashMap<>();
            parser.forEach(record -> {
                PsStock stock = PsStock.builder()
                        .productId(record.get("towar_numer"))
                        .amount(record.get("towar_ilosc"))
                        .shortestExpirationDate(record.isSet(SHORTEST_DATE_COL) ? record.get(SHORTEST_DATE_COL) : null)
                        .build();
                stockMap.put(stock.productId(), stock);
            });

            return PsStocks.builder()
                    .map(stockMap)
                    .hasShortestExpirationDate(parser.getHeaderMap().containsKey(SHORTEST_DATE_COL))
                    .build();
        } catch (IOException e) {
            throw new MercuryException("Error parsing stocks CSV file", e);
        }
    }
}
