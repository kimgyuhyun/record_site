package com.recordsite.backend.config;

import com.recordsite.backend.repository.ItemRepository;
import com.recordsite.backend.service.DataDragonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * DDragon 정적 메타데이터(아이템) 시딩.
 *
 * 챔피언 상세의 아이템 빌드 집계(시작 아이템/신발/핵심 빌드)는 Item 테이블의 가격·태그·상위템 정보에 의존한다.
 * 이 테이블이 비어 있으면 세 섹션이 전부 "데이터 없음"으로 뜬다(룬·스킬·스펠은 Item 테이블을 안 써서 정상).
 *
 * 기존 DataInitRunner 는 매 기동마다 챔피언까지 전부 재적재(챔피언당 1회 HTTP = N+1)해 기동을 지연시켜 꺼져 있었다.
 * 여기서는 Item 테이블이 비어 있을 때만 아이템(item.json 단일 호출)을 1회 적재한다. 이미 있으면 count 쿼리 한 번으로 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceDataInitializer {

    private final ItemRepository itemRepository;
    private final DataDragonService dataDragonService;

    @EventListener(ApplicationReadyEvent.class)
    public void seedItemsIfEmpty() {
        if (itemRepository.count() > 0) {
            return;
        }
        try {
            log.info("Item 메타데이터가 비어 있어 DDragon 에서 적재를 시작한다.");
            dataDragonService.fetchAndSaveAllItems();
            log.info("Item 메타데이터 적재 완료: {} 건", itemRepository.count());
        } catch (Exception e) {
            // 적재 실패해도 앱은 계속 뜬다(아이템 빌드 섹션만 비어 보일 뿐). 다음 기동 때 다시 시도한다.
            log.error("Item 메타데이터 적재 실패 — 챔피언 아이템 빌드가 비어 보일 수 있다.", e);
        }
    }
}
