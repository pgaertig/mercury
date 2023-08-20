package pl.amitec.mercury.dict;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PostCodeTest {

    @Test
    public void testCodeToProvince() {
        assertThat(PostCodes.getInstance().codeToProvince("62-030"), equalTo("wielkopolskie"));
        assertThat(PostCodes.getInstance().codeToProvince("41-949"), equalTo("śląskie"));
        assertThat(PostCodes.getInstance().codeToProvince("41-948"), nullValue());
    }
}
