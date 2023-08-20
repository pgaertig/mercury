package pl.amitec.mercury.providers.polsoft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.amitec.mercury.transport.Transport;

import java.io.IOException;

public interface PsCommonSync {

    Logger LOG = LogManager.getLogger(PsCommonSync.class);

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
