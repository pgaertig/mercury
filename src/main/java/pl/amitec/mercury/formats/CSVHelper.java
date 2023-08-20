package pl.amitec.mercury.formats;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public class CSVHelper {

    private final CSVFormat csvFormat;

    public CSVHelper() {
        csvFormat = CSVFormat.TDF.builder().setSkipHeaderRecord(true).build();
    }

    public Iterable<Map<String, String>> streamCSV(Reader lines) throws IOException {
        CSVParser parser = csvFormat.parse(lines);

        return new Iterable<Map<String, String>>() {
            @Override
            public Iterator<Map<String, String>> iterator() {
                return new Iterator<Map<String, String>>() {
                    private final Iterator<CSVRecord> recordIterator = parser.iterator();

                    @Override
                    public boolean hasNext() {
                        return recordIterator.hasNext();
                    }

                    @Override
                    public Map<String, String> next() {
                        CSVRecord record = recordIterator.next();
                        Map<String, String> map = new HashMap<>();
                        for (String header : parser.getHeaderNames()) {
                            map.put(header, record.get(header));
                        }
                        return map;
                    }
                };
            }
        };
    }

    public Map<String, String> mapCSV(Reader csvReader, String keyColumn, String valueColumn) {
        try {
            CSVParser parser = csvFormat.parse(csvReader);
            return parser.stream().collect(
                    Collectors.toMap(
                            (row) -> row.get(keyColumn),
                            (row) -> row.get(valueColumn)
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}