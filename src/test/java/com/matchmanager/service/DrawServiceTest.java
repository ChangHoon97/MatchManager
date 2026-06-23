package com.matchmanager.service;

import com.matchmanager.model.Court;
import com.matchmanager.model.Game;
import com.matchmanager.model.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class DrawServiceTest {

    private final DrawService service = new DrawService();

    private static List<Player> samplePlayers(int count) {
        String[] grades = {"A", "B", "C", "D", "E", "F"};
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Player("P" + (i + 1), grades[i % grades.length]));
        }
        return list;
    }

    @Nested
    @DisplayName("코트 배분 규칙")
    class CourtAllocation {

        @Test
        @DisplayName("6명 → 1코트 6명")
        void sixPlayers_oneCourt() {
            List<Court> courts = service.generateDraw(samplePlayers(6));
            assertThat(courts).hasSize(1);
            assertThat(courts.get(0).getPlayers()).hasSize(6);
        }

        @Test
        @DisplayName("7명 → 1코트 7명")
        void sevenPlayers_oneCourt() {
            List<Court> courts = service.generateDraw(samplePlayers(7));
            assertThat(courts).hasSize(1);
            assertThat(courts.get(0).getPlayers()).hasSize(7);
        }

        @Test
        @DisplayName("8명 → 1코트 8명")
        void eightPlayers_oneCourt() {
            List<Court> courts = service.generateDraw(samplePlayers(8));
            assertThat(courts).hasSize(1);
            assertThat(courts.get(0).getPlayers()).hasSize(8);
        }

        @Test
        @DisplayName("14명 → 2코트, 각 코트 6~8명")
        void fourteen_twoCourts() {
            List<Court> courts = service.generateDraw(samplePlayers(14));
            assertThat(courts).hasSize(2);
            for (Court c : courts) {
                assertThat(c.getPlayers().size()).isBetween(6, 8);
            }
        }

        @Test
        @DisplayName("16명 → 2코트 각 8명")
        void sixteen_twoCourts() {
            List<Court> courts = service.generateDraw(samplePlayers(16));
            assertThat(courts).hasSize(2);
            for (Court c : courts) {
                assertThat(c.getPlayers()).hasSize(8);
            }
        }

        @Test
        @DisplayName("20명 → 3코트, 각 코트 6~8명")
        void twenty_threeCourts() {
            List<Court> courts = service.generateDraw(samplePlayers(20));
            assertThat(courts).hasSize(3);
            int total = 0;
            for (Court c : courts) {
                assertThat(c.getPlayers().size()).isBetween(6, 8);
                total += c.getPlayers().size();
            }
            assertThat(total).isEqualTo(20);
        }

        @Test
        @DisplayName("모든 선수가 정확히 한 코트에 배정된다")
        void everyPlayerAssignedExactlyOnce() {
            List<Player> players = samplePlayers(20);
            List<Court> courts = service.generateDraw(players);

            Set<String> names = new HashSet<>();
            int total = 0;
            for (Court c : courts) {
                for (Player p : c.getPlayers()) {
                    assertThat(names.add(p.getName()))
                            .as("선수 %s가 중복 배정됨", p.getName())
                            .isTrue();
                    total++;
                }
            }
            assertThat(total).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Snake 분배 균형")
    class SnakeBalance {

        @Test
        @DisplayName("16명 2코트일 때 코트별 평균 급수 차이가 1.0 이내")
        void averageGradeBalanced() {
            List<Court> courts = service.generateDraw(samplePlayers(16));

            double[] averages = courts.stream()
                    .mapToDouble(c -> c.getPlayers().stream()
                            .mapToInt(Player::getGradeValue)
                            .average().orElseThrow())
                    .toArray();

            double max = IntStream.range(0, averages.length).mapToDouble(i -> averages[i]).max().orElseThrow();
            double min = IntStream.range(0, averages.length).mapToDouble(i -> averages[i]).min().orElseThrow();
            assertThat(max - min).isLessThanOrEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("게임 생성")
    class GameGeneration {

        @Test
        @DisplayName("6명 코트는 게임 2개, 게임마다 대기 2명")
        void sixPlayerCourt_twoGames_twoWaiting() {
            List<Court> courts = service.generateDraw(samplePlayers(6));
            List<Game> games = courts.get(0).getGames();
            assertThat(games).hasSize(2);
            for (Game g : games) {
                assertThat(g.getTeamA1()).isNotNull();
                assertThat(g.getTeamA2()).isNotNull();
                assertThat(g.getTeamB1()).isNotNull();
                assertThat(g.getTeamB2()).isNotNull();
                assertThat(g.getWaiting1()).isNotNull();
                assertThat(g.getWaiting2()).isNotNull();
            }
        }

        @Test
        @DisplayName("8명 코트는 게임 3개, 대기 4명")
        void eightPlayerCourt_threeGames() {
            List<Court> courts = service.generateDraw(samplePlayers(8));
            List<Game> games = courts.get(0).getGames();
            assertThat(games).hasSize(3);
        }

        @Test
        @DisplayName("게임 번호는 1부터 순차적으로 매겨진다")
        void gameNumbersSequential() {
            List<Court> courts = service.generateDraw(samplePlayers(8));
            List<Game> games = courts.get(0).getGames();
            for (int i = 0; i < games.size(); i++) {
                assertThat(games.get(i).getGameNumber()).isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("각 게임의 4명 선수는 서로 다르다")
        void teamsHaveDistinctPlayers() {
            List<Court> courts = service.generateDraw(samplePlayers(8));
            for (Game g : courts.get(0).getGames()) {
                Set<String> names = new HashSet<>();
                names.add(g.getTeamA1().getName());
                names.add(g.getTeamA2().getName());
                names.add(g.getTeamB1().getName());
                names.add(g.getTeamB2().getName());
                assertThat(names).hasSize(4);
            }
        }
    }

    @Nested
    @DisplayName("Player.getGradeValue / getTotalScore")
    class GradeValue {

        @Test
        @DisplayName("S=7(최강), A=6, F=1(약), 알 수 없는 등급은 0")
        void gradeMapping() {
            assertThat(new Player("x", "S").getGradeValue()).isEqualTo(7);
            assertThat(new Player("x", "s").getGradeValue()).isEqualTo(7);
            assertThat(new Player("x", "A").getGradeValue()).isEqualTo(6);
            assertThat(new Player("x", "F").getGradeValue()).isEqualTo(1);
            assertThat(new Player("x", "a").getGradeValue()).isEqualTo(6);
            assertThat(new Player("x", "Z").getGradeValue()).isEqualTo(0);
        }

        @Test
        @DisplayName("totalScore: A+100=600(최강), F+0=0(최약), 경계값 A+0=B+100=500")
        void totalScoreMapping() {
            assertThat(new Player("x", "A", 100, "", 0).getTotalScore()).isEqualTo(600);
            assertThat(new Player("x", "F", 0, "", 0).getTotalScore()).isEqualTo(0);
            assertThat(new Player("x", "A", 0, "", 0).getTotalScore()).isEqualTo(500);
            assertThat(new Player("x", "B", 100, "", 0).getTotalScore()).isEqualTo(500);
        }
    }
}
