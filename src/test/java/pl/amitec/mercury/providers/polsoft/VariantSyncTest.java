package pl.amitec.mercury.providers.polsoft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.formats.Charsets;
import pl.amitec.mercury.persistence.HashCache;
import pl.amitec.mercury.providers.bitbee.BitbeeClient;
import pl.amitec.mercury.transport.Transport;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.amitec.mercury.TestUtil.fileReader;

@ExtendWith(MockitoExtension.class)
public class VariantSyncTest {
    @Mock
    HashCache hashCache;
    @Mock
    BitbeeClient rbc;
    @Mock
    Transport deptDir;

    JobContext jobContext;

    @BeforeEach
    public void setup() throws IOException {
        jobContext = new JobContext(
                hashCache, rbc,
                Map.of(
                        "tenant", "mm"
                ),
                new SyncStats()
        );

        when(deptDir.reader(eq("produc.txt"))).thenReturn(
                fileReader("polsoft/test1/produc.txt", Charsets.ISO_8859_2));

        when(deptDir.reader(eq("stany.txt"))).thenReturn(
                fileReader("polsoft/test1/stany.txt", Charsets.ISO_8859_2));

        when(deptDir.reader(eq("towary.txt"))).thenReturn(
                fileReader("polsoft/test1/towary.txt", Charsets.ISO_8859_2));

        when(deptDir.reader(eq("grupy.txt"))).thenReturn(
                fileReader("polsoft/test1/grupy.txt", Charsets.ISO_8859_2));

        when(rbc.getWarehouseId("polsoft", "1")).thenReturn(Optional.of("3"));

    }

    @Test
    public void testSimpleProduct() throws IOException {
        new VariantSync().sync(jobContext, deptDir, "1", Set.of("1"));

        verify(hashCache).hit(eq("mm"), eq("ps"), eq("p"), eq("1:1"), eq("""
                {"code":"2/413","product_code":"2/413","source":"polsoft","source_id":"1","ean":"5903407024134","unit":"KPL","tax":"23%","lang":"pl","producer":{"source_id":"13","name":"CAN -PRODUKTY FIRMY CANPOL BABIES","source":"polsoft"},"name":{"pl":"SZCZ.DO BUT.2/413 I SMOCZKÓW"},"categories":[[{"source_id":"48","name":{"pl":"SZCZOTKI DO BUTELEK"}}]],"attrs":[{"name":"GRATIS","value":"0","lang":"pl"},{"name":"ZBIORCZE","value":"1","lang":"pl"}],"stocks":[{"source_id":"1:1","source":"polsoft","warehouse_id":"3","quantity":"42","price":"3.90"}]}"""), any());
    }

}
