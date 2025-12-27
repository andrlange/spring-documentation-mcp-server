package com.example.library.service;

import com.example.library.data.DataInitializer;
import com.example.library.model.Member;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service for member operations with observability.
 */
@Service
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);
    private final DataInitializer dataInitializer;

    public MemberService(DataInitializer dataInitializer) {
        this.dataInitializer = dataInitializer;
    }

    @Observed(name = "library.member.findAll", contextualName = "find-all-members")
    public Collection<Member> findAll() {
        log.info("Finding all members");
        return dataInitializer.getMembers().values();
    }

    @Observed(name = "library.member.findById", contextualName = "find-member-by-id")
    public Optional<Member> findById(Long id) {
        log.info("Finding member by id: {}", id);
        return Optional.ofNullable(dataInitializer.getMembers().get(id));
    }

    @Observed(name = "library.member.findActive", contextualName = "find-active-members")
    public List<Member> findActive() {
        log.info("Finding active members");
        return dataInitializer.getMembers().values().stream()
            .filter(Member::active)
            .toList();
    }

    @Observed(name = "library.member.search", contextualName = "search-members")
    public List<Member> search(String query) {
        log.info("Searching members with query: {}", query);
        String lowerQuery = query.toLowerCase();
        return dataInitializer.getMembers().values().stream()
            .filter(member ->
                member.name().toLowerCase().contains(lowerQuery) ||
                member.email().toLowerCase().contains(lowerQuery))
            .toList();
    }

    public long count() {
        return dataInitializer.getMembers().size();
    }

    public long countActive() {
        return dataInitializer.getMembers().values().stream()
            .filter(Member::active)
            .count();
    }
}
