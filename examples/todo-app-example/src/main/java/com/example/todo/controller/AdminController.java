package com.example.todo.controller;

import com.example.todo.dto.TodoStats;
import com.example.todo.dto.UserDto;
import com.example.todo.service.TodoService;
import com.example.todo.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TodoService todoService;
    private final UserService userService;

    public AdminController(TodoService todoService, UserService userService) {
        this.todoService = todoService;
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(Model model) {
        TodoStats stats = todoService.getStats();
        List<UserDto> users = userService.findAllUsers();

        model.addAttribute("stats", stats);
        model.addAttribute("users", users);

        return "admin/dashboard";
    }
}
