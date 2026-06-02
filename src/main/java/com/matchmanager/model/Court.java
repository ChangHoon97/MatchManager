package com.matchmanager.model;

import lombok.Data;
import java.util.List;

@Data
public class Court {

    private int courtNumber;
    private List<Player> players;
    private List<Game> games;

    public Court(int courtNumber, List<Player> players) {
        this.courtNumber = courtNumber;
        this.players = players;
    }
}
