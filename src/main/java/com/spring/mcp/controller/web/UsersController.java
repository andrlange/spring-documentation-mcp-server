package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.User;
import com.spring.mcp.model.enums.UserRole;
import com.spring.mcp.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for managing users.
 * Handles user management operations (Admin only).
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class UsersController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * List all users.
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "users/list"
     */
    @GetMapping
    public String listUsers(Model model) {
        log.debug("Listing all users");

        // Set active page for sidebar navigation
        model.addAttribute("activePage", "users");
        model.addAttribute("pageTitle", "Users");

        // Load all users sorted by username
        var users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "username"));
        model.addAttribute("users", users);
        model.addAttribute("totalElements", users.size());

        return "users/list";
    }

    /**
     * Show details of a specific user.
     *
     * @param id the user ID
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes for flash messages on redirect
     * @return view name "users/detail" or redirect to list
     */
    @GetMapping("/{id}")
    public String showUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing user with ID: {}", id);

        return userRepository.findById(id)
            .map(user -> {
                // Check if this is the last admin user
                boolean isLastAdmin = false;
                if (user.getRole() == UserRole.ADMIN) {
                    long adminCount = userRepository.countByRole(UserRole.ADMIN);
                    isLastAdmin = (adminCount == 1);
                }

                model.addAttribute("user", user);
                model.addAttribute("activePage", "users");
                model.addAttribute("pageTitle", user.getUsername());
                model.addAttribute("isLastAdmin", isLastAdmin);
                return "users/detail";
            })
            .orElseGet(() -> {
                log.warn("User not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/users";
            });
    }

    /**
     * Show form to create a new user.
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "users/form"
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.debug("Showing create user form");

        var user = User.builder()
            .enabled(true)
            .isActive(true)
            .role(UserRole.VIEWER)
            .build();

        model.addAttribute("user", user);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("pageTitle", "Create New User");
        model.addAttribute("activePage", "users");
        model.addAttribute("isNewUser", true);

        return "users/form";
    }

    /**
     * Create a new user.
     *
     * @param user the user to create
     * @param bindingResult validation result
     * @param plainPassword the plain text password
     * @param redirectAttributes for flash messages
     * @param model Spring MVC model
     * @return redirect to user detail or form on error
     */
    @PostMapping
    public String createUser(
            @Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            @RequestParam("plainPassword") String plainPassword,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.debug("Creating new user: {}", user.getUsername());

        // Check for username uniqueness
        if (userRepository.existsByUsername(user.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "Username already exists");
        }

        // Check for email uniqueness if email is provided
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            if (userRepository.existsByEmail(user.getEmail())) {
                bindingResult.rejectValue("email", "duplicate", "Email already exists");
            }
        }

        // Validate password
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            bindingResult.reject("password.empty", "Password is required");
        } else if (plainPassword.length() < 8) {
            bindingResult.reject("password.length", "Password must be at least 8 characters");
        }

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors creating user: {}", bindingResult.getAllErrors());
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("pageTitle", "Create New User");
            model.addAttribute("activePage", "users");
            model.addAttribute("isNewUser", true);
            return "users/form";
        }

        try {
            // Encode password
            user.setPassword(passwordEncoder.encode(plainPassword));

            User savedUser = userRepository.save(user);
            log.info("User created successfully with ID: {}", savedUser.getId());
            redirectAttributes.addFlashAttribute("success", "User created successfully");
            return "redirect:/users/" + savedUser.getId();
        } catch (Exception e) {
            log.error("Error creating user", e);
            model.addAttribute("error", "Failed to create user: " + e.getMessage());
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("pageTitle", "Create New User");
            model.addAttribute("activePage", "users");
            model.addAttribute("isNewUser", true);
            return "users/form";
        }
    }

    /**
     * Show form to edit an existing user.
     *
     * @param id the user ID
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes for flash messages on redirect
     * @return view name "users/form" or redirect to list
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing edit form for user with ID: {}", id);

        return userRepository.findById(id)
            .map(user -> {
                model.addAttribute("user", user);
                model.addAttribute("roles", UserRole.values());
                model.addAttribute("pageTitle", "Edit " + user.getUsername());
                model.addAttribute("activePage", "users");
                model.addAttribute("isNewUser", false);
                return "users/form";
            })
            .orElseGet(() -> {
                log.warn("User not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/users";
            });
    }

    /**
     * Update an existing user.
     *
     * @param id the user ID
     * @param user the updated user data
     * @param bindingResult validation result
     * @param plainPassword optional new password
     * @param redirectAttributes for flash messages
     * @param model Spring MVC model
     * @return redirect to user detail or form on error
     */
    @PostMapping("/{id}")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            @RequestParam(value = "plainPassword", required = false) String plainPassword,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.debug("Updating user with ID: {}", id);

        return userRepository.findById(id)
            .map(existingUser -> {
                // Check for username uniqueness (excluding current user)
                if (!existingUser.getUsername().equals(user.getUsername()) &&
                    userRepository.existsByUsername(user.getUsername())) {
                    bindingResult.rejectValue("username", "duplicate", "Username already exists");
                }

                // Check for email uniqueness (excluding current user)
                if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                    if ((existingUser.getEmail() == null || !existingUser.getEmail().equals(user.getEmail())) &&
                        userRepository.existsByEmail(user.getEmail())) {
                        bindingResult.rejectValue("email", "duplicate", "Email already exists");
                    }
                }

                // Validate new password if provided
                if (plainPassword != null && !plainPassword.trim().isEmpty()) {
                    if (plainPassword.length() < 8) {
                        bindingResult.reject("password.length", "Password must be at least 8 characters");
                    }
                }

                if (bindingResult.hasErrors()) {
                    log.warn("Validation errors updating user: {}", bindingResult.getAllErrors());
                    model.addAttribute("user", user);
                    model.addAttribute("roles", UserRole.values());
                    model.addAttribute("pageTitle", "Edit User");
                    model.addAttribute("activePage", "users");
                    model.addAttribute("isNewUser", false);
                    return "users/form";
                }

                // Update fields
                existingUser.setUsername(user.getUsername());
                existingUser.setDisplayName(user.getDisplayName());
                existingUser.setEmail(user.getEmail());
                existingUser.setRole(user.getRole());
                existingUser.setEnabled(user.getEnabled());
                existingUser.setIsActive(user.getIsActive());

                // Update password only if a new one is provided
                if (plainPassword != null && !plainPassword.trim().isEmpty()) {
                    existingUser.setPassword(passwordEncoder.encode(plainPassword));
                }

                try {
                    User updatedUser = userRepository.save(existingUser);
                    log.info("User updated successfully with ID: {}", updatedUser.getId());
                    redirectAttributes.addFlashAttribute("success", "User updated successfully");
                    return "redirect:/users/" + updatedUser.getId();
                } catch (Exception e) {
                    log.error("Error updating user", e);
                    model.addAttribute("error", "Failed to update user: " + e.getMessage());
                    model.addAttribute("user", existingUser);
                    model.addAttribute("roles", UserRole.values());
                    model.addAttribute("pageTitle", "Edit User");
                    model.addAttribute("activePage", "users");
                    model.addAttribute("isNewUser", false);
                    return "users/form";
                }
            })
            .orElseGet(() -> {
                log.warn("User not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/users";
            });
    }

    /**
     * Delete a user.
     *
     * @param id the user ID
     * @param redirectAttributes for flash messages
     * @return redirect to users list
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.debug("Deleting user with ID: {}", id);

        // Get current logged-in username
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findById(id)
            .map(user -> {
                // Safety check: prevent deleting the last admin
                if (user.getRole() == UserRole.ADMIN) {
                    long adminCount = userRepository.countByRole(UserRole.ADMIN);
                    if (adminCount == 1) {
                        log.warn("Attempted to delete the last ADMIN user: {}", user.getUsername());
                        redirectAttributes.addFlashAttribute("error",
                            "Cannot delete the last ADMIN user. At least one ADMIN must exist to manage the system.");
                        return "redirect:/users/" + id;
                    }
                }

                // Check if user is deleting their own account
                boolean deletingSelf = user.getUsername().equals(currentUsername);

                try {
                    String username = user.getUsername();
                    userRepository.delete(user);
                    log.info("User deleted successfully: {}", username);

                    // If user deleted their own account, invalidate session and redirect to login
                    if (deletingSelf) {
                        log.info("User deleted their own account, logging out: {}", username);
                        request.getSession().invalidate();
                        SecurityContextHolder.clearContext();
                        redirectAttributes.addFlashAttribute("success",
                            "Your account has been deleted successfully");
                        return "redirect:/login?logout";
                    }

                    redirectAttributes.addFlashAttribute("success",
                        "User '" + username + "' deleted successfully");
                } catch (Exception e) {
                    log.error("Error deleting user with ID: {}", id, e);
                    redirectAttributes.addFlashAttribute("error",
                        "Failed to delete user: " + e.getMessage());
                }
                return "redirect:/users";
            })
            .orElseGet(() -> {
                log.warn("User not found with ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/users";
            });
    }
}
