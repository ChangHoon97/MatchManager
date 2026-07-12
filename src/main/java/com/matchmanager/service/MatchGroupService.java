package com.matchmanager.service;

import com.matchmanager.dto.DrawDetailDto;
import com.matchmanager.dto.DrawSummaryDto;
import com.matchmanager.dto.SaveDrawRequestDto;
import com.matchmanager.dto.ScoreUpdateRequestDto;
import com.matchmanager.dto.ShareViewDto;
import com.matchmanager.entity.Match;
import com.matchmanager.entity.MatchGroup;
import com.matchmanager.exception.ForbiddenException;
import com.matchmanager.exception.NotFoundException;
import com.matchmanager.exception.UnauthorizedException;
import com.matchmanager.model.Court;
import com.matchmanager.model.Game;
import com.matchmanager.model.Player;
import com.matchmanager.repository.MatchGroupRepository;
import com.matchmanager.repository.MatchRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchGroupService {

    private static final String NOT_DELETED = "N";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

    private final MatchGroupRepository matchGroupRepository;
    private final MatchRepository matchRepository;
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> shareEmitters = new ConcurrentHashMap<>();

    @Transactional
    public Long saveDraw(Long userId, SaveDrawRequestDto req) {
        List<Court> courts = req.getContent();

        for (Court court : courts) {
            for (Game game : court.getGames()) {
                if (game.getTeamA1() == null || game.getTeamA2() == null
                        || game.getTeamB1() == null || game.getTeamB2() == null) {
                    throw new IllegalArgumentException("모든 게임에 4명의 선수가 배정되어야 저장할 수 있습니다.");
                }
            }
        }

        int totalPlayers = (int) courts.stream()
                .flatMap(c -> c.getPlayers().stream())
                .map(Player::getName)
                .distinct()
                .count();

        MatchGroup group = new MatchGroup(req.getTitle(), totalPlayers, req.getCourtCount(), req.getGamesPerPlayer(), userId);
        matchGroupRepository.save(group);

        List<Match> matches = new ArrayList<>();
        for (Court court : courts) {
            for (Game game : court.getGames()) {
                Match match = new Match();
                match.setMatchGroupId(group.getId());
                match.setCourtNo(court.getCourtNumber());
                match.setRoundNo(game.getGameNumber());
                fillPlayer(match, 1, game.getTeamA1());
                fillPlayer(match, 2, game.getTeamA2());
                fillPlayer(match, 3, game.getTeamB1());
                fillPlayer(match, 4, game.getTeamB2());
                match.setRegId(userId);
                match.setModId(userId);
                matches.add(match);
            }
        }
        matchRepository.saveAll(matches);

        return group.getId();
    }

    public List<DrawSummaryDto> listMine(Long userId) {
        return matchGroupRepository.findByRegIdAndDelYnOrderByRegDateDesc(userId, NOT_DELETED).stream()
                .map(g -> new DrawSummaryDto(
                        g.getId(), g.getTitle(), g.getTotalPlayers(), g.getCourtCount(), g.getGameCount(),
                        g.getShareToken() != null, g.getRegDate()))
                .collect(Collectors.toList());
    }

    public DrawDetailDto getDetail(Long groupId, Long userId) {
        MatchGroup group = matchGroupRepository.findByIdAndDelYn(groupId, NOT_DELETED)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 대진표입니다."));
        if (!group.getRegId().equals(userId)) {
            throw new ForbiddenException("본인이 저장한 대진표만 조회할 수 있습니다.");
        }

        List<Court> content = reconstructCourts(groupId);
        return new DrawDetailDto(group.getId(), group.getTitle(), group.getTotalPlayers(),
                group.getCourtCount(), group.getGameCount(), content, true, group.getShareToken() != null);
    }

    @Transactional
    public void deleteDraw(Long groupId, Long userId) {
        MatchGroup group = matchGroupRepository.findByIdAndDelYn(groupId, NOT_DELETED)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 대진표입니다."));
        if (!group.getRegId().equals(userId)) {
            throw new ForbiddenException("본인이 저장한 대진표만 삭제할 수 있습니다.");
        }
        group.setDelYn("Y");
        matchGroupRepository.save(group);
    }

    @Transactional
    public void updateScores(Long groupId, Long userId, List<ScoreUpdateRequestDto> scores) {
        MatchGroup group = matchGroupRepository.findByIdAndDelYn(groupId, NOT_DELETED)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 대진표입니다."));
        if (!group.getRegId().equals(userId)) {
            throw new ForbiddenException("본인이 저장한 대진표의 점수만 수정할 수 있습니다.");
        }

        List<Match> matches = matchRepository.findByMatchGroupIdAndDelYnOrderByCourtNoAscRoundNoAsc(groupId, NOT_DELETED);
        Map<Long, Match> byId = matches.stream()
                .collect(Collectors.toMap(Match::getId, m -> m));

        List<Match> updatedMatches = new ArrayList<>();
        for (ScoreUpdateRequestDto score : scores) {
            Match match = byId.get(score.getMatchId());
            if (match == null) {
                throw new NotFoundException("해당 대진표에 포함되지 않은 경기입니다.");
            }
            match.setTeam1Score(score.getTeam1Score());
            match.setTeam2Score(score.getTeam2Score());
            match.setModId(userId);
            updatedMatches.add(match);
        }

        matchRepository.saveAll(updatedMatches);
        notifyScoresUpdatedAfterCommit(group.getShareToken(), group.getId());
    }

    @Transactional
    public String createShare(Long groupId, Long userId, String rawPassword) {
        MatchGroup group = matchGroupRepository.findByIdAndDelYn(groupId, NOT_DELETED)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 대진표입니다."));
        if (!group.getRegId().equals(userId)) {
            throw new ForbiddenException("본인이 저장한 대진표만 공유할 수 있습니다.");
        }

        String token = group.getShareToken() != null ? group.getShareToken() : generateShareToken();
        group.setShareToken(token);
        group.setPassword(rawPassword);
        matchGroupRepository.save(group);
        return token;
    }

    public Map<String, Object> getShareInfo(Long groupId, Long userId) {
        MatchGroup group = matchGroupRepository.findByIdAndDelYn(groupId, NOT_DELETED)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 대진표입니다."));
        if (!group.getRegId().equals(userId)) {
            throw new ForbiddenException("본인이 저장한 대진표만 조회할 수 있습니다.");
        }
        if (group.getShareToken() == null) {
            throw new NotFoundException("아직 공유되지 않은 대진표입니다.");
        }
        return Map.of(
                "token", group.getShareToken(),
                "shareUrl", "/share/" + group.getShareToken(),
                "password", group.getPassword()
        );
    }

    @Transactional
    public void stopShare(Long groupId, Long userId) {
        MatchGroup group = matchGroupRepository.findByIdAndDelYn(groupId, NOT_DELETED)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 대진표입니다."));
        if (!group.getRegId().equals(userId)) {
            throw new ForbiddenException("본인이 저장한 대진표만 공유 중단할 수 있습니다.");
        }

        String oldToken = group.getShareToken();
        group.setShareToken(null);
        group.setPassword(null);
        matchGroupRepository.save(group);
        closeShareEmittersAfterCommit(oldToken);
    }

    public ShareViewDto getShareView(String token, HttpSession session) {
        MatchGroup group = matchGroupRepository.findByShareTokenAndDelYn(token, NOT_DELETED)
                .orElseThrow(() -> new NotFoundException("존재하지 않거나 만료된 링크입니다."));

        boolean unlocked = Boolean.TRUE.equals(session.getAttribute(shareSessionKey(token)));
        List<Court> content = unlocked ? reconstructCourts(group.getId()) : null;
        return new ShareViewDto(group.getTitle(), true, unlocked,
                unlocked ? group.getTotalPlayers() : 0,
                unlocked ? group.getCourtCount() : 0,
                content);
    }

    public ShareViewDto unlockShare(String token, String rawPassword, HttpSession session) {
        MatchGroup group = matchGroupRepository.findByShareTokenAndDelYn(token, NOT_DELETED)
                .orElseThrow(() -> new NotFoundException("존재하지 않거나 만료된 링크입니다."));

        if (group.getPassword() == null || !group.getPassword().equals(rawPassword)) {
            throw new UnauthorizedException("비밀번호가 올바르지 않습니다.");
        }

        session.setAttribute(shareSessionKey(token), Boolean.TRUE);
        List<Court> content = reconstructCourts(group.getId());
        return new ShareViewDto(group.getTitle(), true, true,
                group.getTotalPlayers(), group.getCourtCount(), content);
    }

    public SseEmitter subscribeShareEvents(String token, HttpSession session) {
        matchGroupRepository.findByShareTokenAndDelYn(token, NOT_DELETED)
                .orElseThrow(() -> new NotFoundException("존재하지 않거나 만료된 링크입니다."));
        if (!Boolean.TRUE.equals(session.getAttribute(shareSessionKey(token)))) {
            throw new UnauthorizedException("비밀번호 확인 후 실시간 갱신을 사용할 수 있습니다.");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        shareEmitters.computeIfAbsent(token, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeShareEmitter(token, emitter));
        emitter.onTimeout(() -> removeShareEmitter(token, emitter));
        emitter.onError(ex -> removeShareEmitter(token, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            removeShareEmitter(token, emitter);
        }

        return emitter;
    }

    private String shareSessionKey(String token) {
        return "share_unlocked_" + token;
    }

    private void notifyScoresUpdatedAfterCommit(String shareToken, Long groupId) {
        if (shareToken == null) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notifyScoresUpdated(shareToken, groupId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifyScoresUpdated(shareToken, groupId);
            }
        });
    }

    private void notifyScoresUpdated(String shareToken, Long groupId) {
        List<SseEmitter> emitters = shareEmitters.getOrDefault(shareToken, new CopyOnWriteArrayList<>());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("scores-updated")
                        .data(Map.of("matchGroupId", groupId)));
            } catch (IOException | IllegalStateException e) {
                removeShareEmitter(shareToken, emitter);
            }
        }
    }

    private void removeShareEmitter(String token, SseEmitter emitter) {
        List<SseEmitter> emitters = shareEmitters.get(token);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            shareEmitters.remove(token);
        }
    }

    private void closeShareEmitters(String token) {
        if (token == null) return;
        List<SseEmitter> emitters = shareEmitters.remove(token);
        if (emitters == null) return;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // Already closed by the client or container.
            }
        }
    }

    private void closeShareEmittersAfterCommit(String token) {
        if (token == null) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            closeShareEmitters(token);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                closeShareEmitters(token);
            }
        });
    }

    private String generateShareToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void fillPlayer(Match match, int slot, Player player) {
        switch (slot) {
            case 1 -> {
                match.setPlayer1Name(player.getName());
                match.setPlayer1Grade(player.getGrade());
                match.setPlayer1Gender(player.getGender());
                match.setPlayer1Age(player.getAge());
            }
            case 2 -> {
                match.setPlayer2Name(player.getName());
                match.setPlayer2Grade(player.getGrade());
                match.setPlayer2Gender(player.getGender());
                match.setPlayer2Age(player.getAge());
            }
            case 3 -> {
                match.setPlayer3Name(player.getName());
                match.setPlayer3Grade(player.getGrade());
                match.setPlayer3Gender(player.getGender());
                match.setPlayer3Age(player.getAge());
            }
            case 4 -> {
                match.setPlayer4Name(player.getName());
                match.setPlayer4Grade(player.getGrade());
                match.setPlayer4Gender(player.getGender());
                match.setPlayer4Age(player.getAge());
            }
            default -> throw new IllegalStateException("잘못된 선수 슬롯: " + slot);
        }
    }

    private List<Court> reconstructCourts(Long groupId) {
        List<Match> matches = matchRepository.findByMatchGroupIdAndDelYnOrderByCourtNoAscRoundNoAsc(groupId, NOT_DELETED);

        Map<Integer, List<Match>> byCourtNo = new LinkedHashMap<>();
        for (Match m : matches) {
            byCourtNo.computeIfAbsent(m.getCourtNo(), k -> new ArrayList<>()).add(m);
        }

        List<Court> courts = new ArrayList<>();
        for (Map.Entry<Integer, List<Match>> entry : byCourtNo.entrySet()) {
            List<Game> games = new ArrayList<>();
            LinkedHashSet<Player> playersInCourt = new LinkedHashSet<>();

            for (Match m : entry.getValue()) {
                Player p1 = toPlayer(m.getPlayer1Name(), m.getPlayer1Grade(), m.getPlayer1Gender(), m.getPlayer1Age());
                Player p2 = toPlayer(m.getPlayer2Name(), m.getPlayer2Grade(), m.getPlayer2Gender(), m.getPlayer2Age());
                Player p3 = toPlayer(m.getPlayer3Name(), m.getPlayer3Grade(), m.getPlayer3Gender(), m.getPlayer3Age());
                Player p4 = toPlayer(m.getPlayer4Name(), m.getPlayer4Grade(), m.getPlayer4Gender(), m.getPlayer4Age());

                Game game = new Game(m.getRoundNo(), p1, p2, p3, p4, null, null);
                game.setMatchId(m.getId());
                game.setTeam1Score(m.getTeam1Score());
                game.setTeam2Score(m.getTeam2Score());
                games.add(game);
                playersInCourt.add(p1);
                playersInCourt.add(p2);
                playersInCourt.add(p3);
                playersInCourt.add(p4);
            }

            Court court = new Court(entry.getKey(), new ArrayList<>(playersInCourt));
            court.setGames(games);
            courts.add(court);
        }

        return courts;
    }

    private Player toPlayer(String name, String grade, String gender, Integer age) {
        return new Player(name, grade, 50, gender, age != null ? age : 0);
    }
}
