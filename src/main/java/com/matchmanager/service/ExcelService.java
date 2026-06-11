package com.matchmanager.service;

import com.matchmanager.dto.DrawRequestDto;
import com.matchmanager.dto.ExcelUploadResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelService {

    // ===================== 템플릿 생성 =====================

    public byte[] createTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("선수목록");

            // 헤더 스타일
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);

            // 데이터 스타일 (중앙 정렬)
            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 1행: 헤더
            Row header = sheet.createRow(0);
            String[] titles = {"선수명", "등급 (A~F)", "수치 (0~100)"};
            for (int i = 0; i < titles.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(titles[i]);
                cell.setCellStyle(headerStyle);
            }

            // 샘플 데이터 3행
            Object[][] samples = {
                    {"홍길동", "A", 85},
                    {"김영희", "B", 60},
                    {"이철수", "C", 50},
            };
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.createCell(0).setCellValue((String) samples[r][0]);
                Cell gradeCell = row.createCell(1);
                gradeCell.setCellValue((String) samples[r][1]);
                gradeCell.setCellStyle(centerStyle);
                Cell valueCell = row.createCell(2);
                valueCell.setCellValue((int) samples[r][2]);
                valueCell.setCellStyle(centerStyle);
            }

            // 열 너비
            sheet.setColumnWidth(0, 5000);
            sheet.setColumnWidth(1, 3500);
            sheet.setColumnWidth(2, 3500);

            // 등급 열 드롭다운 (B2:B1000)
            DataValidationHelper dvHelper = sheet.getDataValidationHelper();
            CellRangeAddressList gradeRange = new CellRangeAddressList(1, 999, 1, 1);
            DataValidationConstraint gradeConstraint =
                    dvHelper.createExplicitListConstraint(new String[]{"A", "B", "C", "D", "E", "F"});
            DataValidation gradeDv = dvHelper.createValidation(gradeConstraint, gradeRange);
            gradeDv.setShowErrorBox(true);
            gradeDv.createErrorBox("등급 오류", "A~F 중 하나를 입력하세요.");
            sheet.addValidationData(gradeDv);

            // 수치 열 범위 검증 (C2:C1000)
            CellRangeAddressList valueRange = new CellRangeAddressList(1, 999, 2, 2);
            DataValidationConstraint valueConstraint =
                    dvHelper.createIntegerConstraint(
                            DataValidationConstraint.OperatorType.BETWEEN, "0", "100");
            DataValidation valueDv = dvHelper.createValidation(valueConstraint, valueRange);
            valueDv.setShowErrorBox(true);
            valueDv.createErrorBox("수치 오류", "0~100 사이의 정수를 입력하세요.");
            sheet.addValidationData(valueDv);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ===================== 파일 파싱 및 검증 =====================

    public ExcelUploadResult parse(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        List<DrawRequestDto.PlayerDto> players = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            if (lastRow < 1) {
                errors.add("데이터가 없습니다. 2행부터 선수 정보를 입력해주세요.");
                return new ExcelUploadResult(false, errors, players);
            }

            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (isBlankRow(row)) continue;

                String name  = cellString(row, 0);
                String grade = cellString(row, 1).toUpperCase();
                String valStr = cellString(row, 2);
                int rowNum = r + 1;
                boolean ok = true;

                // 선수명 검증
                if (name.isEmpty()) {
                    errors.add(rowNum + "행: 선수명이 비어있습니다.");
                    ok = false;
                } else if (name.length() > 20) {
                    errors.add(rowNum + "행: 선수명은 20자 이하여야 합니다. (입력값: \"" + name + "\")");
                    ok = false;
                }

                // 등급 검증
                if (!grade.matches("[A-F]")) {
                    errors.add(rowNum + "행: 등급은 A~F 중 하나여야 합니다. (입력값: \"" + cellString(row, 1) + "\")");
                    ok = false;
                }

                // 수치 검증
                int value = 50;
                if (valStr.isEmpty()) {
                    errors.add(rowNum + "행: 수치가 비어있습니다. (0~100 정수를 입력하세요)");
                    ok = false;
                } else {
                    try {
                        value = Integer.parseInt(valStr);
                        if (value < 0 || value > 100) {
                            errors.add(rowNum + "행: 수치는 0~100 범위여야 합니다. (입력값: " + value + ")");
                            ok = false;
                        }
                    } catch (NumberFormatException e) {
                        errors.add(rowNum + "행: 수치는 정수여야 합니다. (입력값: \"" + valStr + "\")");
                        ok = false;
                    }
                }

                if (ok) {
                    DrawRequestDto.PlayerDto dto = new DrawRequestDto.PlayerDto();
                    dto.setName(name);
                    dto.setGrade(grade);
                    dto.setValue(value);
                    players.add(dto);
                }
            }
        }

        return new ExcelUploadResult(errors.isEmpty(), errors, players);
    }

    // ===================== 내부 유틸 =====================

    private boolean isBlankRow(Row row) {
        if (row == null) return true;
        for (int c = 0; c < 3; c++) {
            if (!cellString(row, c).isEmpty()) return false;
        }
        return true;
    }

    private String cellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";

        // FORMULA 타입은 캐시된 결과 타입으로 재평가
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();

        return switch (type) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }
}
