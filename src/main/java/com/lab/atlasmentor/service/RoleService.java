package com.lab.atlasmentor.service;

import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.RoleRepository;
import com.lab.atlasmentor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    public Role findOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role(roleName);
                    return roleRepository.save(newRole);
                });
    }

    public Role findRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
    }

    public Role findRoleById(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));
    }

    @Transactional
    public void assignRoleToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = findRole(roleName);
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void assignRolesToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Assign single role (take first role from set for backward compatibility)
        Role role = findRole(roleName);
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void assignRoleToUserById(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = findRoleById(roleId);
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void removeRoleFromUser(Long userId, String roleName) {
        // This method doesn't make sense with single role - user must have a role
        // For now, we'll set role to null, but this should be handled by business logic
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() != null && user.getRole().getName().equals(roleName)) {
            user.setRole(null);
            userRepository.save(user);
        }
    }

    public String getUserRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getRole() != null ? user.getRole().getName() : null;
    }

    public boolean hasRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.hasRole(roleName);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public void initializeDefaultRoles() {
        String[] defaultRoles = { "ADMIN", "MANAGER", "VIDEO_EDITOR", "COUNSELLOR", "REFERRAL", "COMPANY", "STUDENT" };
        for (String roleName : defaultRoles) {
            findOrCreateRole(roleName);
        }
    }
}
