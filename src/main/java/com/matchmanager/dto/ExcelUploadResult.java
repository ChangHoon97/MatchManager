package com.matchmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExcelUploadResult {
    private boolean success;
    private List<String> errors;
    private List<DrawRequestDto.PlayerDto> players;
}
