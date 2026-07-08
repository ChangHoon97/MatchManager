package com.matchmanager.service;

import com.matchmanager.dto.DrawDetailDto;
import com.matchmanager.dto.ScoreUpdateRequestDto;
import com.matchmanager.dto.ShareViewDto;
import com.matchmanager.entity.Match;
import com.matchmanager.entity.MatchGroup;
import com.matchmanager.exception.ForbiddenException;
import com.matchmanager.exception.UnauthorizedException;
import com.matchmanager.repository.MatchGroupRepository;
import com.matchmanager.repository.MatchRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchGroupServiceTest {

    private final MatchGroupRepository matchGroupRepository = mock(MatchGroupRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final MatchGroupService service = new MatchGroupService(matchGroupRepository, matchRepository);

    @Test
    void lockedShareViewExposesOnlyTitle() {
        MatchGroup group = sharedGroup();
        HttpSession session = mock(HttpSession.class);
        when(matchGroupRepository.findByShareTokenAndDelYn("token123", "N")).thenReturn(Optional.of(group));

        ShareViewDto view = service.getShareView("token123", session);

        assertThat(view.getTitle()).isEqualTo("화요 번개");
        assertThat(view.isRequiresPassword()).isTrue();
        assertThat(view.isUnlocked()).isFalse();
        assertThat(view.getTotalPlayers()).isZero();
        assertThat(view.getCourtCount()).isZero();
        assertThat(view.getContent()).isNull();
        verify(matchRepository, never()).findByMatchGroupIdAndDelYnOrderByCourtNoAscRoundNoAsc(1L, "N");
    }

    @Test
    void unlockShareUsesPlainPassword() {
        MatchGroup group = sharedGroup();
        HttpSession session = mock(HttpSession.class);
        when(matchGroupRepository.findByShareTokenAndDelYn("token123", "N")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.unlockShare("token123", "wrong", session))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("비밀번호가 올바르지 않습니다.");

        verify(session, never()).setAttribute(anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateScoresOnlyForOwner() {
        MatchGroup group = sharedGroup();
        when(matchGroupRepository.findByIdAndDelYn(1L, "N")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.updateScores(1L, 8L, List.of(score(10L, 21, 18))))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("본인이 저장한 대진표의 점수만 수정할 수 있습니다.");
    }

    @Test
    void updateScoresStoresScoresOnGroupMatches() {
        MatchGroup group = sharedGroup();
        Match match = match(10L);
        when(matchGroupRepository.findByIdAndDelYn(1L, "N")).thenReturn(Optional.of(group));
        when(matchRepository.findByMatchGroupIdAndDelYnOrderByCourtNoAscRoundNoAsc(1L, "N"))
                .thenReturn(List.of(match));

        service.updateScores(1L, 7L, List.of(score(10L, 21, 18)));

        assertThat(match.getTeam1Score()).isEqualTo(21);
        assertThat(match.getTeam2Score()).isEqualTo(18);
        assertThat(match.getModId()).isEqualTo(7L);
        verify(matchRepository).saveAll(List.of(match));
    }

    @Test
    void stopShareClearsShareTokenAndPassword() {
        MatchGroup group = sharedGroup();
        when(matchGroupRepository.findByIdAndDelYn(1L, "N")).thenReturn(Optional.of(group));

        service.stopShare(1L, 7L);

        assertThat(group.getShareToken()).isNull();
        assertThat(group.getPassword()).isNull();
        verify(matchGroupRepository).save(group);
    }

    @Test
    void stopShareOnlyForOwner() {
        MatchGroup group = sharedGroup();
        when(matchGroupRepository.findByIdAndDelYn(1L, "N")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.stopShare(1L, 8L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("본인이 저장한 대진표만 공유 중단할 수 있습니다.");
    }

    @Test
    void subscribeShareEventsRequiresUnlockedSession() {
        MatchGroup group = sharedGroup();
        HttpSession session = mock(HttpSession.class);
        when(matchGroupRepository.findByShareTokenAndDelYn("token123", "N")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.subscribeShareEvents("token123", session))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("비밀번호 확인 후 실시간 갱신을 사용할 수 있습니다.");
    }

    @Test
    void updateScoresWithUnlockedShareSubscriberCompletes() {
        MatchGroup group = sharedGroup();
        Match match = match(10L);
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("share_unlocked_token123")).thenReturn(Boolean.TRUE);
        when(matchGroupRepository.findByShareTokenAndDelYn("token123", "N")).thenReturn(Optional.of(group));
        when(matchGroupRepository.findByIdAndDelYn(1L, "N")).thenReturn(Optional.of(group));
        when(matchRepository.findByMatchGroupIdAndDelYnOrderByCourtNoAscRoundNoAsc(1L, "N"))
                .thenReturn(List.of(match));

        service.subscribeShareEvents("token123", session);
        service.updateScores(1L, 7L, List.of(score(10L, 21, 18)));

        assertThat(match.getTeam1Score()).isEqualTo(21);
        assertThat(match.getTeam2Score()).isEqualTo(18);
    }

    @Test
    void detailIncludesMatchIdAndScores() {
        MatchGroup group = sharedGroup();
        Match match = match(10L);
        match.setTeam1Score(21);
        match.setTeam2Score(18);
        when(matchGroupRepository.findByIdAndDelYn(1L, "N")).thenReturn(Optional.of(group));
        when(matchRepository.findByMatchGroupIdAndDelYnOrderByCourtNoAscRoundNoAsc(1L, "N"))
                .thenReturn(List.of(match));

        DrawDetailDto detail = service.getDetail(1L, 7L);

        assertThat(detail.getContent().get(0).getGames().get(0).getMatchId()).isEqualTo(10L);
        assertThat(detail.getContent().get(0).getGames().get(0).getTeam1Score()).isEqualTo(21);
        assertThat(detail.getContent().get(0).getGames().get(0).getTeam2Score()).isEqualTo(18);
    }

    private MatchGroup sharedGroup() {
        MatchGroup group = new MatchGroup("화요 번개", 12, 2, 0, 7L);
        group.setId(1L);
        group.setShareToken("token123");
        group.setPassword("1234");
        return group;
    }

    private ScoreUpdateRequestDto score(Long matchId, Integer team1Score, Integer team2Score) {
        ScoreUpdateRequestDto score = new ScoreUpdateRequestDto();
        score.setMatchId(matchId);
        score.setTeam1Score(team1Score);
        score.setTeam2Score(team2Score);
        return score;
    }

    private Match match(Long id) {
        Match match = new Match();
        match.setId(id);
        match.setMatchGroupId(1L);
        match.setCourtNo(1);
        match.setRoundNo(1);
        match.setPlayer1Name("김철수");
        match.setPlayer1Grade("A");
        match.setPlayer1Gender("남");
        match.setPlayer1Age(30);
        match.setPlayer2Name("이영희");
        match.setPlayer2Grade("B");
        match.setPlayer2Gender("여");
        match.setPlayer2Age(40);
        match.setPlayer3Name("박민준");
        match.setPlayer3Grade("C");
        match.setPlayer3Gender("남");
        match.setPlayer3Age(45);
        match.setPlayer4Name("최지은");
        match.setPlayer4Grade("D");
        match.setPlayer4Gender("여");
        match.setPlayer4Age(50);
        return match;
    }
}
