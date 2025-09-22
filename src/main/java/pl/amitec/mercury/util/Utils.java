package pl.amitec.mercury.util;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;

public class Utils {

    public static String sha1HexDigest(String input) {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
        byte[] hash = sha1.digest(input.getBytes());
        return bytesToHex(hash);
    }
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b: bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static <K, V> Map<K, V> orderedMapOfEntries(Map.Entry<? extends K, ? extends V>... entries){
        var map = new LinkedHashMap<K, V>(entries.length, 1);
        for(var entry: entries) {
            if(entry != null) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    public static Map<String, String> orderedMapOfStrings(String... kvs){
        if(kvs.length % 2 != 0) {
            throw new IllegalArgumentException("KVs must have even number of elements");
        }
        var map = new LinkedHashMap<String, String>(kvs.length / 2, 1);
        for (int i=0 ; i<kvs.length ; i+=2) {
            map.put(kvs[i], kvs[i + 1]);
        }
        return map;
    }

    public static <K, V> Map.Entry<K, V> entry(K k, V v) {
        Objects.requireNonNull(k);
        return new LinkedHashMap.SimpleEntry<>(k, v);
    }

    public static <T> List<T> compactListOf(T...items) {
        return Arrays.stream(items).filter(Objects::nonNull).toList();
    }

    public static ObjectNode jsonObject() {
        return JsonNodeFactory.instance.objectNode();
    }

    public static ObjectNode jsonObject(String... kvs) {
        if(kvs.length % 2 != 0) {
            throw new IllegalArgumentException("KVs must have even number of elements");
        }
        var node = jsonObject();

        for (int i=0 ; i<kvs.length ; i+=2) {
            node.put(kvs[i], kvs[i+1]);
        }
        return node;
    }
    public static ObjectNode jsonObjectWith(Consumer<ObjectNode> obj) {
        var node = JsonNodeFactory.instance.objectNode();
        if(obj != null) {
            obj.accept(node);
        }
        return node;
    }

    public static <T extends JsonNode> ArrayNode jsonArray(T... objs) {
        return jsonList(List.of(objs));
    }

    public static <T extends JsonNode> ArrayNode jsonList(List<T> elements) {
        return jsonArray().addAll(elements);
    }

    public static ArrayNode jsonArray() {
        return JsonNodeFactory.instance.arrayNode();
    }

    public static ArrayNode jsonArrayWith(Consumer<ArrayNode> arr) {
        var node = JsonNodeFactory.instance.arrayNode();
        if(arr != null) {
            arr.accept(node);
        }
        return node;
    }

    /**
     * Makes consistent string representation of a map (sorted by keys)
     * @param map
     * @return
     */
    public static <K extends Comparable<? super K>> String mapToString(Map<K, ?> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((e1, e2) -> e1 + ";" + e2)
                .orElse("");
    }
}
