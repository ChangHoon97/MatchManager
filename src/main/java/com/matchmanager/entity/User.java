package com.matchmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBLUSER")
@Getter
@Setter
@NoArgsConstructor
public class User {

    public static final String PROVIDER_LOCAL = "LOCAL";
    public static final String PROVIDER_GOOGLE = "GOOGLE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EMAIL", nullable = false, unique = true)
    private String email;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "NICKNAME", nullable = false)
    private String nickname;

    @Column(name = "CELNO")
    private String celno;

    @Column(name = "PROVIDER", nullable = false)
    private String provider;

    @Column(name = "PROVIDER_ID")
    private String providerId;

    @Column(name = "REG_DATE", nullable = false, updatable = false)
    private LocalDateTime regDate;

    @Column(name = "MOD_DATE", nullable = false)
    private LocalDateTime modDate;

    @Column(name = "DEL_YN", nullable = false)
    private String delYn = "N";

    public User(String email, String password, String nickname, String celno, String provider) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.celno = celno;
        this.provider = provider;
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
