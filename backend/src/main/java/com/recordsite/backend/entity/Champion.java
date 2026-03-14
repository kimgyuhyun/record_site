package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity // DB 테이블 인식
@Table(name = "champion") // 매핑할 테이블 이름
@Getter
@Setter
@NoArgsConstructor // 인자없는 기본  생성자 자동 생성
public class Champion {

    @Id // 기본키
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment
    private Long id; // db 자체 id

    @Column(name = "champion_id", nullable = false, unique = true) // null 불가, 중복 불가
    private String championId; // 챔피언 이름

    @Column(name = "champion_key", nullable = false, unique = true)
    private int championKey; // 챔피온 식별 key

    @Column(name = "name_en", nullable = false)
    private String nameEn; // 영문 이름

    @Column(name = "name_kor", nullable = false)
    private String nameKor; // 한국 이름

    @Column(name = "title", nullable = false)
    private String title; // 챔피언 칭호

    @Column(name = "image_url", nullable = false)
    private String imageUrl; // 이미지 파일명

    @Column
    private String tags;
    
    @Column
    private String partype; // 자원 타입

    @Column(columnDefinition = "TEXT")
    private String blurb; // 짧은 설명
    
    @Column(columnDefinition = "TEXT")
    private String lore; // 전체 스토리
    
    private int infoAttack; // 물리 공격 성향
    private int infoDefense; // 방어/생존 성형
    private int infoMagic; // 마법 공격 성향
    private int infoDifficulty; // 조작 난이도

    @OneToOne(mappedBy = "champion", cascade = CascadeType.ALL)
    // 일대일 매핑
    // 자식 엔티티에서 나를 champion이라는 이름에 필드로 참조함 즉, 자식식 엔티티에서 champion 테이블에 기본키를 외래키로 사용함
    // mappedBy를 선언하는쪽은 부모 테이블이고 fk안가지고 자식 테이블쪽이 fk를 가짐
    // mappedBy에 지정한 이름은 자식 테이블에서 필드로 사용함
    // 챔피언 저장/삭제 시 스텟도 같이 저장/ 삭제 (연쇄 처리)
    private ChampionStats stats;

    @OneToMany(mappedBy = "champion", cascade = CascadeType.ALL)
    private List<ChampionSpell> spells = new ArrayList<>();

    @OneToMany(mappedBy = "champion", cascade = CascadeType.ALL)
    private List<ChampionSkin> skins = new ArrayList<>();
}
