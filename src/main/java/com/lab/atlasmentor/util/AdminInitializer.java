package com.lab.atlasmentor.util;

import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Check if admin user already exists
        if (!userRepository.existsByEmail("admin@yopmail.com")) {
            User admin = new User();
            admin.setFirstName("Jitesh");
            admin.setLastName("Kumar");
            admin.setEmail("admin@yopmail.com");
            admin.setPhone("0000000000");
//            admin.setEmail("Jitesh.gupta@atlasmentor.com");
            admin.setEmail("admin@yopmail.com");

            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setIsVerified(true);
            admin.setPhone("9876543210");

            // Get or create ADMIN role
            Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role("ADMIN", "System Administrator", true);
                    role.setDisplayName("Admin");
                    return roleRepository.save(role);
                });
            admin.setRole(adminRole);
            
            userRepository.save(admin);
            
            System.out.println("=================================");
            System.out.println("ADMIN USER CREATED SUCCESSFULLY");
            System.out.println("Email: Jitesh.gupta@atlasmentor.com");
            System.out.println("Password: admin123");
            System.out.println("Please change this password after first login!");
            System.out.println("=================================");
        }
    }
}
