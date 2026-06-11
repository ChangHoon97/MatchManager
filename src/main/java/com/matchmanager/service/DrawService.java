package com.matchmanager.service;

import com.matchmanager.model.Court;
import com.matchmanager.model.Game;
import com.matchmanager.model.Player;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DrawService {

    /**
     * 전체 대진표 생성 메인 메서드
     * 1. 급수 기준으로 정렬
     * 2. 코트별로 배분 (비슷한 실력끼리)
     * 3. 각 코트에서 게임 대진표 생성
     */
    public List<Court> generateDraw(List<Player> players, int requestedCourtCount, int gamesPerPlayer) {
        players.sort(Comparator.comparingInt(Player::getTotalScore).reversed());

        int courtCount;
        if (requestedCourtCount > 0) {
            int minPerCourt = players.size() / requestedCourtCount;
            if (minPerCourt < 4) {
                int maxCourts = players.size() / 4;
                throw new IllegalArgumentException(
                    "코트당 최소 4명이 필요합니다. " +
                    players.size() + "명으로는 최대 " + maxCourts + "개 코트 설정 가능합니다.");
            }
            courtCount = requestedCourtCount;
        } else {
            courtCount = calculateCourtCount(players.size());
        }

        List<List<Player>> courtPlayers = assignPlayersToCourts(players, courtCount);

        List<Court> courts = new ArrayList<>();
        for (int i = 0; i < courtCount; i++) {
            Court court = new Court(i + 1, courtPlayers.get(i));
            List<Game> games = generateGamesForCourt(courtPlayers.get(i), gamesPerPlayer);
            court.setGames(games);
            courts.add(court);
        }

        return courts;
    }

    /**
     * 코트 수 계산
     * 코트당 최소 6명, 최대 8명 기준
     */
    private int calculateCourtCount(int totalPlayers) {
        for (int courts = 1; courts <= 10; courts++) {
            int perCourt = totalPlayers / courts;
            int remainder = totalPlayers % courts;
            if (perCourt >= 6 && perCourt <= 8 && (remainder == 0 || perCourt + 1 <= 8)) {
                return courts;
            }
        }
        return (int) Math.ceil(totalPlayers / 7.0);
    }

    /**
     * 급수 기반으로 코트에 선수 배분
     * 강한 순으로 정렬된 선수를 라운드로빈 방식으로 배분
     * → 각 코트의 평균 실력이 비슷해짐
     */
    private List<List<Player>> assignPlayersToCourts(List<Player> sortedPlayers, int courtCount) {
        List<List<Player>> courts = new ArrayList<>();
        for (int i = 0; i < courtCount; i++) {
            courts.add(new ArrayList<>());
        }

        // 뱀 모양(snake) 배분
        boolean forward = true;
        int courtIndex = 0;
        for (Player player : sortedPlayers) {
            courts.get(courtIndex).add(player);
            if (forward) {
                courtIndex++;
                if (courtIndex >= courtCount) {
                    courtIndex = courtCount - 1;
                    forward = false;
                }
            } else {
                courtIndex--;
                if (courtIndex < 0) {
                    courtIndex = 0;
                    forward = true;
                }
            }
        }

        return courts;
    }

    /**
     * 코트 내 게임 대진표 생성
     * - 6명: 2게임 (매 게임 2명 대기, 로테이션)
     * - 7명: 3게임 (매 게임 3명 대기 또는 혼합)
     * - 8명: 2게임 (4명 vs 4명 / 2게임 모두 다른 조합)
     *
     * 원칙: 같은 파트너 최대한 안 겹치게
     */
    private List<Game> generateGamesForCourt(List<Player> players, int gamesPerPlayer) {
        int size = players.size();
        int gameCount = (gamesPerPlayer > 0)
            ? (int) Math.ceil((double) size * gamesPerPlayer / 4)
            : (size <= 6 ? 2 : 3);

        Map<String, Integer> partnerCount = new HashMap<>();

        List<Game> games = new ArrayList<>();
        List<Integer> waitingIndices = new ArrayList<>();

        for (int g = 0; g < gameCount; g++) {
            Game game = createGame(players, g, partnerCount, waitingIndices, size);
            game.setGameNumber(g + 1);
            games.add(game);
        }

        return games;
    }

    private Game createGame(List<Player> players, int gameIndex,
                             Map<String, Integer> partnerCount,
                             List<Integer> prevWaiting, int size) {

        List<Integer> activeIndices = new ArrayList<>();
        List<Integer> newWaiting = new ArrayList<>();

        if (gameIndex == 0) {
            for (int i = 0; i < size; i++) {
                if (i < 4) activeIndices.add(i);
                else newWaiting.add(i);
            }
        } else {
            List<Integer> candidates = new ArrayList<>(prevWaiting);
            List<Integer> others = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                if (!prevWaiting.contains(i)) others.add(i);
            }
            Collections.shuffle(others);
            candidates.addAll(others);
            for (int i = 0; i < size; i++) {
                if (i < 4) activeIndices.add(candidates.get(i));
                else newWaiting.add(candidates.get(i));
            }
        }

        prevWaiting.clear();
        prevWaiting.addAll(newWaiting);

        int[] bestSplit = findBestSplit(players, activeIndices, partnerCount);

        Player a1 = players.get(bestSplit[0]);
        Player a2 = players.get(bestSplit[1]);
        Player b1 = players.get(bestSplit[2]);
        Player b2 = players.get(bestSplit[3]);

        updatePartnerCount(partnerCount, a1, a2);
        updatePartnerCount(partnerCount, b1, b2);

        Game game = new Game();
        game.setTeamA1(a1);
        game.setTeamA2(a2);
        game.setTeamB1(b1);
        game.setTeamB2(b2);

        if (!newWaiting.isEmpty()) game.setWaiting1(players.get(newWaiting.get(0)));
        if (newWaiting.size() > 1) game.setWaiting2(players.get(newWaiting.get(1)));

        return game;
    }

    /**
     * 3가지 팀 분할 중 최적 선택
     * 우선순위 1: 파트너 반복 최소화
     * 우선순위 2: 양 팀 합산 점수 차 최소화 (±100 이내 목표)
     */
    private int[] findBestSplit(List<Player> players, List<Integer> indices,
                                 Map<String, Integer> partnerCount) {
        int i0 = indices.get(0), i1 = indices.get(1),
            i2 = indices.get(2), i3 = indices.get(3);

        int[][] splits = {
            {i0, i1, i2, i3},
            {i0, i2, i1, i3},
            {i0, i3, i1, i2}
        };

        int bestScore = Integer.MAX_VALUE;
        int[] bestSplit = splits[0];

        for (int[] split : splits) {
            int partnerScore = getPartnerScore(players, split[0], split[1], partnerCount)
                             + getPartnerScore(players, split[2], split[3], partnerCount);
            int teamA = players.get(split[0]).getTotalScore() + players.get(split[1]).getTotalScore();
            int teamB = players.get(split[2]).getTotalScore() + players.get(split[3]).getTotalScore();
            int balanceScore = Math.abs(teamA - teamB);
            // 파트너 반복 1회 = 점수차 500에 해당하는 패널티
            int score = partnerScore * 500 + balanceScore;
            if (score < bestScore) {
                bestScore = score;
                bestSplit = split;
            }
        }

        return bestSplit;
    }

    private int getPartnerScore(List<Player> players, int i, int j,
                                 Map<String, Integer> partnerCount) {
        String key = makePartnerKey(players.get(i).getName(), players.get(j).getName());
        return partnerCount.getOrDefault(key, 0);
    }

    private void updatePartnerCount(Map<String, Integer> partnerCount, Player a, Player b) {
        String key = makePartnerKey(a.getName(), b.getName());
        partnerCount.merge(key, 1, Integer::sum);
    }

    private String makePartnerKey(String name1, String name2) {
        return name1.compareTo(name2) < 0
            ? name1 + "|" + name2
            : name2 + "|" + name1;
    }
}
