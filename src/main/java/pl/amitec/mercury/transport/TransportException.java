package pl.amitec.mercury.transport;

import pl.amitec.mercury.MercuryException;

import java.io.IOException;

public class TransportException extends MercuryException {

    public TransportException(Transport transport, IOException e) {
        super(String.format("Problem with %s", transport), e);
    }

    public TransportException(String str) {
        super(str);
    }
}
