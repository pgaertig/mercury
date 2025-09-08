package pl.amitec.mercury.formats;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CSVHelper {

    private static final Logger LOG = LoggerFactory.getLogger(CSVHelper.class);
    public static final String LINE = "__line__";

    private final CSVFormat csvFormat;

    public CSVHelper() {
        csvFormat = CSVFormat.TDF.builder()
                .setQuote(null)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setAllowMissingColumnNames(true)
                .build();
    }

    public CSVParser parse(Reader lines) throws IOException {
        return csvFormat.parse(lines);
    }

    public class CSVRecordMap extends AbstractMap<String, String> {
        private final CSVRecord record;

        public CSVRecordMap(CSVRecord record) {
            this.record = record;
        }

        @Override
        public Set<Map.Entry<String, String>> entrySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String get(Object key) {
            if (LINE.equals(key)) {
                return record.toString();
            }
            if(record.isMapped((String) key)) {
                return record.get((String) key);
            } else {
                return null;
            }
        }

        @Override
        public boolean containsKey(Object key) {
            if (LINE.equals(key)) {
                return true;
            }
            return record.isMapped((String) key);
        }

        @Override
        public String toString() {
            return record.toString();
        }
    }

    public interface AutoClosableIterable<E> extends Iterator<E>, AutoCloseable {}

    public Iterable<Map<String, String>> streamCSV(Reader lines) throws IOException {
        CSVParser parser = csvFormat.parse(lines);

        return () -> new Iterator<>() {
            private final Iterator<CSVRecord> recordIterator = parser.iterator();

            @Override
            public boolean hasNext() {
                return recordIterator.hasNext();
            }

            @Override
            public Map<String, String> next() {
                return new CSVRecordMap(recordIterator.next());
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