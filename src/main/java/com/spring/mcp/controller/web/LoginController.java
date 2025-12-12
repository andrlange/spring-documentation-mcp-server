package com.spring.mcp.controller.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the login page.
 * Needed as a proper controller (not view controller) to ensure
 * model attributes like appVersion are properly resolved.
 */
@Controller
public class LoginController {

    @Value("${info.app.version:unknown}")
    private String appVersion;

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("appVersion", appVersion);
        return "login";
    }
}
