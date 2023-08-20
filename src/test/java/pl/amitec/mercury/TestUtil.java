package pl.amitec.mercury;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

public class TestUtil {

    public static Reader fileReader(String testResource, Charset charset) {
        return new BufferedReader(new InputStreamReader(
                TestUtil.class.getClassLoader().getResourceAsStream(testResource),
                charset));
    }
}
