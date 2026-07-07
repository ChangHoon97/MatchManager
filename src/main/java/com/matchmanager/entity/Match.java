package com.matchmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBLMATCH")
@Getter
@Setter
@NoArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "MATCHGROUP_ID", nullable = false)
    private Long matchGroupId;

    @Column(name = "COURT_NO", nullable = false)
    private int courtNo;

    @Column(name = "ROUND_NO", nullable = false)
    private int roundNo;

    @Column(name = "PLAYER1_NAME", nullable = false)
    private String player1Name;
    @Column(name = "PLAYER1_GRADE", nullable = false)
    private String player1Grade;
    @Column(name = "PLAYER1_GENDER")
    private String player1Gender;
    @Column(name = "PLAYER1_AGE")
    private Integer player1Age;

    @Column(name = "PLAYER2_NAME", nullable = false)
    private String player2Name;
    @Column(name = "PLAYER2_GRADE", nullable = false)
    private String player2Grade;
    @Column(name = "PLAYER2_GENDER")
    private String player2Gender;
    @Column(name = "PLAYER2_AGE")
    private Integer player2Age;

    @Column(name = "PLAYER3_NAME", nullable = false)
    private String player3Name;
    @Column(name = "PLAYER3_GRADE", nullable = false)
    private String player3Grade;
    @Column(name = "PLAYER3_GENDER")
    private String player3Gender;
    @Column(name = "PLAYER3_AGE")
    private Integer player3Age;

    @Column(name = "PLAYER4_NAME", nullable = false)
    private String player4Name;
    @Column(name = "PLAYER4_GRADE", nullable = false)
    private String player4Grade;
    @Column(name = "PLAYER4_GENDER")
    private String player4Gender;
    @Column(name = "PLAYER4_AGE")
    private Integer player4Age;

    @Column(name = "TEAM1_SCORE")
    private Integer team1Score;

    @Column(name = "TEAM2_SCORE")
    private Integer team2Score;

    @Column(name = "REG_ID", nullable = false, updatable = false)
    private Long regId;

    @Column(name = "REG_DATE", nullable = false, updatable = false)
    private LocalDateTime regDate;

    @Column(name = "MOD_ID")
    private Long modId;

    @Column(name = "MOD_DATE", nullable = false)
    private LocalDateTime modDate;

    @Column(name = "DEL_YN", nullable = false)
    private String delYn = "N";

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.regDate = now;
        this.modDate = now;
    }

    @PreUpdate
    void onUpdate() {
        this.modDate = LocalDateTime.now();
    }
}
