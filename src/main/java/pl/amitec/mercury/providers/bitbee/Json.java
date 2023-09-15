package pl.amitec.mercury.providers.bitbee;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class Json {
    private LinkedHashMap<String, Object> properties = new LinkedHashMap<>();

    @JsonAnyGetter
    public LinkedHashMap<String, Object> getProperties() {
        return properties;
    }

    @JsonAnySetter
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    public String getString(String name) {
        var property = properties.get(name);
        return property == null ? null : property.toString();
    }

    @Override
    public String toString() {
        return "Json{" +
                "properties=" + properties +
                '}';
    }

    public List<Object> getList(String name) {
        var property = properties.get(name);
        if(property == null) {
            return null;
        }
        if(property instanceof List l) {
            return l;
        } else if(property.getClass().isArray()) {
            return Arrays.asList((Object[])property);
        }
        return null;
    }
}
