package commodity.trading.controller.ui;

import commodity.trading.service.auth.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
@RequestMapping("/ui")
public class AuthUiController {

    private final AuthService authService;

    public AuthUiController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "ui/login";
    }

    @GetMapping("/logout")
    public String logoutGet(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        authService.logout(extractBearerToken(authorizationHeader));
        return "redirect:/ui/login";
    }

    @PostMapping("/logout")
    public String logoutPost(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        authService.logout(extractBearerToken(authorizationHeader));
        return "redirect:/ui/login";
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
