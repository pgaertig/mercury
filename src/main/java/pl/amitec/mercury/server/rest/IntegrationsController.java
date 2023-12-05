package pl.amitec.mercury.server.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.amitec.mercury.server.IntegratorDiscovery;

import java.util.List;

@RestController
@RequestMapping("/integrators")
public class IntegrationsController {

    private IntegratorDiscovery integratorDiscovery;
    public IntegrationsController(
            IntegratorDiscovery integratorDiscovery) {
        this.integratorDiscovery = integratorDiscovery;
    }

    @GetMapping
    public List<String> list() {
        return integratorDiscovery.getAllNames();
    }
}
