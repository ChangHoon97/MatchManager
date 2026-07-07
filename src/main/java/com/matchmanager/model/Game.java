package com.matchmanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Game {

    private Long matchId;

    private int gameNumber;

    private Player teamA1;
    private Player teamA2;

    private Player teamB1;
    private Player teamB2;

    private Player waiting1;
    private Player waiting2;

    private Integer team1Score;
    private Integer team2Score;

    public Game(int gameNumber, Player teamA1, Player teamA2, Player teamB1, Player teamB2,
                Player waiting1, Player waiting2) {
        this.gameNumber = gameNumber;
        this.teamA1 = teamA1;
        this.teamA2 = teamA2;
        this.teamB1 = teamB1;
        this.teamB2 = teamB2;
        this.waiting1 = waiting1;
        this.waiting2 = waiting2;
    }

    public String getTeamADisplay() {
        return teamA1.getName() + " / " + teamA2.getName();
    }

    public String getTeamBDisplay() {
        return teamB1.getName() + " / " + teamB2.getName();
    }
}
