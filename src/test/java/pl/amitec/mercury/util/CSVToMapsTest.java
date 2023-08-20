package pl.amitec.mercury.util;

import org.junit.jupiter.api.Test;
import pl.amitec.mercury.TestUtil;
import pl.amitec.mercury.formats.Charsets;
import pl.amitec.mercury.util.CSVToMaps;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CSVToMapsTest {

    @Test
    public void testCSVToMapStream() throws IOException {
        var csvToMap = new CSVToMaps();
        var list = csvToMap.stream(TestUtil.fileReader("polsoft/test1/produc.txt", Charsets.ISO_8859_2)).toList();
        assertThat(list, hasSize(154));
        var lastMap = list.get(153);
        assertThat(lastMap,
                allOf(
                    hasEntry("prd_numer", "2223"),
                    hasEntry("prd_nazwa", "POO-POOFI & CO")));
    }
}
