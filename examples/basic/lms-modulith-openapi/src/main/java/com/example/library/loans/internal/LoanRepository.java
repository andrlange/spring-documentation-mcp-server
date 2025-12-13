package com.example.library.loans.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByMemberId(Long memberId);

    List<Loan> findByBookIsbn(String bookIsbn);

    List<Loan> findByStatus(Loan.LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.memberId = :memberId AND l.status = :status")
    List<Loan> findByMemberIdAndStatus(
        @Param("memberId") Long memberId,
        @Param("status") Loan.LoanStatus status
    );

    @Query("SELECT l FROM Loan l WHERE l.bookIsbn = :isbn AND l.status = 'ACTIVE'")
    Optional<Loan> findActiveByBookIsbn(@Param("isbn") String isbn);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < :date")
    List<Loan> findOverdueLoans(@Param("date") LocalDate date);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate BETWEEN :startDate AND :endDate")
    List<Loan> findLoansDueSoon(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.memberId = :memberId AND l.status = 'ACTIVE'")
    long countActiveByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT l FROM Loan l WHERE l.memberId = :memberId ORDER BY l.loanDate DESC")
    Page<Loan> findByMemberIdOrderByLoanDateDesc(
        @Param("memberId") Long memberId,
        Pageable pageable
    );

    long countByStatus(Loan.LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.fineAmount > 0 AND l.finePaid = false")
    List<Loan> findLoansWithUnpaidFines();

    @Query("SELECT SUM(l.fineAmount) FROM Loan l WHERE l.finePaid = false")
    Double getTotalUnpaidFines();
}
