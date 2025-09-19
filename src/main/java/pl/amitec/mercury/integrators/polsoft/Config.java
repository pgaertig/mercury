package pl.amitec.mercury.integrators.polsoft;

import lombok.Data;
import pl.amitec.mercury.Configurable;
import pl.amitec.mercury.clients.bitbee.BitbeeConfig;

@Data
public class Config implements Configurable {
    String name;
    BitbeeConfig bitbee;
    PolsoftConfig polsoft;
}
