package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 챔피언별 운영 팁(코멘트). op.gg 챔피언 팁 게시판처럼 로그인 없이 닉네임으로 한 줄 팁을 남긴다.
// 대댓글은 없다(단순 코멘트). 추천/비추천으로 인기순 정렬하고, 신고가 누적되면 목록에서 숨긴다.
@Entity
@Table(
        name = "champion_tip",
        indexes = @Index(name = "idx_champion_tip_champion", columnList = "champion_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChampionTip {

    // 신고가 이 수 이상 쌓이면 자동으로 목록에서 숨긴다(운영자 개입 전 임시 차단).
    private static final int HIDE_THRESHOLD = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "champion_id", nullable = false)
    private int championId;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, length = 500)
    private String content;

    // 작성 시점의 게임 패치(예: "16.12"). 오래된 팁을 버전으로 걸러보게 한다.
    @Column(name = "patch_version", length = 20)
    private String patchVersion;

    @Column(nullable = false, length = 20)
    private String language; // 예: "한국어"

    // 삭제용 비밀번호 해시(PBKDF2, "salt$hash"). 비로그인이라 본인 글 삭제 키로만 쓴다.
    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(nullable = false)
    private int upvotes;

    @Column(nullable = false)
    private int downvotes;

    @Column(name = "report_count", nullable = false)
    private int reportCount;

    @Column(nullable = false)
    private boolean hidden; // 신고 누적으로 숨겨졌는지

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private ChampionTip(int championId, String nickname, String content,
                        String patchVersion, String language, String passwordHash) {
        this.championId = championId;
        this.nickname = nickname;
        this.content = content;
        this.patchVersion = patchVersion;
        this.language = language;
        this.passwordHash = passwordHash;
        this.upvotes = 0;
        this.downvotes = 0;
        this.reportCount = 0;
        this.hidden = false;
        this.createdAt = LocalDateTime.now();
    }

    public static ChampionTip of(int championId, String nickname, String content,
                                 String patchVersion, String language, String passwordHash) {
        return new ChampionTip(championId, nickname, content, patchVersion, language, passwordHash);
    }

    // 추천/비추천 증가는 동시 요청의 증가분 유실을 막으려고 리포지토리의 UPDATE 쿼리로 처리한다(엔티티에 증가 메서드 없음).

    public void report() {
        this.reportCount++;
        if (this.reportCount >= HIDE_THRESHOLD) {
            this.hidden = true;
        }
    }

    public void editContent(String content) {
        this.content = content;
    }

    public int score() {
        return upvotes - downvotes;
    }
}
