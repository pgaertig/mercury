package pl.amitec.mercury;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.*;

import java.io.IOException;

public class TestRealPolsoft {

    @BeforeAll
    static void setup(){
        System.out.println("@BeforeAll executed");
    }

    @BeforeEach
    void setupThis() {
        System.out.println("@BeforeEach executed");
    }

    @Tag("DEV")
    @Test
    void testCalcOne() {
    }

    @Tag("PROD")
    @Test
    void testCalcTwo() throws IOException {
        var ftp = new FTPClient();
        ftp.connect("ftp.redbay.pl");
        ftp.login("minimaxi", "***REMOVED***");
        /*var ftpHelper = new FTPHelper();
        ftpHelper.listAllFiles(ftp, "");
        ftp.connect("xy");*/
    }

    @AfterEach
    void tearThis() {
        System.out.println("@AfterEach executed");
    }

    @AfterAll
    static void tear(){
        System.out.println("@AfterAll executed");
    }
}
