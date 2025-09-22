package pl.amitec.mercury.integrators.polsoft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.amitec.mercury.SyncStats;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.clients.bitbee.types.Warehouse;
import pl.amitec.mercury.engine.JobContext;
import pl.amitec.mercury.formats.Charsets;
import pl.amitec.mercury.persistence.HashCache;
import pl.amitec.mercury.transport.Transport;

import java.io.IOException;
import java.util.Map;
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
                        "tenant", "mm",
                        "bitbee.source", "polsoft"
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

    }

    @Test
    public void testSimpleProduct() throws IOException {
        Warehouse warehouse = Warehouse.builder().id(3).name("").source("").sourceId("").build();
        when(rbc.getOrCreateWarehouse(any())).thenReturn(warehouse);

        new VariantSync().sync(jobContext, deptDir, "1", Set.of("1"));

        verify(hashCache).hit(eq("mm"), eq("ps"), eq("p"), eq("1:1"), eq("""
                {"code":"1","product_code":"1","source":"polsoft","source_id":"1","ean":"5903407024134","unit":"KPL","tax":"23%","lang":"pl","producer":{"source_id":"13","name":"CAN -PRODUKTY FIRMY CANPOL BABIES","source":"polsoft"},"name":{"pl":"SZCZ.DO BUT.2/413 I SMOCZKÓW"},"categories":[[{"source_id":"48","name":{"pl":"SZCZOTKI DO BUTELEK"}}]],"attrs":[{"code":"GRATIS","name":"GRATIS","value":"0","lang":"pl"},{"code":"ZBIORCZE","name":"ZBIORCZE","value":"1","lang":"pl"},{"code":"KOD","name":"KOD","value":"2/413","lang":"pl"}],"stocks":[{"source_id":"1:1","source":"polsoft","warehouse_id":"3","quantity":"42","price":"3.90","retail_price":null}]}"""), any());
    }

    @Test
    public void testWarehouseCreation() throws IOException {
        Warehouse warehouse1 = Warehouse.builder().name("Magazyn 1").source("polsoft").sourceId("1").availability(24).build();
        Warehouse warehouse2 = Warehouse.builder().id(3).name("Magazyn 1").source("polsoft").sourceId("1").availability(24).build();
        when(rbc.getOrCreateWarehouse(eq(warehouse1))).thenReturn(warehouse2);

        new VariantSync().sync(jobContext, deptDir, "1", Set.of("1"));

        verify(hashCache).hit(eq("mm"), eq("ps"), eq("p"), eq("1:1"), eq("""
                {"code":"1","product_code":"1","source":"polsoft","source_id":"1","ean":"5903407024134","unit":"KPL","tax":"23%","lang":"pl","producer":{"source_id":"13","name":"CAN -PRODUKTY FIRMY CANPOL BABIES","source":"polsoft"},"name":{"pl":"SZCZ.DO BUT.2/413 I SMOCZKÓW"},"categories":[[{"source_id":"48","name":{"pl":"SZCZOTKI DO BUTELEK"}}]],"attrs":[{"code":"GRATIS","name":"GRATIS","value":"0","lang":"pl"},{"code":"ZBIORCZE","name":"ZBIORCZE","value":"1","lang":"pl"},{"code":"KOD","name":"KOD","value":"2/413","lang":"pl"}],"stocks":[{"source_id":"1:1","source":"polsoft","warehouse_id":"3","quantity":"42","price":"3.90","retail_price":null}]}"""), any());
    }

}
