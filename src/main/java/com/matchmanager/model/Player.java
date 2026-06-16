package com.matchmanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    private String name;
    private String grade; // A~F
    private int value;    // 0~100 (높을수록 해당 급수 내 강함)
    private String gender; // "남" or "여" (optional)

    public Player(String name, String grade) {
        this.name = name;
        this.grade = grade;
        this.value = 50;
    }

    public Player(String name, String grade, int value) {
        this.name = name;
        this.grade = grade;
        this.value = value;
    }

    /** A=6, B=5 ... F=1 — 클수록 강함 */
    public int getGradeValue() {
        return switch (grade.toUpperCase()) {
            case "S" -> 7;
            case "A" -> 6;
            case "B" -> 5;
            case "C" -> 4;
            case "D" -> 3;
            case "E" -> 2;
            case "F" -> 1;
            default  -> 0;
        };
    }

    /** 0~600 범위. 높을수록 강함. A+100=600, F+0=0 */
    public int getTotalScore() {
        return (getGradeValue() - 1) * 100 + value;
    }
}
