package commodity.trading.controller.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui")
public class AuthUiController {

    @GetMapping("/login")
    public String loginPage() {
        return "ui/login";
    }
}
