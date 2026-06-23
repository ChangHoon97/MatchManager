package com.matchmanager.service;

import com.matchmanager.dto.DrawRequestDto;
import com.matchmanager.dto.ExcelUploadResult;
import com.matchmanager.model.Court;
import com.matchmanager.model.Game;
import com.matchmanager.model.Player;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ExcelService {

    private static final int[] VALID_AGES = {20, 30, 40, 45, 50, 55, 60, 65};

    // ===================== 선수 입력 템플릿 생성 =====================

    public byte[] createTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("선수목록");

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

            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row header = sheet.createRow(0);
            // 컬럼 순서: 선수명(0) / 나이(1) / 등급(2) / 성별(3) / 수치(4)
            String[] titles = {"선수명", "나이", "등급 (S~F)", "성별 (남/여)", "수치 (0~100)"};
            for (int i = 0; i < titles.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(titles[i]);
                cell.setCellStyle(headerStyle);
            }

            Object[][] samples = {
                    {"홍길동", 30, "A", "남", 85},
                    {"김영희", 40, "B", "여", 60},
                    {"이철수", 45, "C", "남", 50},
            };
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.createCell(0).setCellValue((String) samples[r][0]);
                Cell ageCell = row.createCell(1);
                ageCell.setCellValue((int) samples[r][1]);
                ageCell.setCellStyle(centerStyle);
                Cell gradeCell = row.createCell(2);
                gradeCell.setCellValue((String) samples[r][2]);
                gradeCell.setCellStyle(centerStyle);
                Cell genderCell = row.createCell(3);
                genderCell.setCellValue((String) samples[r][3]);
                genderCell.setCellStyle(centerStyle);
                Cell valueCell = row.createCell(4);
                valueCell.setCellValue((int) samples[r][4]);
                valueCell.setCellStyle(centerStyle);
            }

            sheet.setColumnWidth(0, 5000);
            sheet.setColumnWidth(1, 3500);
            sheet.setColumnWidth(2, 3500);
            sheet.setColumnWidth(3, 3500);
            sheet.setColumnWidth(4, 3500);

            DataValidationHelper dvHelper = sheet.getDataValidationHelper();

            // 나이(1) 드롭다운
            String[] ageStrings = {"20", "30", "40", "45", "50", "55", "60", "65"};
            CellRangeAddressList ageRange = new CellRangeAddressList(1, 999, 1, 1);
            DataValidationConstraint ageConstraint =
                    dvHelper.createExplicitListConstraint(ageStrings);
            DataValidation ageDv = dvHelper.createValidation(ageConstraint, ageRange);
            ageDv.setShowErrorBox(false);
            sheet.addValidationData(ageDv);

            // 등급(2) 드롭다운
            CellRangeAddressList gradeRange = new CellRangeAddressList(1, 999, 2, 2);
            DataValidationConstraint gradeConstraint =
                    dvHelper.createExplicitListConstraint(new String[]{"S", "A", "B", "C", "D", "E", "F"});
            DataValidation gradeDv = dvHelper.createValidation(gradeConstraint, gradeRange);
            gradeDv.setShowErrorBox(true);
            gradeDv.createErrorBox("등급 오류", "S~F 중 하나를 입력하세요.");
            sheet.addValidationData(gradeDv);

            // 성별(3) 드롭다운
            CellRangeAddressList genderRange = new CellRangeAddressList(1, 999, 3, 3);
            DataValidationConstraint genderConstraint =
                    dvHelper.createExplicitListConstraint(new String[]{"남", "여"});
            DataValidation genderDv = dvHelper.createValidation(genderConstraint, genderRange);
            genderDv.setShowErrorBox(false);
            sheet.addValidationData(genderDv);

            // 수치(4) 범위 제한
            CellRangeAddressList valueRange = new CellRangeAddressList(1, 999, 4, 4);
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

    // ===================== 선수 엑셀 파싱 =====================

    public ExcelUploadResult parse(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        List<DrawRequestDto.PlayerDto> players = new ArrayList<>();
        Set<Integer> validAgeSet = Set.of(20, 30, 40, 45, 50, 55, 60, 65);

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

                String name   = cellString(row, 0);
                String ageStr = cellString(row, 1);
                String grade  = cellString(row, 2).toUpperCase();
                String gender = cellString(row, 3);
                String valStr = cellString(row, 4);
                int rowNum = r + 1;
                boolean ok = true;

                if (name.isEmpty()) {
                    errors.add(rowNum + "행: 선수명이 비어있습니다.");
                    ok = false;
                } else if (name.length() > 20) {
                    errors.add(rowNum + "행: 선수명은 20자 이하여야 합니다. (입력값: \"" + name + "\")");
                    ok = false;
                }

                if (!grade.matches("[SA-F]")) {
                    errors.add(rowNum + "행: 등급은 S~F 중 하나여야 합니다. (입력값: \"" + cellString(row, 2) + "\")");
                    ok = false;
                }

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

                if (gender.isEmpty()) {
                    errors.add(rowNum + "행: 성별을 입력해주세요. (남 또는 여)");
                    ok = false;
                } else if (!gender.equals("남") && !gender.equals("여")) {
                    errors.add(rowNum + "행: 성별은 남 또는 여만 입력 가능합니다. (입력값: \"" + gender + "\")");
                    ok = false;
                }

                int age = 0;
                if (ageStr.isEmpty()) {
                    errors.add(rowNum + "행: 나이를 입력해주세요. (20, 30, 40, 45, 50, 55, 60, 65 중 선택)");
                    ok = false;
                } else {
                    try {
                        age = Integer.parseInt(ageStr);
                        if (!validAgeSet.contains(age)) {
                            errors.add(rowNum + "행: 나이는 20, 30, 40, 45, 50, 55, 60, 65 중 하나여야 합니다. (입력값: " + age + ")");
                            ok = false;
                        }
                    } catch (NumberFormatException e) {
                        errors.add(rowNum + "행: 나이는 숫자여야 합니다. (입력값: \"" + ageStr + "\")");
                        ok = false;
                    }
                }

                if (ok) {
                    DrawRequestDto.PlayerDto dto = new DrawRequestDto.PlayerDto();
                    dto.setName(name);
                    dto.setGrade(grade);
                    dto.setValue(value);
                    dto.setGender(gender);
                    dto.setAge(age);
                    players.add(dto);
                }
            }
        }

        return new ExcelUploadResult(errors.isEmpty(), errors, players);
    }

    // ===================== 대진표 엑셀 생성 =====================

    public byte[] createDrawExcel(List<Court> courts) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("대진표");

            Font boldBlack = wb.createFont();
            boldBlack.setBold(true);
            boldBlack.setColor(IndexedColors.BLACK1.getIndex());
            boldBlack.setFontHeightInPoints((short) 11);

            // 연회색 배경 + 검은 굵은 글씨 (코트명, 게임번호 공용)
            CellStyle grayStyle = wb.createCellStyle();
            grayStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            grayStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            grayStyle.setAlignment(HorizontalAlignment.CENTER);
            grayStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            grayStyle.setFont(boldBlack);
            applyThinBorder(grayStyle);

            // 1행 0열 빈 셀 (배경 없음, 테두리만)
            CellStyle emptyStyle = wb.createCellStyle();
            emptyStyle.setAlignment(HorizontalAlignment.CENTER);
            emptyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorder(emptyStyle);

            // 선수 셀 (배경 없음, 테두리만)
            CellStyle playerStyle = wb.createCellStyle();
            playerStyle.setAlignment(HorizontalAlignment.CENTER);
            playerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorder(playerStyle);

            int maxGames = courts.stream()
                    .mapToInt(c -> c.getGames() != null ? c.getGames().size() : 0)
                    .max().orElse(0);

            // Row 0: (0,0) 빈칸 + 코트 헤더 (4열 병합)
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            Cell emptyCell = headerRow.createCell(0);
            emptyCell.setCellStyle(emptyStyle);

            for (int ci = 0; ci < courts.size(); ci++) {
                int startCol = 1 + ci * 4;
                Cell courtCell = headerRow.createCell(startCol);
                courtCell.setCellValue(courts.get(ci).getCourtNumber() + "코트");
                courtCell.setCellStyle(grayStyle);
                for (int c = startCol + 1; c < startCol + 4; c++) {
                    headerRow.createCell(c).setCellStyle(grayStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(0, 0, startCol, startCol + 3));
            }

            // Row 1+: 게임별 데이터
            for (int gi = 0; gi < maxGames; gi++) {
                Row row = sheet.createRow(gi + 1);
                row.setHeightInPoints(22);

                Cell gameCell = row.createCell(0);
                gameCell.setCellValue((gi + 1) + "게임");
                gameCell.setCellStyle(grayStyle);

                for (int ci = 0; ci < courts.size(); ci++) {
                    int startCol = 1 + ci * 4;
                    Court court = courts.get(ci);

                    if (court.getGames() != null && gi < court.getGames().size()) {
                        Game game = court.getGames().get(gi);
                        String[] vals = {
                            playerDisplay(game.getTeamA1()),
                            playerDisplay(game.getTeamA2()),
                            playerDisplay(game.getTeamB1()),
                            playerDisplay(game.getTeamB2())
                        };
                        for (int c = 0; c < 4; c++) {
                            Cell cell = row.createCell(startCol + c);
                            cell.setCellValue(vals[c]);
                            cell.setCellStyle(playerStyle);
                        }
                    } else {
                        for (int c = startCol; c < startCol + 4; c++) {
                            row.createCell(c).setCellStyle(playerStyle);
                        }
                    }
                }
            }

            // 열 너비
            sheet.setColumnWidth(0, 3200);
            for (int ci = 0; ci < courts.size(); ci++) {
                for (int c = 1 + ci * 4; c < 5 + ci * 4; c++) {
                    sheet.setColumnWidth(c, 4800);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void applyThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String playerDisplay(Player p) {
        if (p == null) return "";
        String ageStr = p.getAge() > 0 ? String.valueOf(p.getAge()) : "";
        return p.getName() + "(" + ageStr + p.getGrade() + ")";
    }

    // ===================== 내부 유틸 =====================

    private boolean isBlankRow(Row row) {
        if (row == null) return true;
        for (int c = 0; c < 5; c++) {
            if (!cellString(row, c).isEmpty()) return false;
        }
        return true;
    }

    private String cellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";

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
