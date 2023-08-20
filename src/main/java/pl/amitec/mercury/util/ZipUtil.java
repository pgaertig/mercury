package pl.amitec.mercury.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

public class ZipUtil {
    public static void readResourceStream(String path, String entryName, Consumer<InputStream> consumer) {
        URL zipUrl = ZipUtil.class.getResource(path);
        InputStream inputStream = null;
        try(ZipFile zipFile = new ZipFile(new File(zipUrl.toURI()))) {
            var entry = zipFile.getEntry(entryName);
            try(InputStream is = zipFile.getInputStream(entry)) {
                consumer.accept(is);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
