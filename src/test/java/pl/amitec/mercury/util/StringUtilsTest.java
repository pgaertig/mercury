package pl.amitec.mercury.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static pl.amitec.mercury.util.StringUtils.truncate;

class StringUtilsTest {

    @Test
    public void testTruncateWithEllipsisWithMaxLength() {
        assertThat(StringUtils.truncate("Hello World", 5), is("Hello…"));
        assertThat(StringUtils.truncate("Short", 5), is("Short"));
        assertThat(StringUtils.truncate("Short", 50), is("Short"));
        assertThat(StringUtils.truncate("ThreeCharactersLeft", 18), is("ThreeCharactersLeft"));
        assertThat(StringUtils.truncate(null, 5), nullValue());
    }

    @Test
    public void testTruncateWithEllipsisDefaultLength() {
        assertThat(StringUtils.truncate("This string is longer than forty characters and should be truncated."), is("This string is longer than forty charact…"));
        assertThat(StringUtils.truncate("Short"), is("Short"));
        assertThat(StringUtils.truncate(null), nullValue());
    }

    @Test
    public void testTruncateWithEllipsisWithShowCharacters() {
        assertThat(truncate("Hello World World",  5, true), is("Hello…<12 more>"));
        assertThat(truncate("Short", 5, true), is("Short"));
        assertThat(truncate("Short", 50, true), is("Short"));
        assertThat(truncate("NearlyTenCharactersLeft", 13, true), is("NearlyTenCharactersLeft"));
        assertThat(truncate(null, 5, true), nullValue());
    }

    @Test
    public void testTruncateWithEllipsisDefaultLengthWithShowCharacters() {
        assertThat(StringUtils.truncate("This string is longer than forty characters and should be truncated.", true), is("This string is longer than forty charact…<28 more>"));
        assertThat(StringUtils.truncate("Short", true), is("Short"));
        assertThat(StringUtils.truncate(null, true), nullValue());
    }
}