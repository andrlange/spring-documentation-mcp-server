package com.example.todo.service;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.TodoDto;
import com.example.todo.dto.TodoStats;
import com.example.todo.dto.UpdateTodoRequest;
import com.example.todo.entity.Todo;
import com.example.todo.entity.User;
import com.example.todo.repository.TodoRepository;
import com.example.todo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    public TodoService(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    public Todo createTodo(CreateTodoRequest request, User user) {
        Todo todo = new Todo(request.title(), user);
        todo.setDescription(request.description());
        todo.setPriority(request.priority() != null ? request.priority() : Todo.Priority.MEDIUM);
        todo.setDueDate(request.dueDate());
        return todoRepository.save(todo);
    }

    public Todo updateTodo(Long todoId, UpdateTodoRequest request, User user) {
        Todo todo = todoRepository.findByIdAndUser(todoId, user)
            .orElseThrow(() -> new IllegalArgumentException("Todo not found"));

        todo.setTitle(request.title());
        todo.setDescription(request.description());
        todo.setPriority(request.priority());
        todo.setDueDate(request.dueDate());

        if (request.status() == Todo.Status.COMPLETED && todo.getStatus() != Todo.Status.COMPLETED) {
            todo.setCompletedAt(LocalDateTime.now());
        }
        todo.setStatus(request.status());

        return todoRepository.save(todo);
    }

    public void deleteTodo(Long todoId, User user) {
        Todo todo = todoRepository.findByIdAndUser(todoId, user)
            .orElseThrow(() -> new IllegalArgumentException("Todo not found"));
        todoRepository.delete(todo);
    }

    public Todo toggleComplete(Long todoId, User user) {
        Todo todo = todoRepository.findByIdAndUser(todoId, user)
            .orElseThrow(() -> new IllegalArgumentException("Todo not found"));

        if (todo.getStatus() == Todo.Status.COMPLETED) {
            todo.setStatus(Todo.Status.PENDING);
            todo.setCompletedAt(null);
        } else {
            todo.setStatus(Todo.Status.COMPLETED);
            todo.setCompletedAt(LocalDateTime.now());
        }

        return todoRepository.save(todo);
    }

    @Transactional(readOnly = true)
    public List<TodoDto> findAllByUser(User user) {
        return todoRepository.findByUserSorted(user).stream()
            .map(TodoDto::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public TodoDto findByIdAndUser(Long id, User user) {
        return todoRepository.findByIdAndUser(id, user)
            .map(TodoDto::fromEntity)
            .orElseThrow(() -> new IllegalArgumentException("Todo not found"));
    }

    @Transactional(readOnly = true)
    public TodoStats getStats() {
        LocalDate today = LocalDate.now();
        return new TodoStats(
            todoRepository.count(),
            todoRepository.countByStatus(Todo.Status.PENDING),
            todoRepository.countByStatus(Todo.Status.IN_PROGRESS),
            todoRepository.countByStatus(Todo.Status.COMPLETED),
            todoRepository.countByStatus(Todo.Status.CANCELLED),
            todoRepository.countOverdue(today),
            todoRepository.countDueToday(today),
            userRepository.count(),
            userRepository.countActiveUsers()
        );
    }

    @Transactional(readOnly = true)
    public long countByUser(User user) {
        return todoRepository.countByUser(user);
    }

    @Transactional(readOnly = true)
    public long countCompletedByUser(User user) {
        return todoRepository.countByUserAndStatus(user, Todo.Status.COMPLETED);
    }
}
