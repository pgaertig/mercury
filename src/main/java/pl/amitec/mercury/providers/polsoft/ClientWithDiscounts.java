package pl.amitec.mercury.providers.polsoft;

import org.apache.commons.csv.CSVRecord;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClientWithDiscounts {
    public static final String DISCOUNT_TOWAR_NUMER = "towar_numer";
    public static final String DISCOUNT_TOWAR_CENA_KONTRAH = "towar_cena_kontrah";

    public static final String KT_NUMER = "kt_numer";

    private final Map<String, String> client;
    private final Map<String, String> discounts;

    public ClientWithDiscounts(Map<String, String> client) {
        this.client = client;
        this.discounts = new LinkedHashMap<>();
    }

    public Map<String, String> getClient() {
        return client;
    }

    public Map<String, String> getDiscounts() {
        return discounts;
    }

    public void addDiscount(CSVRecord discount) {
        discounts.put(discount.get(DISCOUNT_TOWAR_NUMER), discount.get(DISCOUNT_TOWAR_CENA_KONTRAH));
    }
}
