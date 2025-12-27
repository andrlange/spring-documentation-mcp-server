package com.example.library.controller;

import com.example.library.model.Member;
import com.example.library.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * REST API controller for member operations.
 */
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public Collection<Member> getAllMembers() {
        return memberService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberById(@PathVariable Long id) {
        return memberService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Member> searchMembers(@RequestParam String query) {
        return memberService.search(query);
    }

    @GetMapping("/active")
    public List<Member> getActiveMembers() {
        return memberService.findActive();
    }
}
