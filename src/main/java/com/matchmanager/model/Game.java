package com.matchmanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {

    private int gameNumber;

    private Player teamA1;
    private Player teamA2;

    private Player teamB1;
    private Player teamB2;

    private Player waiting1;
    private Player waiting2;

    public String getTeamADisplay() {
        return teamA1.getName() + " / " + teamA2.getName();
    }

    public String getTeamBDisplay() {
        return teamB1.getName() + " / " + teamB2.getName();
    }
}
