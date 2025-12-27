package com.example.library.controller;

import com.example.library.model.BookView;
import com.example.library.model.Category;
import com.example.library.model.CategoryView;
import com.example.library.model.Loan;
import com.example.library.model.MemberView;
import com.example.library.service.BookService;
import com.example.library.service.CategoryService;
import com.example.library.service.LoanService;
import com.example.library.service.MemberService;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Web controller for Thymeleaf UI pages.
 * Provides dashboard, status, simulation, and data management views.
 */
@Controller
@Observed(name = "web.controller")
public class WebController {

    private final BookService bookService;
    private final MemberService memberService;
    private final LoanService loanService;
    private final CategoryService categoryService;

    public WebController(BookService bookService, MemberService memberService,
                         LoanService loanService, CategoryService categoryService) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("stats", getStats());

        // Get recent loans (last 5)
        var recentLoans = loanService.findActive().stream()
                .sorted(Comparator.comparing(Loan::loanDate).reversed())
                .limit(5)
                .toList();
        model.addAttribute("recentLoans", recentLoans);

        return "dashboard";
    }

    @GetMapping("/simulation")
    public String simulation(Model model) {
        model.addAttribute("currentPage", "simulation");
        return "simulation";
    }

    @GetMapping("/status")
    public String status(Model model) {
        model.addAttribute("currentPage", "status");
        model.addAttribute("stats", getStats());
        return "status";
    }

    @GetMapping("/books")
    public String books(Model model) {
        model.addAttribute("currentPage", "books");

        // Create category lookup map
        var categoryMap = categoryService.findAll().stream()
                .collect(Collectors.toMap(Category::id, Category::name));

        // Convert books to BookView with category names
        var bookViews = bookService.findAll().stream()
                .map(book -> BookView.from(book, categoryMap.getOrDefault(book.categoryId(), "Unknown")))
                .toList();

        model.addAttribute("books", bookViews);
        model.addAttribute("categories", categoryService.findAll());
        return "books";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("currentPage", "categories");

        // Count books per category
        var bookCountByCategory = bookService.findAll().stream()
                .collect(Collectors.groupingBy(
                        book -> book.categoryId(),
                        Collectors.counting()
                ));

        // Convert categories to CategoryView with book counts
        var categoryViews = categoryService.findAll().stream()
                .map(cat -> CategoryView.from(cat, bookCountByCategory.getOrDefault(cat.id(), 0L).intValue()))
                .toList();

        model.addAttribute("categories", categoryViews);
        return "categories";
    }

    @GetMapping("/members")
    public String members(Model model) {
        model.addAttribute("currentPage", "members");

        // Count active loans per member
        var loanCountByMember = loanService.findActive().stream()
                .collect(Collectors.groupingBy(
                        Loan::memberId,
                        Collectors.counting()
                ));

        // Convert members to MemberView with current loan counts
        var memberViews = memberService.findAll().stream()
                .map(member -> MemberView.from(member, loanCountByMember.getOrDefault(member.id(), 0L).intValue()))
                .toList();

        model.addAttribute("members", memberViews);
        return "members";
    }

    @GetMapping("/loans")
    public String loans(Model model) {
        model.addAttribute("currentPage", "loans");
        model.addAttribute("loans", loanService.findAll());
        return "loans";
    }

    @GetMapping("/loans/overdue")
    public String overdueLoans(Model model) {
        model.addAttribute("currentPage", "overdue");
        model.addAttribute("loans", loanService.findOverdue());
        return "loans";
    }

    private Map<String, Object> getStats() {
        return Map.of(
                "totalBooks", bookService.count(),
                "availableBooks", bookService.countAvailable(),
                "totalCategories", categoryService.count(),
                "totalMembers", memberService.count(),
                "activeMembers", memberService.countActive(),
                "totalLoans", loanService.count(),
                "activeLoans", loanService.countActive(),
                "overdueLoans", loanService.countOverdue()
        );
    }
}
