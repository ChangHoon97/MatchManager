package com.matchmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShareCreateRequestDto {

    @NotBlank(message = "공유 비밀번호를 입력해주세요.")
    @Size(min = 4, max = 30, message = "공유 비밀번호는 4자 이상 30자 이하여야 합니다.")
    private String password;
}
