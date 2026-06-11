package com.matchmanager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DrawRequestDto {

    @NotEmpty(message = "선수 목록이 비어 있습니다.")
    @Size(min = 4, message = "최소 4명 이상 입력해주세요.")
    @Valid
    private List<PlayerDto> players;

    @Min(value = 0, message = "코트 수는 0 이상이어야 합니다.")
    private int courtCount = 0; // 0 = 자동 계산

    @Min(value = 0, message = "인당 게임 수는 0 이상이어야 합니다.")
    private int gamesPerPlayer = 0; // 0 = 자동 계산

    @Data
    public static class PlayerDto {
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
        private String name;

        @NotBlank(message = "급수를 선택해주세요.")
        @Pattern(regexp = "[SsA-Fa-f]", message = "급수는 S~F 중 하나여야 합니다.")
        private String grade;

        @Min(value = 0, message = "수치는 0 이상이어야 합니다.")
        @Max(value = 100, message = "수치는 100 이하여야 합니다.")
        private int value = 50;
    }
}
