package pl.amitec.mercury.integrators.dynamics;

import lombok.Data;
import pl.amitec.mercury.Configurable;

@Data
public class Config implements Configurable{
    BitbeeConfig bitbee;
    DynamicsConfig dynamics;
}
