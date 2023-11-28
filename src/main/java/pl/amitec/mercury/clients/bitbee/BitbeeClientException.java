package pl.amitec.mercury.clients.bitbee;

public class BitbeeClientException extends RuntimeException {
    public BitbeeClientException(Exception e) {
        super(e);
    }

    public BitbeeClientException(String s) {
        super(s);
    }

    public BitbeeClientException(String s, Exception e) {
        super(s, e);
    }
}
