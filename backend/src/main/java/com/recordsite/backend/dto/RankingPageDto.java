package com.recordsite.backend.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// 랭킹 페이지 응답. Page 객체를 직접 노출하지 않고 필요한 필드만 추려 반환한다.
public record RankingPageDto(
        List<RankingRowDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static RankingPageDto from(Page<RankingRowDto> page) {
        return new RankingPageDto(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
