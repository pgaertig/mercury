package pl.amitec.mercury.server.webapp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.amitec.mercury.Plan;
import pl.amitec.mercury.server.IntegratorRepository;
import pl.amitec.mercury.server.PlanLoader;

import java.util.List;

@Controller
public class AdminController {

    private final PlanLoader planLoader;
    private final IntegratorRepository integrators;

    public AdminController(PlanLoader planLoader, IntegratorRepository integrators) {
        this.planLoader = planLoader;
        this.integrators = integrators;
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        List<Plan> plans = planLoader.getAllPlans();
        model.addAttribute("plans", plans);
        model.addAttribute("integrators", integrators.getAllNames());
        return "admin/index";
    }
}