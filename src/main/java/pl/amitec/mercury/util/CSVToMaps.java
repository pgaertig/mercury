package pl.amitec.mercury.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.stream.Stream;

public class CSVToMaps {

    public Stream<Map<String,String>> stream(Reader csvReader) throws IOException {
        CSVFormat csvFormat = CSVFormat.TDF.builder().setQuote(null).setHeader().setTrim(true).setAllowMissingColumnNames(true).build();
        return csvFormat.parse(csvReader).stream().map(CSVRecord::toMap);
    }

    public Stream<Map<String,String>> stream(String csv) throws IOException {
        return stream(new StringReader(csv));
    }
}
