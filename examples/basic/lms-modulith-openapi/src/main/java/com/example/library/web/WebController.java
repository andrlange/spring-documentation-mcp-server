package com.example.library.web;

import com.example.library.catalog.api.BookService;
import com.example.library.loans.api.LoanService;
import com.example.library.members.api.MemberService;
import com.example.library.notifications.api.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Web Controller for Thymeleaf UI pages.
 * Provides a dark-themed management interface for the library.
 */
@Controller
public class WebController {

    private final BookService bookService;
    private final MemberService memberService;
    private final LoanService loanService;
    private final NotificationService notificationService;

    public WebController(BookService bookService,
                         MemberService memberService,
                         LoanService loanService,
                         NotificationService notificationService) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
        this.notificationService = notificationService;
    }

    // ==================== Dashboard ====================

    @GetMapping("/")
    public String dashboard(Model model) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBooks", bookService.countBooks());
        stats.put("availableBooks", bookService.countAvailableBooks());
        stats.put("totalMembers", memberService.countMembers());
        stats.put("activeMembers", memberService.countActiveMembers());
        stats.put("premiumMembers", memberService.countPremiumMembers());
        stats.put("activeLoans", loanService.countActiveLoans());
        stats.put("overdueLoans", loanService.countOverdueLoans());
        stats.put("totalUnpaidFines", loanService.getTotalUnpaidFines());
        stats.put("pendingNotifications", notificationService.countPending());
        stats.put("failedNotifications", notificationService.countFailed());

        model.addAttribute("stats", stats);
        model.addAttribute("recentLoans", loanService.findActiveLoans().stream()
            .limit(5)
            .toList());
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("pageTitle", "Dashboard - Library Management");

        return "dashboard";
    }

    // ==================== Books ====================

    @GetMapping("/books")
    public String listBooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String availability,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        var books = bookService.findAll();

        // Simple filtering (in production, use repository queries)
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            books = books.stream()
                .filter(b -> b.getTitle().toLowerCase().contains(searchLower) ||
                            b.getIsbn().toLowerCase().contains(searchLower) ||
                            b.getAuthor().getName().toLowerCase().contains(searchLower))
                .toList();
        }

        // Filter by category
        if (category != null) {
            books = books.stream()
                .filter(b -> b.getCategory() != null && b.getCategory().getId().equals(category))
                .toList();
        }

        if (availability != null) {
            books = switch (availability) {
                case "available" -> books.stream().filter(b -> b.isAvailable()).toList();
                case "loaned" -> books.stream().filter(b -> !b.isAvailable()).toList();
                default -> books;
            };
        }

        model.addAttribute("books", books);
        model.addAttribute("categories", bookService.findAllCategories());
        model.addAttribute("search", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("availability", availability);
        model.addAttribute("currentPage", "books");
        model.addAttribute("pageTitle", "Books - Library Management");
        model.addAttribute("totalPages", 1);

        return "books/list";
    }

    @GetMapping("/books/{isbn}")
    public String bookDetail(@PathVariable String isbn, Model model) {
        var book = bookService.findByIsbn(isbn)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));

        // Get loan information for this book
        var loans = loanService.findByBookIsbn(isbn);
        var activeLoan = loans.stream()
            .filter(l -> !l.isReturned())
            .findFirst()
            .orElse(null);

        model.addAttribute("book", book);
        model.addAttribute("activeLoan", activeLoan);
        model.addAttribute("loanHistory", loans);
        model.addAttribute("currentPage", "books");
        model.addAttribute("pageTitle", book.getTitle() + " - Library Management");

        return "books/detail";
    }

    @GetMapping("/books/{isbn}/edit")
    public String editBookForm(@PathVariable String isbn, Model model) {
        var book = bookService.findByIsbn(isbn)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));

        model.addAttribute("book", book);
        model.addAttribute("categories", bookService.findAllCategories());
        model.addAttribute("authors", bookService.findAllAuthors());
        model.addAttribute("currentPage", "books");
        model.addAttribute("pageTitle", "Edit " + book.getTitle() + " - Library Management");

        return "books/edit";
    }

    @PostMapping("/books/{isbn}/edit")
    public String updateBook(
            @PathVariable String isbn,
            @RequestParam String title,
            @RequestParam Long authorId,
            @RequestParam Long categoryId,
            @RequestParam int totalCopies,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            var book = bookService.findByIsbn(isbn)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));

            // Update book fields
            book.setTitle(title);
            book.setTotalCopies(totalCopies);
            book.setDescription(description);

            // Update author and category
            var author = bookService.findAuthorById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Author not found"));
            var category = bookService.findCategoryById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            book.setAuthor(author);
            book.setCategory(category);

            bookService.save(book);
            redirectAttributes.addFlashAttribute("success", "Book updated successfully!");
            return "redirect:/books/" + isbn;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update book: " + e.getMessage());
            return "redirect:/books/" + isbn + "/edit";
        }
    }

    // ==================== Members ====================

    @GetMapping("/members")
    public String listMembers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        var members = memberService.findAll();

        // Simple filtering
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            members = members.stream()
                .filter(m -> m.getFullName().toLowerCase().contains(searchLower) ||
                            m.getEmail().toLowerCase().contains(searchLower))
                .toList();
        }

        if (status != null && !status.isBlank()) {
            members = members.stream()
                .filter(m -> m.getStatus().name().equals(status))
                .toList();
        }

        if (type != null && !type.isBlank()) {
            members = members.stream()
                .filter(m -> m.getMembershipType().name().equals(type))
                .toList();
        }

        model.addAttribute("members", members);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("currentPage", "members");
        model.addAttribute("pageTitle", "Members - Library Management");
        model.addAttribute("totalPages", 1);

        return "members/list";
    }

    @GetMapping("/members/{id}")
    public String memberDetail(@PathVariable Long id, Model model) {
        var member = memberService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));

        // Get loans for this member
        var loans = loanService.findByMemberId(id);
        var activeLoans = loans.stream().filter(l -> !l.isReturned()).toList();
        var returnedLoans = loans.stream().filter(l -> l.isReturned()).toList();

        // Get notifications for this member
        var notifications = notificationService.findByMemberId(id);

        model.addAttribute("member", member);
        model.addAttribute("activeLoans", activeLoans);
        model.addAttribute("loanHistory", returnedLoans);
        model.addAttribute("notifications", notifications);
        model.addAttribute("currentPage", "members");
        model.addAttribute("pageTitle", member.getFullName() + " - Library Management");

        return "members/detail";
    }

    @GetMapping("/members/{id}/edit")
    public String editMemberForm(@PathVariable Long id, Model model) {
        var member = memberService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));

        model.addAttribute("member", member);
        model.addAttribute("currentPage", "members");
        model.addAttribute("pageTitle", "Edit " + member.getFullName() + " - Library Management");

        return "members/edit";
    }

    @PostMapping("/members/{id}/edit")
    public String updateMember(
            @PathVariable Long id,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam String membershipType,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            var member = memberService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));

            member.setFirstName(firstName);
            member.setLastName(lastName);
            member.setEmail(email);
            member.setPhone(phone);
            member.setMembershipType(com.example.library.members.internal.Member.MembershipType.valueOf(membershipType));

            // Update status using the appropriate method
            switch (status) {
                case "ACTIVE" -> member.activate();
                case "INACTIVE" -> member.deactivate();
                case "SUSPENDED" -> member.suspend();
            }

            memberService.save(member);
            redirectAttributes.addFlashAttribute("success", "Member updated successfully!");
            return "redirect:/members/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update member: " + e.getMessage());
            return "redirect:/members/" + id + "/edit";
        }
    }

    // ==================== Loans ====================

    @GetMapping("/loans")
    public String listLoans(@RequestParam(defaultValue = "0") int page, Model model) {
        var loans = loanService.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeLoans", loanService.countActiveLoans());
        stats.put("overdueLoans", loanService.countOverdueLoans());
        stats.put("totalUnpaidFines", loanService.getTotalUnpaidFines());

        model.addAttribute("loans", loans);
        model.addAttribute("stats", stats);
        model.addAttribute("dueSoonCount", loanService.findLoansDueSoon(7).size());
        model.addAttribute("showOverdue", false);
        model.addAttribute("currentPage", "loans");
        model.addAttribute("pageTitle", "Loans - Library Management");
        model.addAttribute("totalPages", 1);

        return "loans/list";
    }

    @GetMapping("/loans/overdue")
    public String listOverdueLoans(Model model) {
        var loans = loanService.findOverdueLoans();

        model.addAttribute("loans", loans);
        model.addAttribute("showOverdue", true);
        model.addAttribute("currentPage", "overdue");
        model.addAttribute("pageTitle", "Overdue Loans - Library Management");
        model.addAttribute("totalPages", 1);

        return "loans/list";
    }

    @PostMapping("/loans/{id}/return")
    public String returnBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            loanService.returnBook(id);
            redirectAttributes.addFlashAttribute("success", "Book returned successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to return book: " + e.getMessage());
        }
        return "redirect:/loans";
    }

    @PostMapping("/loans/{id}/renew")
    public String renewLoan(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            loanService.renewLoan(id);
            redirectAttributes.addFlashAttribute("success", "Loan renewed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to renew loan: " + e.getMessage());
        }
        return "redirect:/loans";
    }

    @GetMapping("/loans/{id}")
    public String loanDetail(@PathVariable Long id, Model model) {
        var loan = loanService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + id));

        model.addAttribute("loan", loan);
        model.addAttribute("currentPage", "loans");
        model.addAttribute("pageTitle", "Loan #" + id + " - Library Management");

        return "loans/detail";
    }

    @GetMapping("/loans/member/{memberId}")
    public String memberLoans(@PathVariable Long memberId, Model model) {
        var member = memberService.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        var loans = loanService.findByMemberId(memberId);
        var activeLoans = loans.stream().filter(l -> !l.isReturned()).toList();
        var returnedLoans = loans.stream().filter(l -> l.isReturned()).toList();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLoans", loans.size());
        stats.put("activeLoans", activeLoans.size());
        stats.put("returnedLoans", returnedLoans.size());

        model.addAttribute("member", member);
        model.addAttribute("loans", loans);
        model.addAttribute("activeLoans", activeLoans);
        model.addAttribute("returnedLoans", returnedLoans);
        model.addAttribute("stats", stats);
        model.addAttribute("currentPage", "loans");
        model.addAttribute("pageTitle", "Loans for " + member.getFullName() + " - Library Management");

        return "loans/member";
    }

    @GetMapping("/loans/new")
    public String newLoanForm(@RequestParam(required = false) String isbn, Model model) {
        // Get all available books
        var availableBooks = bookService.findAll().stream()
            .filter(b -> b.isAvailable())
            .toList();

        // Get all active members
        var activeMembers = memberService.findAll().stream()
            .filter(m -> m.getStatus().name().equals("ACTIVE"))
            .toList();

        model.addAttribute("books", availableBooks);
        model.addAttribute("members", activeMembers);
        model.addAttribute("selectedIsbn", isbn);
        model.addAttribute("currentPage", "loans");
        model.addAttribute("pageTitle", "New Loan - Library Management");

        return "loans/new";
    }

    @PostMapping("/loans/new")
    public String createLoan(
            @RequestParam String bookIsbn,
            @RequestParam Long memberId,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            var request = new LoanService.LoanBookRequest(bookIsbn, memberId, notes);
            loanService.loanBook(request);
            redirectAttributes.addFlashAttribute("success", "Book loaned successfully!");
            return "redirect:/loans";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create loan: " + e.getMessage());
            return "redirect:/loans/new?isbn=" + bookIsbn;
        }
    }

    // ==================== Notifications ====================

    @GetMapping("/notifications")
    public String listNotifications(Model model) {
        var notifications = notificationService.findAll();

        model.addAttribute("notifications", notifications);
        model.addAttribute("currentPage", "notifications");
        model.addAttribute("pageTitle", "Notifications - Library Management");

        return "notifications/list";
    }

    // ==================== Static Pages ====================

    @GetMapping("/authors")
    public String listAuthors(Model model) {
        var authors = bookService.findAllAuthors();
        var books = bookService.findAll();

        // Count books per author
        var bookCounts = new java.util.HashMap<Long, Long>();
        for (var author : authors) {
            long count = books.stream()
                .filter(b -> b.getAuthor().getId().equals(author.getId()))
                .count();
            bookCounts.put(author.getId(), count);
        }

        model.addAttribute("authors", authors);
        model.addAttribute("bookCounts", bookCounts);
        model.addAttribute("currentPage", "authors");
        model.addAttribute("pageTitle", "Authors - Library Management");
        return "authors/list";
    }

    @GetMapping("/categories")
    public String listCategories(Model model) {
        var categories = bookService.findAllCategories();
        var books = bookService.findAll();

        // Count books per category
        var bookCounts = new java.util.HashMap<Long, Long>();
        for (var category : categories) {
            long count = books.stream()
                .filter(b -> b.getCategory() != null && b.getCategory().getId().equals(category.getId()))
                .count();
            bookCounts.put(category.getId(), count);
        }

        model.addAttribute("categories", categories);
        model.addAttribute("bookCounts", bookCounts);
        model.addAttribute("currentPage", "categories");
        model.addAttribute("pageTitle", "Categories - Library Management");
        return "categories/list";
    }

    @GetMapping("/members/cards")
    public String listCards(Model model) {
        var cards = memberService.findAllCards();
        var expiringCards = memberService.findExpiringCards(30);

        model.addAttribute("cards", cards);
        model.addAttribute("expiringSoonCount", expiringCards.size());
        model.addAttribute("currentPage", "cards");
        model.addAttribute("pageTitle", "Membership Cards - Library Management");
        return "members/cards";
    }
}
