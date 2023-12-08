package pl.amitec.mercury.integrators.dynamics;

import lombok.Data;
import pl.amitec.mercury.Configurable;
import pl.amitec.mercury.clients.bitbee.BitbeeConfig;

@Data
public class Config implements Configurable{
    BitbeeConfig bitbee;
    DynamicsConfig dynamics;
}
