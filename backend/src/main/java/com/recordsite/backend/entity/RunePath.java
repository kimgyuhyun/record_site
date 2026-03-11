package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rune_path")
@Getter
@Setter
@NoArgsConstructor
public class RunePath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer pathKey; // 경로키 ex) 8100

    @Column(nullable = false)
    private String runePathNameEn; // 경로 이름 ex) Domination

    @Column(nullable = true)
    private String image;

    @Column(nullable = false)
    private String runePathNameKor; // 경로 이름 ex) 지배


    @OneToMany(mappedBy = "path", cascade = CascadeType.ALL)
    private List<Rune> runes = new ArrayList<>();
}
