package commodity.trading.controller;

import commodity.trading.entity.Role;
import commodity.trading.entity.User;
import commodity.trading.repository.RoleRepository;
import commodity.trading.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User regularUser;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        userRole = new Role();
        userRole.setName("USER");
        roleRepository.save(userRole);

        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("password"));
        adminUser.setEnabled(true);
        adminUser.setRoles(Set.of(adminRole));
        userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setPassword(passwordEncoder.encode("password"));
        regularUser.setEnabled(true);
        regularUser.setRoles(Set.of(userRole));
        userRepository.save(regularUser);
    }

    @Test
    void deleteUser_WithAdminRole_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", regularUser.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", regularUser.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_NotFound_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 999L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }
}
