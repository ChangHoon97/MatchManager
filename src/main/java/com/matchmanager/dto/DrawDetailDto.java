package com.matchmanager.dto;

import com.matchmanager.model.Court;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DrawDetailDto {
    private Long id;
    private String title;
    private int totalPlayers;
    private int courtCount;
    private int gameCount;
    private List<Court> content;
    private boolean owner;
    private boolean hasShare;
}
