package com.matchmanager.controller;

import com.matchmanager.dto.DrawRequestDto;
import com.matchmanager.dto.ExcelUploadResult;
import com.matchmanager.model.Court;
import com.matchmanager.model.Player;
import com.matchmanager.service.DrawService;
import com.matchmanager.service.ExcelService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class DrawController {

    @Autowired
    private DrawService drawService;

    @Autowired
    private ExcelService excelService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/api/draw")
    @ResponseBody
    public List<Court> generateDrawApi(@Valid @RequestBody DrawRequestDto request) {
        List<Player> players = new ArrayList<>();
        for (DrawRequestDto.PlayerDto dto : request.getPlayers()) {
            players.add(new Player(dto.getName(), dto.getGrade(), dto.getValue(), dto.getGender()));
        }
        return drawService.generateDraw(players, request.getCourtCount(), request.getGamesPerPlayer());
    }

    @GetMapping("/api/excel-template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] bytes = excelService.createTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''%EC%84%A0%EC%88%98_%EC%96%91%EC%8B%9D.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping("/api/upload-excel")
    @ResponseBody
    public ResponseEntity<ExcelUploadResult> uploadExcel(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ExcelUploadResult(false, List.of("파일이 비어있습니다."), List.of()));
        }

        String filename = file.getOriginalFilename();
        if (filename == null
                || (!filename.toLowerCase().endsWith(".xlsx")
                    && !filename.toLowerCase().endsWith(".xls"))) {
            return ResponseEntity.badRequest().body(
                    new ExcelUploadResult(false,
                            List.of("엑셀 파일(.xlsx, .xls)만 업로드 가능합니다."), List.of()));
        }

        try {
            ExcelUploadResult result = excelService.parse(file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    new ExcelUploadResult(false,
                            List.of("파일을 읽는 중 오류가 발생했습니다: " + e.getMessage()), List.of()));
        }
    }
}
