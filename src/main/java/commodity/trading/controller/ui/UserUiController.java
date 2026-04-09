package commodity.trading.controller.ui;

import commodity.trading.dto.UserCreateRequest;
import commodity.trading.exception.ConflictException;
import commodity.trading.service.UserService;
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
@RequestMapping("/ui/users")
public class UserUiController {

    private final UserService userService;

    public UserUiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String usersPage(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", new UserCreateRequest("", "", Boolean.TRUE));
        }
        return "ui/users";
    }

    @PostMapping
    public String createUser(
            @Valid @ModelAttribute("userForm") UserCreateRequest userForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userService.getAllUsers());
            return "ui/users";
        }

        try {
            userService.createUser(userForm);
            redirectAttributes.addFlashAttribute("successMessage", "User created");
            return "redirect:/ui/users";
        } catch (ConflictException ex) {
            bindingResult.reject("conflict", ex.getMessage());
            model.addAttribute("users", userService.getAllUsers());
            return "ui/users";
        }
    }
}
