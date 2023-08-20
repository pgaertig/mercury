package pl.amitec.mercury.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    public static String sha1HexDigest(String input) {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
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

    public static Map<String, Object> orderedMapOfStrings(Object... kvs){
        if(kvs.length % 2 != 0) {
            throw new IllegalArgumentException("KVs must have even number of elements");
        }
        var map = new LinkedHashMap<String, Object>(kvs.length / 2, 1);
        for (int i=0 ; i<kvs.length ; i+=2) {
            if(kvs[i] instanceof String key) {
                map.put(key, kvs[i + 1]);
            } else {
                throw new IllegalArgumentException("Keys must be strings: " + kvs[i]);
            }
        }
        return map;
    }

    public static <K, V> Map.Entry<K, V> entry(K k, V v) {
        Objects.requireNonNull(k);
        return new LinkedHashMap.SimpleEntry<>(k, v);
    }
}
