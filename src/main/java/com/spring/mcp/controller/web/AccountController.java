package com.spring.mcp.controller.web;

import com.spring.mcp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for user account operations.
 * Handles operations that authenticated users can perform on their own account.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-11-30
 */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Change the password for the currently authenticated user.
     *
     * @param currentPassword the user's current password
     * @param newPassword the new password
     * @param confirmPassword confirmation of the new password
     * @return JSON response indicating success or failure
     */
    @PostMapping("/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword) {

        log.debug("Processing password change request");

        // Get current authenticated username
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // Validate new passwords match
        if (!newPassword.equals(confirmPassword)) {
            log.warn("Password change failed for {}: passwords do not match", username);
            return ResponseEntity.ok(createResponse(false, "New passwords do not match."));
        }

        // Validate password length
        if (newPassword.length() < 8) {
            log.warn("Password change failed for {}: password too short", username);
            return ResponseEntity.ok(createResponse(false, "New password must be at least 8 characters long."));
        }

        // Find the user
        return userRepository.findByUsername(username)
            .map(user -> {
                // Verify current password
                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    log.warn("Password change failed for {}: incorrect current password", username);
                    return ResponseEntity.ok(createResponse(false, "Current password is incorrect."));
                }

                // Check if new password is different from current
                if (passwordEncoder.matches(newPassword, user.getPassword())) {
                    log.warn("Password change failed for {}: new password same as old", username);
                    return ResponseEntity.ok(createResponse(false, "New password must be different from current password."));
                }

                try {
                    // Update password
                    user.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(user);
                    log.info("Password changed successfully for user: {}", username);

                    return ResponseEntity.ok(createResponse(true, "Password changed successfully!"));
                } catch (Exception e) {
                    log.error("Error changing password for user: {}", username, e);
                    return ResponseEntity.ok(createResponse(false, "An error occurred while changing password. Please try again."));
                }
            })
            .orElseGet(() -> {
                log.error("User not found during password change: {}", username);
                return ResponseEntity.ok(createResponse(false, "User not found. Please log in again."));
            });
    }

    /**
     * Helper method to create a response map.
     */
    private Map<String, Object> createResponse(boolean success, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        return response;
    }
}
