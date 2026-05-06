package commodity.trading.controller;

import commodity.trading.entity.Role;
import commodity.trading.entity.User;
import commodity.trading.repository.RoleRepository;
import commodity.trading.repository.UserRepository;
import commodity.trading.security.CustomUserDetails;
import commodity.trading.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.liquibase.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole = roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setName("USER");
        userRole = roleRepository.save(userRole);

        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("password"));
        adminUser.setEnabled(true);
        adminUser.setRoles(Set.of(adminRole));
        adminUser = userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setPassword(passwordEncoder.encode("password"));
        regularUser.setEnabled(true);
        regularUser.setRoles(Set.of(userRole));
        regularUser = userRepository.save(regularUser);
    }

    @Test
    void deleteUser_WithAdminRole_ShouldReturnNoContent() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(
                new CustomUserDetails(adminUser)
        );
        
        mockMvc.perform(delete("/api/users/{id}", regularUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(
                new CustomUserDetails(regularUser)
        );
        
        mockMvc.perform(delete("/api/users/{id}", regularUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_NotFound_ShouldReturnNotFound() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(
                new CustomUserDetails(adminUser)
        );
        
        mockMvc.perform(delete("/api/users/{id}", 999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
