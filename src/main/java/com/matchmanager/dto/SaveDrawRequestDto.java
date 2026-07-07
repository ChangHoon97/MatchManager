package com.matchmanager.dto;

import com.matchmanager.model.Court;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SaveDrawRequestDto {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @NotEmpty(message = "저장할 대진표 내용이 없습니다.")
    @Valid
    private List<Court> content;

    private int courtCount;

    private int gamesPerPlayer;
}
