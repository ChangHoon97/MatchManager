package com.matchmanager.dto;

import com.matchmanager.model.Court;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ShareViewDto {
    private String title;
    private boolean requiresPassword;
    private boolean unlocked;
    private int totalPlayers;
    private int courtCount;
    private List<Court> content;
}
