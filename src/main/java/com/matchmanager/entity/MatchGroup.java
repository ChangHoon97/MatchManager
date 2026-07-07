package com.matchmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBLMATCHGROUP")
@Getter
@Setter
@NoArgsConstructor
public class MatchGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TITLE", nullable = false)
    private String title;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "SHARE_TOKEN", unique = true)
    private String shareToken;

    @Column(name = "TOTAL_PLAYERS", nullable = false)
    private int totalPlayers;

    @Column(name = "COURT_COUNT", nullable = false)
    private int courtCount;

    @Column(name = "GAME_COUNT", nullable = false)
    private int gameCount;

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

    public MatchGroup(String title, int totalPlayers, int courtCount, int gameCount, Long regId) {
        this.title = title;
        this.totalPlayers = totalPlayers;
        this.courtCount = courtCount;
        this.gameCount = gameCount;
        this.regId = regId;
        this.modId = regId;
    }

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
