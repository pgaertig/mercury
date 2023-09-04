package pl.amitec.mercury.providers.polsoft;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.formats.Charsets;
import pl.amitec.mercury.persistence.HashCache;
import pl.amitec.mercury.providers.redbay.RedbayClient;
import pl.amitec.mercury.transport.Transport;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.amitec.mercury.TestUtil.fileReader;

@ExtendWith(MockitoExtension.class)
class OrderSyncTest {

    @Mock
    HashCache hashCache;
    @Mock
    RedbayClient rbc;
    @Mock
    Transport deptDir;

    JobContext jobContext;

    @BeforeEach
    void setUp() throws IOException {
        jobContext = new JobContext(
                hashCache, rbc,
                Map.of(
                        "tenant", "mm"
                ), new SyncStats()
        );

        when(deptDir.subdir(eq("IMPORT_ODDZ_1"))).thenReturn(deptDir);

        when(rbc.getOrdersJournal()).thenReturn(new ObjectMapper().readTree(new StringReader("""
                {"list":[{"id":"59","shop":"3","type":"order","objectId":"146","active":"Y","added":"2023-08-28 11:08:59","modified":"2023-08-28 11:08:59","confirm":"N"},{"id":"60","shop":"3","type":"order","objectId":"147","active":"Y","added":"2023-08-28 11:24:31","modified":"2023-08-28 11:24:31","confirm":"N"},{"id":"61","shop":"3","type":"order","objectId":"148","active":"Y","added":"2023-08-28 12:19:37","modified":"2023-08-28 12:19:37","confirm":"N"}],"validate":true,"redirect":"","itemsCount":3,"result":null,"errors":[],"encoding":"UTF-8","exception":null,"messages":[],"code":200,"timestamp":{"date":"2023-08-28 12:27:40.027976","timezone_type":3,"timezone":"Europe\\/Warsaw"}}
                """)));

        when(rbc.getOrder("146")).thenReturn(new ObjectMapper().readTree(new StringReader("""
                {"object":{"id":146,"uniqueNumber":"RB003-000146","payment":{"id":20953,"name":"Przelew","provision":0},"delivery":{"id":1,"name":"Kurier","tax":null,"payments":[],"cost":0},"status":{"id":1,"type":"WAITING","name":"oczekuj\\u0105ce","visiblename":"oczekuj\\u0105ce","description":"Zam\\u00f3wienia oczekuj\\u0105ce na potwierdzenie"},"added":{"date":"2023-08-28 11:08:59.000000","timezone_type":3,"timezone":"Europe\\/Warsaw"},"modified":{"date":"2023-08-28 11:08:59.000000","timezone_type":3,"timezone":"Europe\\/Warsaw"},"source":"B2B","sourceid":"62684e1b-a78a-4670-be5f-aa131211b07f","netto":247.10000000000002,"tax":56.830000000000005,"brutto":303.93,"contact":{"company":{"id":3747,"nip":"0000000000","regon":"","fullname":"XYZ S.C.","street":"SEMAFOROWA 1","flat":"","postcode":"42-600","city":"TARNOWSKIE G\\u00d3RY","country":1,"province":12,"phone":"000 000 000","email":"kontakt@mysweetroom.pl","source":"polsoft","sourceid":"9103","type":1,"properties":[]},"id":1361,"forname":"XYZ","surname":"TARNOWSKIE G\\u00d3RY","phone":"000 000 000","email":"kontakt@mysweetroom.pl","street":"SEMAFOROWA 1","flat":"","postcode":"42-600","city":"TARNOWSKIE G\\u00d3RY","country":1,"province":17},"receiver":{"id":1360,"forname":"XYZ","surname":"TARNOWSKIE G\\u00d3RY","phone":"000 000 000","email":"kontakt@mysweetroom.pl","street":"SEMAFOROWA 1","flat":"","postcode":"42-600","city":"TARNOWSKIE G\\u00d3RY","country":1,"province":17},"invoice":{"company":{"id":3747,"nip":"0000000000","regon":"","fullname":"XYZ S.C.","street":"SEMAFOROWA 1","flat":"","postcode":"42-600","city":"TARNOWSKIE G\\u00d3RY","country":1,"province":12,"phone":"000 000 000","email":"kontakt@mysweetroom.pl","source":"polsoft","sourceid":"9103","type":1,"properties":[]},"id":1362,"forname":"XYZ","surname":"TARNOWSKIE G\\u00d3RY","phone":"000 000 000","email":"kontakt@mysweetroom.pl","street":"SEMAFOROWA 1","flat":"","postcode":"42-600","city":"TARNOWSKIE G\\u00d3RY","country":1,"province":17},"positions":[{"id":2167,"variant":11707,"variantName":"NADSTAWKA KLUP\\u015a PT70 415 \\u0141\\u0104KA","variantSource":"polsoft","variantSourceId":"52152","name":"NADSTAWKA KLUP\\u015a PT70 415 \\u0141\\u0104KA","code":"PT70415","price":47.7,"quantity":1,"tax":2,"taxName":null,"warehouse":3,"comment":""},{"id":2168,"variant":11712,"variantName":"NADSTAWKA KLUP\\u015a PT70 B001 BOHO RUSTY","variantSource":"polsoft","variantSourceId":"52157","name":"NADSTAWKA KLUP\\u015a PT70 B001 BOHO RUSTY","code":"PT70B001","price":47.7,"quantity":2,"tax":2,"taxName":null,"warehouse":3,"comment":""},{"id":2169,"variant":10325,"variantName":"NADSTAWKA KLUP\\u015a PT70 410 WACHL.POPIEL","variantSource":"polsoft","variantSourceId":"47656","name":"NADSTAWKA KLUP\\u015a PT70 410 WACHL.POPIEL","code":"PT70410","price":47.7,"quantity":1,"tax":2,"taxName":null,"warehouse":3,"comment":""},{"id":2170,"variant":16039,"variantName":"YOONCO PRZEWIJAK PT70 FOREST BE\\u017b","variantSource":"polsoft","variantSourceId":"54514","name":"YOONCO PRZEWIJAK PT70 FOREST BE\\u017b","code":"Y7599","price":52,"quantity":1,"tax":2,"taxName":null,"warehouse":3,"comment":""},{"id":2171,"variant":16037,"variantName":"YOONCO PRZEWIJAK PT70 FOREST \\u017bO\\u0141\\u0118DZIE","variantSource":"polsoft","variantSourceId":"54512","name":"YOONCO PRZEWIJAK PT70 FOREST \\u017bO\\u0141\\u0118DZIE","code":"Y7575","price":52,"quantity":1,"tax":2,"taxName":null,"warehouse":3,"comment":""}]},"validate":true,"redirect":"","itemsCount":0,"result":null,"errors":[],"encoding":"UTF-8","exception":null,"messages":[],"code":200,"timestamp":{"date":"2023-08-28 12:27:40.797942","timezone_type":3,"timezone":"Europe\\/Warsaw"}}                
                """)).get("object"));
    }

    @Test
    public void test() throws IOException {
        new OrderSync().sync(jobContext, deptDir, "1");

        verify(rbc).confirmJournalItem(eq("59"));

        verify(deptDir).write(matches("^N[0-9]+\\.146\\.txt$"), eq("""
                nrfak\trodzdok\tnrodb\tidhandl\tdatasp\tuwagi
                S3-146\tZA\t9103\t0\t2023-08-28 11:08:59\t
                """));
        verify(deptDir).write(matches("^P[0-9]+\\.146\\.txt"), eq("""
                nrfak\trodzdok\tnrtow\tvat\tilosc\tcenan\tuwagi_do_lini\tzestaw
                S3-146\tZA\t52152\t0\t1\t47.7\t\t0
                S3-146\tZA\t52157\t0\t2\t47.7\t\t0
                S3-146\tZA\t47656\t0\t1\t47.7\t\t0
                S3-146\tZA\t54514\t0\t1\t52\t\t0
                S3-146\tZA\t54512\t0\t1\t52\t\t0
                """));

        verify(deptDir).write(matches("^f[0-9]+\\.146\\.txt"), eq(""));

    }

    @AfterEach
    void tearDown() {
    }
}