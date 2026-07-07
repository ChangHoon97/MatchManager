package com.matchmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShareUnlockRequestDto {

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(max = 30, message = "비밀번호는 30자 이하여야 합니다.")
    private String password;
}
