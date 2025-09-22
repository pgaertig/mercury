package pl.amitec.mercury.dict;

import org.apache.commons.csv.CSVFormat;
import pl.amitec.mercury.MercuryException;
import pl.amitec.mercury.util.ZipUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Polish post codes parser
 * Data source <a href="http://www.kody-pocztowe.dokladnie.com/">Baza kodów pocztowych (CC BY-SA 3.0)</a>
 */
public class PostCodes {
    private static PostCodes INSTANCE;

    private Map<String,String> codeToProvince = new HashMap<>();
    private PostCodes() {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader()
                .build();

        ZipUtil.readResourceStream("/dict/kody.csv.zip", "kody.csv",
                (csv) -> {
                    try {
                        csvFormat.parse(new InputStreamReader(csv)).
                                stream().forEach( (csvRecord) -> {
                                    String[] province = csvRecord.get("WOJEWÓDZTWO").split(" ");
                                    codeToProvince.put(
                                            csvRecord.get("KOD POCZTOWY"),
                                            province[province.length - 1]
                                    );
                        }
                        );
                    } catch (IOException e) {
                        throw new MercuryException("Post code parsing error", e);
                    }
                });
    }

    public static PostCodes getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new PostCodes();
        }
        return INSTANCE;
    }

    public String codeToProvince(String code) {
        return codeToProvince.get(code);
    }
}
