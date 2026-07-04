package com.recordsite.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DDragon 정적 메타데이터(예: 아이템)가 어느 패치 버전으로 시딩됐는지 리소스 단위로 기록한다.
// 앱 기동 시 이 버전과 코드의 목표 버전을 비교해, 다르면 재적재한다(패치 올릴 때 자동 갱신).
@Entity
@Table(name = "reference_data_version")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReferenceDataVersion {

    @Id
    @Column(length = 30)
    private String resource; // 예: "item"

    @Column(nullable = false, length = 20)
    private String version;  // 예: "16.13.1"

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ReferenceDataVersion(String resource, String version) {
        this.resource = resource;
        this.version = version;
        this.updatedAt = LocalDateTime.now();
    }

    public static ReferenceDataVersion of(String resource, String version) {
        return new ReferenceDataVersion(resource, version);
    }
}
