-- Library Management System - Initial Schema
-- Spring Boot 4.0 / Spring Modulith 2.0 Demo

-- =====================================================
-- CATALOG MODULE
-- =====================================================

-- Categories table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Authors table
CREATE TABLE authors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    biography TEXT,
    birth_year INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Books table
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    author_id BIGINT NOT NULL REFERENCES authors(id),
    category_id BIGINT REFERENCES categories(id),
    description TEXT,
    publication_year INTEGER,
    total_copies INTEGER NOT NULL DEFAULT 1,
    available_copies INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_available_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author_id);
CREATE INDEX idx_books_category ON books(category_id);

-- =====================================================
-- MEMBERS MODULE
-- =====================================================

-- Members table
CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    membership_date DATE NOT NULL DEFAULT CURRENT_DATE,
    membership_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_membership_type CHECK (membership_type IN ('STANDARD', 'PREMIUM')),
    CONSTRAINT chk_member_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_members_email ON members(email);
CREATE INDEX idx_members_status ON members(status);

-- Membership cards table
CREATE TABLE membership_cards (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE REFERENCES members(id) ON DELETE CASCADE,
    card_number VARCHAR(20) NOT NULL UNIQUE,
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_membership_cards_number ON membership_cards(card_number);

-- =====================================================
-- LOANS MODULE
-- =====================================================

-- Loans table
CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    book_isbn VARCHAR(20) NOT NULL,
    member_id BIGINT NOT NULL REFERENCES members(id),
    loan_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    renewal_count INTEGER NOT NULL DEFAULT 0,
    fine_amount DOUBLE PRECISION NOT NULL DEFAULT 0.00,
    fine_paid BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_loan_status CHECK (status IN ('ACTIVE', 'RETURNED', 'RETURNED_LATE', 'LOST'))
);

CREATE INDEX idx_loans_book_isbn ON loans(book_isbn);
CREATE INDEX idx_loans_member ON loans(member_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_due_date ON loans(due_date);

-- Loan policies table
CREATE TABLE loan_policies (
    id BIGSERIAL PRIMARY KEY,
    membership_type VARCHAR(20) NOT NULL UNIQUE,
    max_books INTEGER NOT NULL DEFAULT 5,
    loan_duration_days INTEGER NOT NULL DEFAULT 14,
    max_renewals INTEGER NOT NULL DEFAULT 2,
    renewal_duration_days INTEGER NOT NULL DEFAULT 7,
    daily_fine_rate DOUBLE PRECISION NOT NULL DEFAULT 0.50,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- NOTIFICATIONS MODULE
-- =====================================================

-- Notifications table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    type VARCHAR(30) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notification_type CHECK (type IN ('LOAN_CONFIRMATION', 'RETURN_CONFIRMATION', 'DUE_SOON_REMINDER', 'OVERDUE_NOTICE', 'FINE_NOTICE', 'CARD_EXPIRING', 'WELCOME', 'GENERAL')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'READ', 'FAILED'))
);

CREATE INDEX idx_notifications_member ON notifications(member_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_status ON notifications(status);

-- =====================================================
-- INITIAL DATA
-- =====================================================

-- Insert default loan policies
INSERT INTO loan_policies (membership_type, max_books, loan_duration_days, max_renewals, renewal_duration_days, daily_fine_rate)
VALUES
    ('STANDARD', 5, 14, 2, 7, 0.50),
    ('PREMIUM', 10, 21, 3, 14, 0.25);

-- Insert sample categories
INSERT INTO categories (name, description) VALUES
    ('Fiction', 'Fictional literature including novels and short stories'),
    ('Non-Fiction', 'Factual books including biographies and documentaries'),
    ('Science', 'Scientific literature and textbooks'),
    ('Technology', 'Computing, programming, and technology books'),
    ('History', 'Historical literature and documents'),
    ('Philosophy', 'Philosophical works and essays');

-- Insert sample authors
INSERT INTO authors (name, biography, birth_year) VALUES
    ('Robert C. Martin', 'American software engineer, author of Clean Code', 1952),
    ('Martin Fowler', 'British software developer, author of Refactoring', 1963),
    ('Eric Evans', 'Author of Domain-Driven Design', 1960),
    ('Joshua Bloch', 'American software engineer, author of Effective Java', 1961),
    ('Craig Walls', 'Author of Spring in Action', 1970);

-- Insert sample books
INSERT INTO books (isbn, title, author_id, category_id, description, publication_year, total_copies, available_copies) VALUES
    ('978-0-13-468599-1', 'Clean Code', 1, 4, 'A Handbook of Agile Software Craftsmanship', 2008, 5, 5),
    ('978-0-13-235088-4', 'Clean Architecture', 1, 4, 'A Craftsmans Guide to Software Structure and Design', 2017, 3, 3),
    ('978-0-201-63361-0', 'Design Patterns', 2, 4, 'Elements of Reusable Object-Oriented Software', 1994, 4, 4),
    ('978-0-321-12521-7', 'Domain-Driven Design', 3, 4, 'Tackling Complexity in the Heart of Software', 2003, 3, 3),
    ('978-0-13-468598-4', 'Effective Java', 4, 4, 'Best practices for the Java platform', 2017, 4, 4),
    ('978-1-61729-875-6', 'Spring in Action', 5, 4, 'Covers Spring 6 and Spring Boot 3', 2022, 5, 5);

-- Insert sample members
INSERT INTO members (email, first_name, last_name, phone, membership_type, status) VALUES
    ('john.doe@example.com', 'John', 'Doe', '+1-555-0101', 'PREMIUM', 'ACTIVE'),
    ('jane.smith@example.com', 'Jane', 'Smith', '+1-555-0102', 'STANDARD', 'ACTIVE'),
    ('bob.wilson@example.com', 'Bob', 'Wilson', '+1-555-0103', 'STANDARD', 'ACTIVE');

-- Insert membership cards
INSERT INTO membership_cards (member_id, card_number, expiry_date) VALUES
    (1, 'LIB-2024-0001', CURRENT_DATE + INTERVAL '2 years'),
    (2, 'LIB-2024-0002', CURRENT_DATE + INTERVAL '1 year'),
    (3, 'LIB-2024-0003', CURRENT_DATE + INTERVAL '1 year');
