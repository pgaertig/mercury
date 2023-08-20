package pl.amitec.mercury.providers.polsoft;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.amitec.mercury.TestUtil;
import pl.amitec.mercury.formats.Charsets;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ClientDiscountStreamerTest {
    private ClientDiscountStreamer clientDiscountJoin;

    @BeforeEach
    public void setUp() {
        clientDiscountJoin = new ClientDiscountStreamer();
    }

    @Test
    public void testJoin() throws IOException {
        Stream<ClientWithDiscounts> joined = clientDiscountJoin.stream(
                TestUtil.fileReader("polsoft/test1/klienci.txt", Charsets.ISO_8859_2),
                TestUtil.fileReader("polsoft/test1/rabaty.txt", Charsets.ISO_8859_2)
        );
        List<ClientWithDiscounts> list = joined.toList();
        assertThat(list.size(), equalTo(4));
        ClientWithDiscounts c1 = list.get(0);
        ClientWithDiscounts c36 = list.get(1);
        ClientWithDiscounts c26 = list.get(2);
        ClientWithDiscounts c48 = list.get(3);

        assertThat(c1.getClient(), hasEntry(ClientWithDiscounts.KT_NUMER,"1"));
        assertThat(c1.getDiscounts(), aMapWithSize(5));
        assertThat(c1.getDiscounts(), hasEntry("21","3.48"));

        assertThat(c36.getClient(), hasEntry(ClientWithDiscounts.KT_NUMER,"36"));
        assertThat(c36.getDiscounts(), aMapWithSize(5));
        assertThat(c36.getDiscounts(), hasEntry("1","3.94"));

        assertThat(c26.getClient(), hasEntry(ClientWithDiscounts.KT_NUMER,"26"));
        assertThat(c26.getDiscounts(), aMapWithSize(7));
        assertThat(c26.getDiscounts(), hasEntry("53","10.61"));

        assertThat(c48.getClient(),hasEntry(ClientWithDiscounts.KT_NUMER, "48"));
        assertThat(c48.getDiscounts(), anEmptyMap());
    }
}
