package commodity.trading.service;

import commodity.trading.dto.RoleResponse;
import commodity.trading.dto.UserCreateRequest;
import commodity.trading.dto.UserResponse;
import commodity.trading.entity.Role;
import commodity.trading.entity.User;
import commodity.trading.exception.ConflictException;
import commodity.trading.exception.NotFoundException;
import commodity.trading.repository.RoleRepository;
import commodity.trading.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("User with username '%s' already exists".formatted(request.username()));
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setEnabled(request.enabled() != null ? request.enabled() : Boolean.TRUE);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse assignRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id '%d' not found".formatted(userId)));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role with id '%d' not found".formatted(roleId)));

        if (!user.getRoles().add(role)) {
            throw new ConflictException("Role '%s' is already assigned to user '%s'".formatted(role.getName(), user.getUsername()));
        }

        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {
        Set<RoleResponse> roleResponses = user.getRoles().stream()
                .map(role -> new RoleResponse(role.getId(), role.getName()))
                .collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getUsername(), user.getEnabled(), roleResponses);
    }
}
