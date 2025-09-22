package pl.amitec.mercury;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TestUtil {

    public static Reader fileReader(String testResource, Charset charset) {
        return new BufferedReader(new InputStreamReader(
                TestUtil.class.getClassLoader().getResourceAsStream(testResource),
                charset));
    }

    public static String readFile(String testResource) {
        return new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(TestUtil.class.getClassLoader().getResourceAsStream(testResource)),
                StandardCharsets.UTF_8)).lines().reduce("", String::concat);
    }
}
