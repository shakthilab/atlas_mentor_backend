package com.lab.atlasmentor.service;

import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoleCacheService {

    @Autowired
    private RoleRepository roleRepository;

    private Map<Long, Role> roleCache = new ConcurrentHashMap<>();
    private Map<String, Role> roleByNameCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeCache() {
        refreshCache();
    }

    public void refreshCache() {
        roleCache.clear();
        roleByNameCache.clear();
        
        roleRepository.findAll().forEach(role -> {
            roleCache.put(role.getId(), role);
            roleByNameCache.put(role.getName(), role);
        });
    }

    public Role getRoleById(Long roleId) {
        Role role = roleCache.get(roleId);
        if (role == null) {
            // Fallback to database if not in cache
            role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));
            roleCache.put(roleId, role);
        }
        return role;
    }

    public Role getRoleByName(String roleName) {
        Role role = roleByNameCache.get(roleName);
        if (role == null) {
            // Fallback to database if not in cache
            role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            roleByNameCache.put(roleName, role);
            roleCache.put(role.getId(), role);
        }
        return role;
    }

    public void clearCache() {
        roleCache.clear();
        roleByNameCache.clear();
    }

    public Map<Long, Role> getAllRolesCache() {
        return new ConcurrentHashMap<>(roleCache);
    }
}
