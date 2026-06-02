package com.matchmanager.dto;

import jakarta.validation.Valid;
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

    @Data
    public static class PlayerDto {
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
        private String name;

        @NotBlank(message = "급수를 선택해주세요.")
        @Pattern(regexp = "[A-Fa-f]", message = "급수는 A~F 중 하나여야 합니다.")
        private String grade;
    }
}
