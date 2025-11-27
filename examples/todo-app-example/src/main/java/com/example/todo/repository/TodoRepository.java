package com.example.todo.repository;

import com.example.todo.entity.Todo;
import com.example.todo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByUserOrderByCreatedAtDesc(User user);

    List<Todo> findByUserAndStatusOrderByDueDateAsc(User user, Todo.Status status);

    Optional<Todo> findByIdAndUser(Long id, User user);

    @Query("SELECT t FROM Todo t WHERE t.user = :user ORDER BY " +
           "CASE WHEN t.status = 'COMPLETED' THEN 1 WHEN t.status = 'CANCELLED' THEN 2 ELSE 0 END, " +
           "CASE t.priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, " +
           "t.dueDate NULLS LAST")
    List<Todo> findByUserSorted(@Param("user") User user);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.status = :status")
    long countByStatus(@Param("status") Todo.Status status);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.dueDate < :today AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
    long countOverdue(@Param("today") LocalDate today);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.dueDate = :today AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
    long countDueToday(@Param("today") LocalDate today);

    long countByUser(User user);

    long countByUserAndStatus(User user, Todo.Status status);
}
