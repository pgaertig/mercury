package pl.amitec.mercury.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipUtil {
    public static void readResourceStream(String path, String entryName, Consumer<InputStream> consumer) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(entryName);
        var rs = ZipUtil.class.getResourceAsStream(path);
        Objects.requireNonNull(rs);
        try(ZipInputStream zis = new ZipInputStream(rs)) {
            var entry = zis.getNextEntry();
            if(entryName.equals(entry.getName())) {
                try(InputStream is = new ByteArrayInputStream(zis.readAllBytes())) {
                    consumer.accept(is);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
