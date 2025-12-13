package com.example.library.members.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Member> findByStatus(Member.MemberStatus status);

    List<Member> findByMembershipType(Member.MembershipType membershipType);

    @Query("SELECT m FROM Member m WHERE m.status = :status AND m.membershipType = :type")
    List<Member> findByStatusAndMembershipType(
        @Param("status") Member.MemberStatus status,
        @Param("type") Member.MembershipType membershipType
    );

    @Query("SELECT m FROM Member m WHERE " +
           "LOWER(m.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Member> searchMembers(@Param("query") String query, Pageable pageable);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.membershipCard WHERE m.id = :id")
    Optional<Member> findByIdWithCard(@Param("id") Long id);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.membershipCard WHERE m.email = :email")
    Optional<Member> findByEmailWithCard(@Param("email") String email);

    long countByStatus(Member.MemberStatus status);

    long countByMembershipType(Member.MembershipType membershipType);
}
