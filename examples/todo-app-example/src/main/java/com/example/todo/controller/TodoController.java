package com.example.todo.controller;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.TodoDto;
import com.example.todo.dto.UpdateTodoRequest;
import com.example.todo.entity.Todo;
import com.example.todo.entity.User;
import com.example.todo.service.TodoService;
import com.example.todo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/todos")
public class TodoController {

    private final TodoService todoService;
    private final UserService userService;

    public TodoController(TodoService todoService, UserService userService) {
        this.todoService = todoService;
        this.userService = userService;
    }

    @GetMapping
    public String listTodos(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getCurrentUser(userDetails);
        List<TodoDto> todos = todoService.findAllByUser(user);

        model.addAttribute("todos", todos);
        model.addAttribute("totalCount", todos.size());
        model.addAttribute("completedCount", todos.stream().filter(t -> t.status() == Todo.Status.COMPLETED).count());
        model.addAttribute("pendingCount", todos.stream().filter(t -> t.status() == Todo.Status.PENDING).count());
        model.addAttribute("overdueCount", todos.stream().filter(TodoDto::overdue).count());
        model.addAttribute("createTodoRequest", new CreateTodoRequest("", "", Todo.Priority.MEDIUM, null));
        model.addAttribute("priorities", Todo.Priority.values());
        model.addAttribute("statuses", Todo.Status.values());
        model.addAttribute("displayName", user.getDisplayName());

        return "todo/list";
    }

    @PostMapping
    public String createTodo(
            @Valid @ModelAttribute CreateTodoRequest createTodoRequest,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fix the errors and try again");
            return "redirect:/todos";
        }

        User user = getCurrentUser(userDetails);
        todoService.createTodo(createTodoRequest, user);
        redirectAttributes.addFlashAttribute("success", "Todo created successfully!");

        return "redirect:/todos";
    }

    @GetMapping("/{id}")
    public String viewTodo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        User user = getCurrentUser(userDetails);
        TodoDto todo = todoService.findByIdAndUser(id, user);

        model.addAttribute("todo", todo);
        model.addAttribute("priorities", Todo.Priority.values());
        model.addAttribute("statuses", Todo.Status.values());

        return "todo/view";
    }

    @GetMapping("/{id}/edit")
    public String editTodoForm(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        User user = getCurrentUser(userDetails);
        TodoDto todo = todoService.findByIdAndUser(id, user);

        model.addAttribute("todo", todo);
        model.addAttribute("updateTodoRequest", new UpdateTodoRequest(
            todo.title(), todo.description(), todo.priority(), todo.status(), todo.dueDate()
        ));
        model.addAttribute("priorities", Todo.Priority.values());
        model.addAttribute("statuses", Todo.Status.values());

        return "todo/edit";
    }

    @PostMapping("/{id}")
    public String updateTodo(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateTodoRequest updateTodoRequest,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fix the errors and try again");
            return "redirect:/todos/" + id + "/edit";
        }

        User user = getCurrentUser(userDetails);
        todoService.updateTodo(id, updateTodoRequest, user);
        redirectAttributes.addFlashAttribute("success", "Todo updated successfully!");

        return "redirect:/todos";
    }

    @PostMapping("/{id}/toggle")
    public String toggleTodo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User user = getCurrentUser(userDetails);
        Todo todo = todoService.toggleComplete(id, user);

        String message = todo.getStatus() == Todo.Status.COMPLETED
            ? "Todo marked as completed!"
            : "Todo marked as pending!";
        redirectAttributes.addFlashAttribute("success", message);

        return "redirect:/todos";
    }

    @PostMapping("/{id}/delete")
    public String deleteTodo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User user = getCurrentUser(userDetails);
        todoService.deleteTodo(id, user);
        redirectAttributes.addFlashAttribute("success", "Todo deleted successfully!");

        return "redirect:/todos";
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userService.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
