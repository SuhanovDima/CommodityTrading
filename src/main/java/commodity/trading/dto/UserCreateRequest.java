package commodity.trading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 100) String password,
        Boolean enabled
) {
}
