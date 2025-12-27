package com.example.library.data;

import com.example.library.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Initializes and manages in-memory mock data for the library demo.
 * All data is relative to the application start date and resets on restart.
 */
@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final Map<Long, Category> categories = new ConcurrentHashMap<>();
    private final Map<String, Book> books = new ConcurrentHashMap<>();
    private final Map<Long, Member> members = new ConcurrentHashMap<>();
    private final Map<Long, Loan> loans = new ConcurrentHashMap<>();

    private final AtomicLong loanIdGenerator = new AtomicLong(1);
    private final LocalDate startDate;
    private final Random random = new Random(42); // Fixed seed for reproducibility

    // Authors for book generation
    private static final String[] AUTHORS = {
        "Jane Austen", "Charles Dickens", "Mark Twain", "Ernest Hemingway",
        "Virginia Woolf", "George Orwell", "F. Scott Fitzgerald", "Leo Tolstoy",
        "Gabriel Garcia Marquez", "Haruki Murakami", "Toni Morrison", "Stephen King",
        "J.K. Rowling", "Isaac Asimov", "Agatha Christie", "Arthur Conan Doyle",
        "Margaret Atwood", "Neil Gaiman", "Terry Pratchett", "Ursula K. Le Guin"
    };

    // Title prefixes and suffixes for generating book titles
    private static final String[] TITLE_PREFIXES = {
        "The", "A", "My", "Our", "Their", "Last", "First", "Secret", "Hidden", "Lost"
    };

    private static final String[] TITLE_NOUNS = {
        "Journey", "Dream", "Mystery", "Adventure", "Story", "Tale", "Legend",
        "Chronicle", "Saga", "Path", "Road", "Garden", "House", "Island", "World"
    };

    public DataInitializer() {
        this.startDate = LocalDate.now();
        initializeData();
    }

    private void initializeData() {
        log.info("Initializing library mock data...");

        initializeCategories();
        initializeBooks();
        initializeMembers();
        initializeLoans();

        log.info("Data initialization complete: {} categories, {} books, {} members, {} loans",
            categories.size(), books.size(), members.size(), loans.size());
    }

    private void initializeCategories() {
        List<Category> categoryList = List.of(
            new Category(1L, "Fiction", "Literary and general fiction novels"),
            new Category(2L, "Science Fiction", "Futuristic and speculative fiction"),
            new Category(3L, "Mystery/Thriller", "Detective stories and suspense novels"),
            new Category(4L, "Romance", "Love stories and romantic fiction"),
            new Category(5L, "Biography", "Life stories of notable people"),
            new Category(6L, "History", "Historical accounts and analysis"),
            new Category(7L, "Science & Technology", "Scientific discoveries and tech"),
            new Category(8L, "Business", "Business strategies and management"),
            new Category(9L, "Self-Help", "Personal development and motivation"),
            new Category(10L, "Children's Books", "Stories for young readers")
        );

        categoryList.forEach(c -> categories.put(c.id(), c));
        log.info("Initialized {} categories", categories.size());
    }

    private void initializeBooks() {
        int bookCount = 0;

        for (Category category : categories.values()) {
            // 20 books per category = 200 total
            for (int i = 0; i < 20; i++) {
                bookCount++;
                String isbn = generateIsbn(category.id(), i);
                String title = generateTitle(category.name(), i);
                String author = AUTHORS[random.nextInt(AUTHORS.length)];
                int publicationYear = 2000 + random.nextInt(25); // 2000-2024
                int totalCopies = 1 + random.nextInt(5); // 1-5 copies
                int availableCopies = totalCopies; // Start with all available

                Book book = new Book(isbn, title, author, category.id(),
                    publicationYear, availableCopies, totalCopies);
                books.put(isbn, book);
            }
        }

        log.info("Initialized {} books across {} categories", bookCount, categories.size());
    }

    private String generateIsbn(Long categoryId, int index) {
        return String.format("978-%d-%04d-%04d-%d",
            categoryId, index + 1, random.nextInt(10000), random.nextInt(10));
    }

    private String generateTitle(String categoryName, int index) {
        String prefix = TITLE_PREFIXES[random.nextInt(TITLE_PREFIXES.length)];
        String noun = TITLE_NOUNS[random.nextInt(TITLE_NOUNS.length)];

        return switch (index % 5) {
            case 0 -> prefix + " " + noun;
            case 1 -> prefix + " " + categoryName + " " + noun;
            case 2 -> noun + " of " + categoryName;
            case 3 -> prefix + " Great " + noun;
            default -> categoryName + ": " + prefix + " " + noun;
        };
    }

    private void initializeMembers() {
        String[] firstNames = {"Alice", "Bob", "Charlie", "Diana", "Edward",
            "Fiona", "George", "Hannah", "Ivan", "Julia",
            "Kevin", "Laura", "Michael", "Nancy", "Oscar",
            "Patricia", "Quentin", "Rachel", "Steven", "Teresa"};

        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones",
            "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};

        for (int i = 0; i < 20; i++) {
            Long id = (long) (i + 1);
            String name = firstNames[i] + " " + lastNames[i % lastNames.length];
            String email = firstNames[i].toLowerCase() + "." +
                lastNames[i % lastNames.length].toLowerCase() + "@example.com";

            // 5 premium members (IDs 1-5), rest are standard
            MembershipType type = (i < 5) ? MembershipType.PREMIUM : MembershipType.STANDARD;

            // Join dates within last 2 years
            int daysAgo = random.nextInt(730); // 0-730 days ago
            LocalDate joinDate = startDate.minusDays(daysAgo);

            // 2-3 inactive members (IDs 18-20)
            boolean active = (i < 17);

            Member member = new Member(id, name, email, type, joinDate, active);
            members.put(id, member);
        }

        log.info("Initialized {} members ({} active, {} inactive)",
            members.size(),
            members.values().stream().filter(Member::active).count(),
            members.values().stream().filter(m -> !m.active()).count());
    }

    private void initializeLoans() {
        List<String> bookIsbns = new ArrayList<>(books.keySet());
        List<Long> activeMemberIds = members.values().stream()
            .filter(Member::active)
            .map(Member::id)
            .toList();

        // Generate ~50 loans with mix of active, overdue, and returned
        for (int i = 0; i < 50; i++) {
            String bookIsbn = bookIsbns.get(random.nextInt(bookIsbns.size()));
            Long memberId = activeMemberIds.get(random.nextInt(activeMemberIds.size()));
            Member member = members.get(memberId);

            // Loan date within last 60 days
            int daysAgo = random.nextInt(60);
            LocalDate loanDate = startDate.minusDays(daysAgo);
            LocalDate dueDate = loanDate.plusDays(member.membershipType().getLoanDays());

            LocalDate returnDate = null;

            // Determine loan status
            int status = random.nextInt(10);
            if (status < 3) {
                // 30% returned loans
                int returnDays = random.nextInt(member.membershipType().getLoanDays());
                returnDate = loanDate.plusDays(returnDays);
            } else if (status < 5 && dueDate.isBefore(startDate)) {
                // 20% overdue (only if due date is in the past)
                // Keep returnDate null to make it overdue
            }
            // 50% active loans (returnDate stays null)

            Loan loan = new Loan(loanIdGenerator.getAndIncrement(),
                bookIsbn, memberId, loanDate, dueDate, returnDate);
            loans.put(loan.id(), loan);

            // Update book availability for active loans
            if (loan.isActive()) {
                Book book = books.get(bookIsbn);
                if (book != null && book.availableCopies() > 0) {
                    books.put(bookIsbn, book.withAvailableCopies(book.availableCopies() - 1));
                }
            }
        }

        long activeLoans = loans.values().stream().filter(Loan::isActive).count();
        long overdueLoans = loans.values().stream().filter(Loan::isOverdue).count();
        long returnedLoans = loans.values().stream().filter(l -> l.returnDate() != null).count();

        log.info("Initialized {} loans ({} active, {} overdue, {} returned)",
            loans.size(), activeLoans, overdueLoans, returnedLoans);
    }

    // Accessors for data collections
    public Map<Long, Category> getCategories() {
        return categories;
    }

    public Map<String, Book> getBooks() {
        return books;
    }

    public Map<Long, Member> getMembers() {
        return members;
    }

    public Map<Long, Loan> getLoans() {
        return loans;
    }

    public Long generateLoanId() {
        return loanIdGenerator.getAndIncrement();
    }

    public LocalDate getStartDate() {
        return startDate;
    }
}
