package pl.amitec.mercury.dict;

import org.apache.commons.csv.CSVFormat;
import pl.amitec.mercury.util.ZipUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class PostCodes {
    private static PostCodes INSTANCE;

    private Map<String,String> codeToProvince = new HashMap<>();
    private PostCodes() {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader()
                .build();
        this.getClass().getResource("dict/kody.csv.zip");

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
                        throw new RuntimeException(e);
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
