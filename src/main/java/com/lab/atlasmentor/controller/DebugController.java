package com.lab.atlasmentor.controller;

import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private TaskRepository taskRepository;

    @GetMapping("/tasks")
    public ResponseEntity<String> debugTasks() {
        try {
            StringBuilder result = new StringBuilder();
            
            result.append("=== DATABASE INVESTIGATION ===\n");
            
            // Get current user info
            var currentUser = SecurityUtils.getCurrentUser();
            result.append("Current User: ").append(currentUser.getUsername()).append("\n");
            result.append("Role: ").append(currentUser.getRole()).append("\n");
            result.append("Branch ID: ").append(currentUser.getBranchId()).append("\n");
            result.append("Is Admin: ").append(currentUser.isAdmin()).append("\n\n");
            
            // Get all tasks without filtering
            List<Task> allTasks = taskRepository.findAllActiveTasks();
            result.append("Total active tasks: ").append(allTasks.size()).append("\n");
            
            // Check task branch assignments
            result.append("\n=== TASK BRANCH ASSIGNMENTS ===\n");
            for (Task task : allTasks) {
                result.append("Task ID: ").append(task.getId())
                      .append(", Title: ").append(task.getTitle())
                      .append(", Branch ID: ").append(task.getBranch() != null ? task.getBranch().getId() : "null")
                      .append("\n");
            }
            
            // Test branch filtering for admin (isAdmin=true, branchId=null)
            result.append("\n=== ADMIN FILTERING TEST ===\n");
            List<Task> adminFilteredTasks = taskRepository.findAllWithAccess(true, null, null);
            result.append("Admin filtered tasks count: ").append(adminFilteredTasks.size()).append("\n");
            
            // Test branch filtering for manager (isAdmin=false, branchId=1)
            result.append("\n=== MANAGER FILTERING TEST ===\n");
            List<Task> managerFilteredTasks = taskRepository.findAllWithAccess(false, 1L, null);
            result.append("Manager filtered tasks count: ").append(managerFilteredTasks.size()).append("\n");
            
            // Test with current user's branch
            result.append("\n=== CURRENT USER FILTERING TEST ===\n");
            List<Task> currentUserFilteredTasks = taskRepository.findAllWithAccess(currentUser.isAdmin(), currentUser.getBranchId(), currentUser.getUserId());
            result.append("Current user filtered tasks count: ").append(currentUserFilteredTasks.size()).append("\n");
            
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.ok("Error: " + e.getMessage() + "\n" + e.toString());
        }
    }
}
