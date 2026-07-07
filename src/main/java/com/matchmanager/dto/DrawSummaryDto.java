package com.matchmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DrawSummaryDto {
    private Long id;
    private String title;
    private int totalPlayers;
    private int courtCount;
    private int gameCount;
    private boolean hasShare;
    private LocalDateTime regDate;
}
