package com.matchmanager.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreUpdateRequestDto {

    @NotNull(message = "경기 ID가 필요합니다.")
    private Long matchId;

    @Min(value = 0, message = "점수는 0 이상이어야 합니다.")
    @Max(value = 99, message = "점수는 99 이하여야 합니다.")
    private Integer team1Score;

    @Min(value = 0, message = "점수는 0 이상이어야 합니다.")
    @Max(value = 99, message = "점수는 99 이하여야 합니다.")
    private Integer team2Score;
}
