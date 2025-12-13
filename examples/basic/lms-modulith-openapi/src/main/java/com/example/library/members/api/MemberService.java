package com.example.library.members.api;

import com.example.library.members.events.MemberRegisteredEvent;
import com.example.library.members.events.MemberStatusChangedEvent;
import com.example.library.members.internal.Member;
import com.example.library.members.internal.Member.MemberStatus;
import com.example.library.members.internal.Member.MembershipType;
import com.example.library.members.internal.MemberRepository;
import com.example.library.members.internal.MembershipCard;
import com.example.library.members.internal.MembershipCardRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for the Members module.
 * This service is exposed to other modules via the Named Interface.
 */
@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MembershipCardRepository cardRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MemberService(MemberRepository memberRepository,
                         MembershipCardRepository cardRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.memberRepository = memberRepository;
        this.cardRepository = cardRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Optional<Member> findById(Long id) {
        return memberRepository.findByIdWithCard(id);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmailWithCard(email);
    }

    public Page<Member> searchMembers(String query, Pageable pageable) {
        return memberRepository.searchMembers(query, pageable);
    }

    public List<Member> findByStatus(MemberStatus status) {
        return memberRepository.findByStatus(status);
    }

    public List<Member> findByMembershipType(MembershipType membershipType) {
        return memberRepository.findByMembershipType(membershipType);
    }

    @Transactional
    public Member registerMember(RegisterMemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        Member member = new Member(
            request.email(),
            request.firstName(),
            request.lastName(),
            request.membershipType() != null ? request.membershipType() : MembershipType.STANDARD
        );
        member.setPhone(request.phone());
        member.setAddress(request.address());

        Member savedMember = memberRepository.save(member);

        // Create membership card
        String cardNumber = generateCardNumber();
        LocalDate expiryDate = LocalDate.now().plusYears(
            savedMember.getMembershipType() == MembershipType.PREMIUM ? 2 : 1
        );
        MembershipCard card = new MembershipCard(savedMember, cardNumber, expiryDate);
        cardRepository.save(card);
        savedMember.setMembershipCard(card);

        // Publish event
        eventPublisher.publishEvent(new MemberRegisteredEvent(
            savedMember.getId(),
            savedMember.getEmail(),
            savedMember.getFullName(),
            savedMember.getMembershipType().name(),
            cardNumber
        ));

        return savedMember;
    }

    @Transactional
    public Member updateMember(Long id, UpdateMemberRequest request) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));

        if (request.firstName() != null) {
            member.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            member.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            member.setPhone(request.phone());
        }
        if (request.address() != null) {
            member.setAddress(request.address());
        }
        if (request.membershipType() != null) {
            member.setMembershipType(request.membershipType());
        }

        return memberRepository.save(member);
    }

    @Transactional
    public Member save(Member member) {
        return memberRepository.save(member);
    }

    @Transactional
    public void suspendMember(Long id) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));

        MemberStatus previousStatus = member.getStatus();
        member.suspend();
        memberRepository.save(member);

        eventPublisher.publishEvent(new MemberStatusChangedEvent(
            member.getId(),
            member.getEmail(),
            previousStatus.name(),
            MemberStatus.SUSPENDED.name()
        ));
    }

    @Transactional
    public void activateMember(Long id) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));

        MemberStatus previousStatus = member.getStatus();
        member.activate();
        memberRepository.save(member);

        eventPublisher.publishEvent(new MemberStatusChangedEvent(
            member.getId(),
            member.getEmail(),
            previousStatus.name(),
            MemberStatus.ACTIVE.name()
        ));
    }

    @Transactional
    public void deactivateMember(Long id) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));

        MemberStatus previousStatus = member.getStatus();
        member.deactivate();
        memberRepository.save(member);

        eventPublisher.publishEvent(new MemberStatusChangedEvent(
            member.getId(),
            member.getEmail(),
            previousStatus.name(),
            MemberStatus.INACTIVE.name()
        ));
    }

    @Transactional
    public MembershipCard renewCard(Long memberId, int years) {
        Member member = memberRepository.findByIdWithCard(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        MembershipCard card = member.getMembershipCard();
        if (card == null) {
            throw new IllegalStateException("Member has no membership card");
        }

        LocalDate newExpiry = card.getExpiryDate().isBefore(LocalDate.now())
            ? LocalDate.now().plusYears(years)
            : card.getExpiryDate().plusYears(years);

        card.setExpiryDate(newExpiry);
        return cardRepository.save(card);
    }

    public Optional<MembershipCard> findCardByNumber(String cardNumber) {
        return cardRepository.findByCardNumberWithMember(cardNumber);
    }

    public List<MembershipCard> findExpiringCards(int daysAhead) {
        return cardRepository.findCardsExpiringSoon(
            LocalDate.now(),
            LocalDate.now().plusDays(daysAhead)
        );
    }

    public List<MembershipCard> findAllCards() {
        return cardRepository.findAllWithMember();
    }

    public long countMembers() {
        return memberRepository.count();
    }

    public long countActiveMembers() {
        return memberRepository.countByStatus(MemberStatus.ACTIVE);
    }

    public long countPremiumMembers() {
        return memberRepository.countByMembershipType(MembershipType.PREMIUM);
    }

    /**
     * Check if a member can borrow books (used by Loans module).
     */
    public boolean canBorrow(Long memberId) {
        return memberRepository.findByIdWithCard(memberId)
            .map(member -> member.isActive() &&
                          member.getMembershipCard() != null &&
                          member.getMembershipCard().isValid())
            .orElse(false);
    }

    private String generateCardNumber() {
        return "LIB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Request DTOs
    public record RegisterMemberRequest(
        String email,
        String firstName,
        String lastName,
        String phone,
        String address,
        MembershipType membershipType
    ) {}

    public record UpdateMemberRequest(
        String firstName,
        String lastName,
        String phone,
        String address,
        MembershipType membershipType
    ) {}
}
