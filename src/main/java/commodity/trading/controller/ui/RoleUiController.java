package commodity.trading.controller.ui;

import commodity.trading.dto.RoleCreateRequest;
import commodity.trading.exception.ConflictException;
import commodity.trading.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ui/roles")
public class RoleUiController {

    private final RoleService roleService;

    public RoleUiController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public String rolesPage(Model model) {
        model.addAttribute("roles", roleService.getAllRoles());
        if (!model.containsAttribute("roleForm")) {
            model.addAttribute("roleForm", new RoleCreateRequest(""));
        }
        return "ui/roles";
    }

    @PostMapping
    public String createRole(
            @Valid @ModelAttribute("roleForm") RoleCreateRequest roleForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", roleService.getAllRoles());
            return "ui/roles";
        }

        try {
            roleService.createRole(roleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Role created");
            return "redirect:/ui/roles";
        } catch (ConflictException ex) {
            bindingResult.reject("conflict", ex.getMessage());
            model.addAttribute("roles", roleService.getAllRoles());
            return "ui/roles";
        }
    }
}
