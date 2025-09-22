package pl.amitec.mercury;

public class MercuryException extends RuntimeException {
    public MercuryException(String str, Exception e) {
        super(str, e);
    }

    public MercuryException(String str) {
        super(str);
    }

    public MercuryException(Exception e) {
        super("Generic exception", e);
    }
}
