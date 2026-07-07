package com.matchmanager.controller;

import com.matchmanager.dto.DrawDetailDto;
import com.matchmanager.dto.DrawSummaryDto;
import com.matchmanager.dto.SaveDrawRequestDto;
import com.matchmanager.security.UserPrincipal;
import com.matchmanager.service.MatchGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/draws")
@RequiredArgsConstructor
public class MatchGroupController {

    private final MatchGroupService matchGroupService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody SaveDrawRequestDto req) {
        Long id = matchGroupService.saveDraw(principal.getId(), req);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping
    public ResponseEntity<List<DrawSummaryDto>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(matchGroupService.listMine(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DrawDetailDto> detail(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable Long id) {
        return ResponseEntity.ok(matchGroupService.getDetail(id, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id) {
        matchGroupService.deleteDraw(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
