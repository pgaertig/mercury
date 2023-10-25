package pl.amitec.mercury.util;

import java.io.FileReader;
import java.io.IOException;
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
    public static Map<String, String> propertiesFileToMap(String filePath) {
        Properties props = new Properties();
        try {
            props.load(new FileReader(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return propertiesToMap(props);
    }
}
