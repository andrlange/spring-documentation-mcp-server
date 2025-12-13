package com.example.library.members.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipCardRepository extends JpaRepository<MembershipCard, Long> {

    Optional<MembershipCard> findByCardNumber(String cardNumber);

    boolean existsByCardNumber(String cardNumber);

    Optional<MembershipCard> findByMemberId(Long memberId);

    @Query("SELECT mc FROM MembershipCard mc WHERE mc.expiryDate < :date")
    List<MembershipCard> findExpiredCards(@Param("date") LocalDate date);

    @Query("SELECT mc FROM MembershipCard mc WHERE mc.expiryDate BETWEEN :startDate AND :endDate")
    List<MembershipCard> findCardsExpiringSoon(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT mc FROM MembershipCard mc JOIN FETCH mc.member WHERE mc.cardNumber = :cardNumber")
    Optional<MembershipCard> findByCardNumberWithMember(@Param("cardNumber") String cardNumber);

    @Query("SELECT mc FROM MembershipCard mc JOIN FETCH mc.member ORDER BY mc.member.lastName, mc.member.firstName")
    List<MembershipCard> findAllWithMember();

    long countByExpiryDateBefore(LocalDate date);
}
