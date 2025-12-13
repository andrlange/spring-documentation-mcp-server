package com.example.library.members.api;

import com.example.library.members.internal.Member;
import com.example.library.members.internal.Member.MembershipType;
import com.example.library.members.internal.MembershipCard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Member management with API Versioning.
 * Demonstrates Spring Framework 7's first-class API versioning support.
 */
@RestController
@RequestMapping("/api/members")
@Tag(name = "Member Management", description = "APIs for managing library members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // ==================== VERSION 1.0 APIs ====================

    @GetMapping(version = "1.0")
    @Operation(summary = "List all members (v1.0)", description = "Returns basic member information")
    @ApiResponse(responseCode = "200", description = "List of members")
    public List<MemberResponseV1> getAllMembersV1() {
        return memberService.findAll().stream()
            .map(this::toMemberResponseV1)
            .toList();
    }

    @GetMapping(path = "/{id}", version = "1.0")
    @Operation(summary = "Get member by ID (v1.0)", description = "Returns basic member details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member found",
            content = @Content(schema = @Schema(implementation = MemberResponseV1.class))),
        @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public ResponseEntity<MemberResponseV1> getMemberByIdV1(
            @Parameter(description = "Member ID") @PathVariable Long id) {
        return memberService.findById(id)
            .map(member -> ResponseEntity.ok(toMemberResponseV1(member)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(version = "1.0")
    @Operation(summary = "Register new member (v1.0)", description = "Creates a new library member")
    @ApiResponse(responseCode = "201", description = "Member registered successfully")
    public ResponseEntity<MemberResponseV1> registerMemberV1(
            @Valid @RequestBody RegisterMemberRequestV1 request) {
        Member member = memberService.registerMember(new MemberService.RegisterMemberRequest(
            request.email(),
            request.firstName(),
            request.lastName(),
            request.phone(),
            null,
            MembershipType.STANDARD
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toMemberResponseV1(member));
    }

    @PutMapping(path = "/{id}", version = "1.0")
    @Operation(summary = "Update member (v1.0)", description = "Updates member details")
    public ResponseEntity<MemberResponseV1> updateMemberV1(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequestV1 request) {
        Member member = memberService.updateMember(id, new MemberService.UpdateMemberRequest(
            request.firstName(),
            request.lastName(),
            request.phone(),
            null,
            null
        ));
        return ResponseEntity.ok(toMemberResponseV1(member));
    }

    @PostMapping(path = "/{id}/suspend", version = "1.0")
    @Operation(summary = "Suspend member (v1.0)", description = "Suspends a member's account")
    @ApiResponse(responseCode = "204", description = "Member suspended")
    public ResponseEntity<Void> suspendMemberV1(@PathVariable Long id) {
        memberService.suspendMember(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/activate", version = "1.0")
    @Operation(summary = "Activate member (v1.0)", description = "Activates a member's account")
    @ApiResponse(responseCode = "204", description = "Member activated")
    public ResponseEntity<Void> activateMemberV1(@PathVariable Long id) {
        memberService.activateMember(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== VERSION 2.0 APIs (Extended) ====================

    @GetMapping(version = "2.0+")
    @Operation(summary = "List all members (v2.0)", description = "Returns extended member information with card details")
    @ApiResponse(responseCode = "200", description = "List of members with full details")
    public List<MemberResponseV2> getAllMembersV2() {
        return memberService.findAll().stream()
            .map(this::toMemberResponseV2)
            .toList();
    }

    @GetMapping(path = "/{id}", version = "2.0+")
    @Operation(summary = "Get member by ID (v2.0)", description = "Returns extended member details with card and statistics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member found",
            content = @Content(schema = @Schema(implementation = MemberResponseV2.class))),
        @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public ResponseEntity<MemberResponseV2> getMemberByIdV2(@PathVariable Long id) {
        return memberService.findById(id)
            .map(member -> ResponseEntity.ok(toMemberResponseV2(member)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(path = "/search", version = "2.0+")
    @Operation(summary = "Search members (v2.0)", description = "Search members by name or email")
    public Page<MemberResponseV2> searchMembersV2(
            @Parameter(description = "Search query") @RequestParam String query,
            Pageable pageable) {
        return memberService.searchMembers(query, pageable)
            .map(this::toMemberResponseV2);
    }

    @GetMapping(path = "/by-email/{email}", version = "2.0+")
    @Operation(summary = "Get member by email (v2.0)", description = "Returns member by email address")
    public ResponseEntity<MemberResponseV2> getMemberByEmailV2(@PathVariable String email) {
        return memberService.findByEmail(email)
            .map(member -> ResponseEntity.ok(toMemberResponseV2(member)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(path = "/cards/expiring", version = "2.0+")
    @Operation(summary = "Get expiring cards (v2.0)", description = "Returns cards expiring within specified days")
    public List<CardExpiringInfo> getExpiringCardsV2(
            @Parameter(description = "Days ahead to check") @RequestParam(defaultValue = "30") int days) {
        return memberService.findExpiringCards(days).stream()
            .map(card -> new CardExpiringInfo(
                card.getCardNumber(),
                card.getMember().getFullName(),
                card.getMember().getEmail(),
                card.getExpiryDate()
            ))
            .toList();
    }

    @PostMapping(version = "2.0+")
    @Operation(summary = "Register new member (v2.0)", description = "Creates a new member with extended options")
    @ApiResponse(responseCode = "201", description = "Member registered successfully")
    public ResponseEntity<MemberResponseV2> registerMemberV2(
            @Valid @RequestBody RegisterMemberRequestV2 request) {
        Member member = memberService.registerMember(new MemberService.RegisterMemberRequest(
            request.email(),
            request.firstName(),
            request.lastName(),
            request.phone(),
            request.address(),
            request.membershipType() != null ? request.membershipType() : MembershipType.STANDARD
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toMemberResponseV2(member));
    }

    @PutMapping(path = "/{id}", version = "2.0+")
    @Operation(summary = "Update member (v2.0)", description = "Updates member with extended fields")
    public ResponseEntity<MemberResponseV2> updateMemberV2(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequestV2 request) {
        Member member = memberService.updateMember(id, new MemberService.UpdateMemberRequest(
            request.firstName(),
            request.lastName(),
            request.phone(),
            request.address(),
            request.membershipType()
        ));
        return ResponseEntity.ok(toMemberResponseV2(member));
    }

    @PostMapping(path = "/{id}/suspend", version = "2.0+")
    @Operation(summary = "Suspend member (v2.0)", description = "Suspends a member's account")
    @ApiResponse(responseCode = "200", description = "Member suspended")
    public ResponseEntity<MemberResponseV2> suspendMemberV2(@PathVariable Long id) {
        memberService.suspendMember(id);
        return memberService.findById(id)
            .map(member -> ResponseEntity.ok(toMemberResponseV2(member)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/{id}/activate", version = "2.0+")
    @Operation(summary = "Activate member (v2.0)", description = "Activates a member's account")
    @ApiResponse(responseCode = "200", description = "Member activated")
    public ResponseEntity<MemberResponseV2> activateMemberV2(@PathVariable Long id) {
        memberService.activateMember(id);
        return memberService.findById(id)
            .map(member -> ResponseEntity.ok(toMemberResponseV2(member)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/{id}/renew-card", version = "2.0+")
    @Operation(summary = "Renew membership card (v2.0)", description = "Extends membership card validity")
    public ResponseEntity<CardInfo> renewCardV2(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Positive int years) {
        MembershipCard card = memberService.renewCard(id, years);
        return ResponseEntity.ok(new CardInfo(
            card.getCardNumber(),
            card.getIssueDate(),
            card.getExpiryDate(),
            card.isValid()
        ));
    }

    // ==================== DTO Mappers ====================

    private MemberResponseV1 toMemberResponseV1(Member member) {
        return new MemberResponseV1(
            member.getId(),
            member.getEmail(),
            member.getFullName(),
            member.getStatus().name()
        );
    }

    private MemberResponseV2 toMemberResponseV2(Member member) {
        MembershipCard card = member.getMembershipCard();
        return new MemberResponseV2(
            member.getId(),
            member.getEmail(),
            member.getFirstName(),
            member.getLastName(),
            member.getPhone(),
            member.getAddress(),
            member.getMembershipDate(),
            member.getMembershipType().name(),
            member.getStatus().name(),
            card != null ? new CardInfo(
                card.getCardNumber(),
                card.getIssueDate(),
                card.getExpiryDate(),
                card.isValid()
            ) : null,
            memberService.canBorrow(member.getId())
        );
    }

    // ==================== Request/Response DTOs ====================

    // V1 DTOs
    @Schema(description = "Member response (Version 1.0)")
    public record MemberResponseV1(
        @Schema(description = "Member ID", example = "1") Long id,
        @Schema(description = "Email", example = "john@example.com") String email,
        @Schema(description = "Full name", example = "John Doe") String fullName,
        @Schema(description = "Status", example = "ACTIVE") String status
    ) {}

    @Schema(description = "Register member request (Version 1.0)")
    public record RegisterMemberRequestV1(
        @NotBlank @Email @Schema(description = "Email", example = "john@example.com") String email,
        @NotBlank @Schema(description = "First name", example = "John") String firstName,
        @NotBlank @Schema(description = "Last name", example = "Doe") String lastName,
        @Schema(description = "Phone number", example = "+1234567890") String phone
    ) {}

    @Schema(description = "Update member request (Version 1.0)")
    public record UpdateMemberRequestV1(
        @Schema(description = "First name") String firstName,
        @Schema(description = "Last name") String lastName,
        @Schema(description = "Phone number") String phone
    ) {}

    // V2 DTOs (Extended)
    @Schema(description = "Member response (Version 2.0) - Extended with full details")
    public record MemberResponseV2(
        @Schema(description = "Member ID", example = "1") Long id,
        @Schema(description = "Email", example = "john@example.com") String email,
        @Schema(description = "First name", example = "John") String firstName,
        @Schema(description = "Last name", example = "Doe") String lastName,
        @Schema(description = "Phone number", example = "+1234567890") String phone,
        @Schema(description = "Address") String address,
        @Schema(description = "Membership date") LocalDate membershipDate,
        @Schema(description = "Membership type", example = "PREMIUM") String membershipType,
        @Schema(description = "Status", example = "ACTIVE") String status,
        @Schema(description = "Membership card details") CardInfo card,
        @Schema(description = "Can borrow books", example = "true") Boolean canBorrow
    ) {}

    @Schema(description = "Membership card information")
    public record CardInfo(
        @Schema(description = "Card number", example = "LIB-A1B2C3D4") String cardNumber,
        @Schema(description = "Issue date") LocalDate issueDate,
        @Schema(description = "Expiry date") LocalDate expiryDate,
        @Schema(description = "Is valid", example = "true") Boolean isValid
    ) {}

    @Schema(description = "Card expiring information")
    public record CardExpiringInfo(
        @Schema(description = "Card number") String cardNumber,
        @Schema(description = "Member name") String memberName,
        @Schema(description = "Member email") String memberEmail,
        @Schema(description = "Expiry date") LocalDate expiryDate
    ) {}

    @Schema(description = "Register member request (Version 2.0)")
    public record RegisterMemberRequestV2(
        @NotBlank @Email @Schema(description = "Email", example = "john@example.com") String email,
        @NotBlank @Schema(description = "First name", example = "John") String firstName,
        @NotBlank @Schema(description = "Last name", example = "Doe") String lastName,
        @Schema(description = "Phone number", example = "+1234567890") String phone,
        @Schema(description = "Address", example = "123 Main St") String address,
        @Schema(description = "Membership type", example = "PREMIUM") MembershipType membershipType
    ) {}

    @Schema(description = "Update member request (Version 2.0)")
    public record UpdateMemberRequestV2(
        @Schema(description = "First name") String firstName,
        @Schema(description = "Last name") String lastName,
        @Schema(description = "Phone number") String phone,
        @Schema(description = "Address") String address,
        @Schema(description = "Membership type") MembershipType membershipType
    ) {}
}
