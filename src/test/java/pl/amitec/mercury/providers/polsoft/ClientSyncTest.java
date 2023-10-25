package pl.amitec.mercury.providers.polsoft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.formats.Charsets;
import pl.amitec.mercury.persistence.HashCache;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.transport.Transport;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static pl.amitec.mercury.TestUtil.fileReader;

@ExtendWith(MockitoExtension.class)
public class ClientSyncTest {

    @Mock HashCache hashCache;
    @Mock
    BitbeeClient rbc;
    @Mock Transport deptDir;

    JobContext jobContext;

    @BeforeEach
    public void setup() throws IOException {
        jobContext = new JobContext(
                hashCache, rbc,
                Map.of(
                        "tenant", "mm"
                ), new SyncStats()
        );

        when(deptDir.exists(anyString())).thenReturn(true);
//        when(deptDir.read(eq("toppc.txt"))).thenReturn("KONIEC");

        when(deptDir.reader(eq("klienci.txt"))).thenReturn(
                fileReader("polsoft/test1/klienci.txt", Charsets.ISO_8859_2));

        when(deptDir.reader(eq("rabaty.txt"))).thenReturn(
                fileReader("polsoft/test1/rabaty.txt", Charsets.ISO_8859_2));
    }

    @Test
    public void testWithStocks() throws IOException {
        new ClientSync().sync(jobContext, deptDir, "1", List.of("1"));

        verify(hashCache).hit(eq("mm"), eq("ps"), eq("c"), eq("1:1"), eq("""
                {"source_id":"1","source":"polsoft","name":"PPHU mini-maxi hurt farm.","email":"1@test.pl","phone":"000000000","street":"RYCHLA 18","postcode":"41-948","city":"PIEKARY SL.","province":null,"nip":"6530003759","country":"PL","properties":{"iph_department":"1","iph_pricetype":"0","iph_discount":"0.00","iph_debt":"0.00","iph_sector":"F"},"stock_discounts":[{"source_id":"1:1","price":"3.90"},{"source_id":"1:2","price":"4.64"},{"source_id":"1:20","price":"7.93"},{"source_id":"1:21","price":"3.48"},{"source_id":"1:35","price":"5.70"}]}"""), any());
    }

    @Test
    public void testWithoutStocks() throws IOException {
        new ClientSync().sync(jobContext, deptDir, "1", List.of("48"));

        verify(hashCache).hit(eq("mm"), eq("ps"), eq("c"), eq("1:48"), eq("""
                {"source_id":"48","source":"polsoft","name":"PHUP VIKI","email":"48@test.pl","phone":"000 000 00 00","street":"ROOSVELTA 54","postcode":"41-800","city":"ZABRZE","province":"śląskie","nip":"6310208979","country":"PL","properties":{"iph_department":"1","iph_pricetype":"0","iph_discount":"0.00","iph_debt":"0.00","iph_sector":"D"}}"""), any());
    }
}