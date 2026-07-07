package com.matchmanager.service;

import com.matchmanager.dto.DrawDetailDto;
import com.matchmanager.dto.DrawSummaryDto;
import com.matchmanager.dto.SaveDrawRequestDto;
import com.matchmanager.entity.Match;
import com.matchmanager.entity.MatchGroup;
import com.matchmanager.exception.ForbiddenException;
import com.matchmanager.exception.NotFoundException;
import com.matchmanager.model.Court;
import com.matchmanager.model.Game;
import com.matchmanager.model.Player;
import com.matchmanager.repository.MatchGroupRepository;
import com.matchmanager.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchGroupService {

    private static final String NOT_DELETED = "N";

    private final MatchGroupRepository matchGroupRepository;
    private final MatchRepository matchRepository;

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
