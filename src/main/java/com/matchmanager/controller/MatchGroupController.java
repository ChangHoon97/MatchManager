package com.matchmanager.controller;

import com.matchmanager.dto.DrawDetailDto;
import com.matchmanager.dto.DrawSummaryDto;
import com.matchmanager.dto.SaveDrawRequestDto;
import com.matchmanager.dto.ScoreUpdateRequestDto;
import com.matchmanager.dto.ShareCreateRequestDto;
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

    @PutMapping("/{id}/scores")
    public ResponseEntity<Void> updateScores(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id,
                                             @Valid @RequestBody List<ScoreUpdateRequestDto> req) {
        matchGroupService.updateScores(id, principal.getId(), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<Map<String, Object>> share(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody ShareCreateRequestDto req) {
        String token = matchGroupService.createShare(id, principal.getId(), req.getPassword());
        return ResponseEntity.ok(Map.of("token", token, "shareUrl", "/share/" + token));
    }

    @GetMapping("/{id}/share")
    public ResponseEntity<Map<String, Object>> getShare(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable Long id) {
        return ResponseEntity.ok(matchGroupService.getShareInfo(id, principal.getId()));
    }
}
