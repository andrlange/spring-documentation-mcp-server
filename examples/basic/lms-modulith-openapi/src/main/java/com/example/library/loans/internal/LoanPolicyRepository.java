package com.example.library.loans.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanPolicyRepository extends JpaRepository<LoanPolicy, Long> {

    Optional<LoanPolicy> findByMembershipType(String membershipType);

    boolean existsByMembershipType(String membershipType);
}
