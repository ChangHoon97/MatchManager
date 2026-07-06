package com.matchmanager.service;

import com.matchmanager.model.Court;
import com.matchmanager.model.Game;
import com.matchmanager.model.Player;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DrawService {

    public List<Court> generateDraw(List<Player> players) {
        return generateDraw(players, 0, 0);
    }

    public List<Court> generateDraw(List<Player> players, int requestedCourtCount, int gamesPerPlayer) {
        players.sort(Comparator.comparingInt(Player::getTotalScore).reversed());

        if (requestedCourtCount > 0 && players.size() / requestedCourtCount < 4) {
            int maxCourts = players.size() / 4;
            throw new IllegalArgumentException(
                "코트당 최소 4명이 필요합니다. " +
                players.size() + "명으로는 최대 " + maxCourts + "개 코트 설정 가능합니다.");
        }

        List<List<Player>> courtPlayers = assignPlayersToCourts(players, requestedCourtCount);

        List<Court> courts = new ArrayList<>();
        for (int i = 0; i < courtPlayers.size(); i++) {
            Court court = new Court(i + 1, courtPlayers.get(i));
            List<Game> games = generateGamesForCourt(courtPlayers.get(i), gamesPerPlayer);
            court.setGames(games);
            courts.add(court);
        }
        return courts;
    }

    private List<List<Player>> assignPlayersToCourts(List<Player> sortedPlayers, int requestedCourtCount) {
        int total = sortedPlayers.size();

        if (requestedCourtCount > 0) {
            int base  = total / requestedCourtCount;
            int extra = total % requestedCourtCount;
            List<Integer> sizes = new ArrayList<>();
            for (int i = 0; i < requestedCourtCount; i++)
                sizes.add(i < extra ? base + 1 : base);
            return fillCourtsWithSizes(sortedPlayers, sizes);
        }

        int estimatedCourts = Math.max(1, (int) Math.ceil((double) total / 8.0));
        int target = (int) Math.max(4, Math.round((double) total / estimatedCourts));
        return fillCourtsFromGrades(sortedPlayers, target);
    }

    /**
     * 코트별 목표 인원(sizes)을 받아 급수 순서대로 carry 버퍼를 채워 코트 확정.
     * 예) D(4)+E(14)+F(8), sizes=[7,7,6,6] → [D4+E3:7, E7:7, E4+F2:6, F6:6]
     */
    private List<List<Player>> fillCourtsWithSizes(List<Player> sortedPlayers, List<Integer> sizes) {
        String[] gradeOrder = {"S", "A", "B", "C", "D", "E", "F"};
        Map<String, List<Player>> byGrade = new LinkedHashMap<>();
        for (String g : gradeOrder) byGrade.put(g, new ArrayList<>());
        for (Player p : sortedPlayers) byGrade.get(p.getGrade().toUpperCase()).add(p);

        List<List<Player>> courts = new ArrayList<>();
        List<Player> carry = new ArrayList<>();
        int courtIdx = 0;

        for (String g : gradeOrder) {
            List<Player> gradePool = byGrade.get(g);
            if (gradePool.isEmpty()) continue;
            carry.addAll(gradePool);
            while (courtIdx < sizes.size() && carry.size() >= sizes.get(courtIdx)) {
                int sz = sizes.get(courtIdx++);
                courts.add(new ArrayList<>(carry.subList(0, sz)));
                carry = new ArrayList<>(carry.subList(sz, carry.size()));
            }
        }

        if (!carry.isEmpty()) {
            if (!courts.isEmpty()) courts.get(courts.size() - 1).addAll(carry);
            else courts.add(carry);
        }

        return courts;
    }

    /**
     * Snake 방식 배분: 총점 내림차순으로 정렬된 선수를 코트에 순환 배정.
     * 짝수 라운드는 좌→우, 홀수 라운드는 우→좌로 배정해 코트 간 평균 급수를 균등화.
     */
    private List<List<Player>> fillCourtsFromGrades(List<Player> sortedPlayers, int target) {
        int numCourts = (int) Math.ceil((double) sortedPlayers.size() / target);
        List<List<Player>> courts = new ArrayList<>();
        for (int i = 0; i < numCourts; i++) courts.add(new ArrayList<>());

        for (int i = 0; i < sortedPlayers.size(); i++) {
            int round = i / numCourts;
            int pos = i % numCourts;
            int courtIdx = (round % 2 == 0) ? pos : (numCourts - 1 - pos);
            courts.get(courtIdx).add(sortedPlayers.get(i));
        }

        return courts;
    }

    /**
     * 코트 내 게임 생성
     * gamesPerPlayer > 0: 1인당 목표 게임 수 기반으로 총 게임 수 역산
     * gamesPerPlayer == 0: 자동 (6명 이하 2게임, 7명 이상 3게임)
     */
    private List<Game> generateGamesForCourt(List<Player> players, int gamesPerPlayer) {
        int size = players.size();
        int gameCount = (gamesPerPlayer > 0)
            ? (int) Math.ceil((double) size * gamesPerPlayer / 4)
            : (size <= 6 ? 2 : 3);

        Map<String, Integer> partnerCount = new HashMap<>();
        int[] gamesPlayed = new int[size];
        List<Game> games = new ArrayList<>();
        List<Integer> waitingIndices = new ArrayList<>();

        for (int g = 0; g < gameCount; g++) {
            Game game = createGame(players, g, partnerCount, waitingIndices, gamesPlayed, size);
            game.setGameNumber(g + 1);
            games.add(game);
        }
        return games;
    }

    private Game createGame(List<Player> players, int gameIndex,
                             Map<String, Integer> partnerCount,
                             List<Integer> prevWaiting, int[] gamesPlayed, int size) {
        List<Integer> activeIndices = new ArrayList<>();
        List<Integer> newWaiting = new ArrayList<>();

        if (gameIndex == 0) {
            // 급수 범위 전체에서 균등 간격으로 4명 선발 → 상하위 혼합
            Set<Integer> selected = new LinkedHashSet<>();
            for (int i = 0; i < 4; i++) {
                selected.add((int) Math.round((double) i * (size - 1) / 3));
            }
            for (int i = 0; i < size; i++) {
                if (selected.contains(i)) activeIndices.add(i);
                else newWaiting.add(i);
            }
        } else {
            // 직전에 쉰 사람은 무조건 출전, 나머지는 "적게 뛴 사람" 우선 (동률은 무작위)으로 채움
            List<Integer> others = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                if (!prevWaiting.contains(i)) others.add(i);
            }
            Collections.shuffle(others);
            others.sort(Comparator.comparingInt(i -> gamesPlayed[i]));

            List<Integer> candidates = new ArrayList<>(prevWaiting);
            candidates.addAll(others);
            for (int i = 0; i < size; i++) {
                if (i < 4) activeIndices.add(candidates.get(i));
                else newWaiting.add(candidates.get(i));
            }
        }

        prevWaiting.clear();
        prevWaiting.addAll(newWaiting);
        for (int idx : activeIndices) gamesPlayed[idx]++;

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
     * 우선순위 2: 양 팀 합산 점수 차 최소화
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
