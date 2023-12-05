package pl.amitec.mercury.server.rest;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.amitec.mercury.Plan;
import pl.amitec.mercury.server.PlanLoader;

import java.util.List;

@RestController
@RequestMapping("/plans")
public class PlansController {
    private PlanLoader planLoader;

    public PlansController(PlanLoader planLoader) {
        this.planLoader = planLoader;
    }

    @GetMapping
    public List<Plan> list() {
        return planLoader.getAllPlans();
    }
}
