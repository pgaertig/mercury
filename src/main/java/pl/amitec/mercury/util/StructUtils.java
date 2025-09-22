package pl.amitec.mercury.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class StructUtils {
    public static Map<String, String> propertiesToMap(Properties properties) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>(properties.size());
        for(String key: properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }
        return map;
    }
}
