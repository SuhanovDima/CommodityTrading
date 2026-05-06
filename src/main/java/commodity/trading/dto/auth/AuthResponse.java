package commodity.trading.dto.auth;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        String username,
        List<String> roles
) {
    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn, String username, List<String> roles) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, username, roles);
    }
}
