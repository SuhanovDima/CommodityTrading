package commodity.trading.service.auth;

import commodity.trading.dto.auth.AuthResponse;
import commodity.trading.dto.auth.LoginRequest;
import commodity.trading.dto.auth.RefreshTokenRequest;
import commodity.trading.entity.JwtToken;
import commodity.trading.exception.NotFoundException;
import commodity.trading.repository.JwtTokenRepository;
import commodity.trading.security.CustomUserDetails;
import commodity.trading.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenRepository jwtTokenRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            JwtTokenRepository jwtTokenRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtTokenRepository = jwtTokenRepository;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        Instant now = Instant.now();
        Instant accessExpiry = now.plusMillis(jwtTokenProvider.getAccessTokenValidityMs());
        Instant refreshExpiry = now.plusMillis(jwtTokenProvider.getRefreshTokenValidityMs());

        JwtToken accessJwtToken = new JwtToken();
        accessJwtToken.setUser(userDetails.getUser());
        accessJwtToken.setToken(accessToken);
        accessJwtToken.setIssuedAt(now);
        accessJwtToken.setExpiresAt(accessExpiry);
        accessJwtToken.setRevoked(false);
        jwtTokenRepository.save(accessJwtToken);

        JwtToken refreshJwtToken = new JwtToken();
        refreshJwtToken.setUser(userDetails.getUser());
        refreshJwtToken.setToken(refreshToken);
        refreshJwtToken.setIssuedAt(now);
        refreshJwtToken.setExpiresAt(refreshExpiry);
        refreshJwtToken.setRevoked(false);
        jwtTokenRepository.save(refreshJwtToken);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        return AuthResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessTokenValidityMs() / 1000, userDetails.getUsername(), roles);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        JwtToken storedToken = jwtTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new NotFoundException("Invalid refresh token"));

        if (storedToken.getRevoked()) {
            throw new NotFoundException("Token has been revoked");
        }

        if (jwtTokenProvider.isTokenExpired(storedToken.getToken())) {
            jwtTokenRepository.revokeByToken(storedToken.getToken());
            throw new NotFoundException("Refresh token has expired");
        }

        CustomUserDetails userDetails = new CustomUserDetails(storedToken.getUser());

        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        Instant now = Instant.now();
        Instant accessExpiry = now.plusMillis(jwtTokenProvider.getAccessTokenValidityMs());
        Instant refreshExpiry = now.plusMillis(jwtTokenProvider.getRefreshTokenValidityMs());

        storedToken.setRevoked(true);
        jwtTokenRepository.save(storedToken);

        JwtToken newAccessJwtToken = new JwtToken();
        newAccessJwtToken.setUser(userDetails.getUser());
        newAccessJwtToken.setToken(newAccessToken);
        newAccessJwtToken.setIssuedAt(now);
        newAccessJwtToken.setExpiresAt(accessExpiry);
        newAccessJwtToken.setRevoked(false);
        jwtTokenRepository.save(newAccessJwtToken);

        JwtToken newRefreshJwtToken = new JwtToken();
        newRefreshJwtToken.setUser(userDetails.getUser());
        newRefreshJwtToken.setToken(newRefreshToken);
        newRefreshJwtToken.setIssuedAt(now);
        newRefreshJwtToken.setExpiresAt(refreshExpiry);
        newRefreshJwtToken.setRevoked(false);
        jwtTokenRepository.save(newRefreshJwtToken);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        return AuthResponse.of(newAccessToken, newRefreshToken, jwtTokenProvider.getAccessTokenValidityMs() / 1000, userDetails.getUsername(), roles);
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        jwtTokenRepository.deleteByToken(token);
    }
}
