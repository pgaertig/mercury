package pl.amitec.mercury.integrators.polsoft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.transport.Transport;

import java.io.IOException;

public interface PsCommonSync {

    Logger LOG = LoggerFactory.getLogger(PsCommonSync.class);

    default boolean isComplete(Transport transport) throws IOException {
        if (transport.exists("toppc.txt") && "KONIEC".equals(transport.read("toppc.txt"))) {
            LOG.debug("Data available");
            return true;
        } else {
            LOG.info("No data available " + transport);
            return false;
        }
    }
}
