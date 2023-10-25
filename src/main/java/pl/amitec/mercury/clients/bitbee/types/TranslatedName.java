package pl.amitec.mercury.clients.bitbee.types;

import java.util.HashMap;

public class TranslatedName extends HashMap<String, String> {

    public static TranslatedName of(String lang, String name) {
        return new TranslatedName().add(lang, name);
    }

    public TranslatedName add(String lang, String name) {
        put(lang, name);
        return this;
    }
}
