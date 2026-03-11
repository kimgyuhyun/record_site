package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rune")
@Getter
@Setter
@NoArgsConstructor
public class Rune {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer runeKey; // ex) 8112

    @Column(nullable = false)
    private String runeNameEn; // ex) Electrocute
    
    @Column(nullable = false)
    private String runeNameKor; // ex) 감전

    @Column(nullable = false)
    private String image;

    @Column(columnDefinition = "TEXT")
    private String shortDesc;
    @Column(columnDefinition = "TEXT")
    private String longDesc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rune_path_id", nullable = false)
    private RunePath path;
}
