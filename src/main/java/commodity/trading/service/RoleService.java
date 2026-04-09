package commodity.trading.service;

import commodity.trading.dto.RoleCreateRequest;
import commodity.trading.dto.RoleResponse;
import commodity.trading.entity.Role;
import commodity.trading.exception.ConflictException;
import commodity.trading.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new ConflictException("Role with name '%s' already exists".formatted(request.name()));
        }

        Role role = new Role();
        role.setName(request.name());
        return toResponse(roleRepository.save(role));
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getName());
    }
}
