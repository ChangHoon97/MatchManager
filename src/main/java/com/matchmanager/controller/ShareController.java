package com.matchmanager.controller;

import com.matchmanager.dto.ShareUnlockRequestDto;
import com.matchmanager.dto.ShareViewDto;
import com.matchmanager.service.MatchGroupService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final MatchGroupService matchGroupService;

    @GetMapping("/{token}")
    public ResponseEntity<ShareViewDto> view(@PathVariable String token, HttpSession session) {
        return ResponseEntity.ok(matchGroupService.getShareView(token, session));
    }

    @PostMapping("/{token}/unlock")
    public ResponseEntity<ShareViewDto> unlock(@PathVariable String token,
                                                @Valid @RequestBody ShareUnlockRequestDto req,
                                                HttpSession session) {
        return ResponseEntity.ok(matchGroupService.unlockShare(token, req.getPassword(), session));
    }
}
