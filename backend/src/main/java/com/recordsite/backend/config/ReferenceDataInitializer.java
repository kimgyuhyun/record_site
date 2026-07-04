package com.recordsite.backend.config;

import com.recordsite.backend.entity.ReferenceDataVersion;
import com.recordsite.backend.repository.ItemRepository;
import com.recordsite.backend.repository.ReferenceDataVersionRepository;
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
 * 이 테이블이 비어 있거나 옛 패치로만 채워져 있으면 세 섹션이 "데이터 없음"으로 뜨거나 신규 아이템이 누락된다
 * (룬·스킬·스펠은 Item 테이블을 안 써서 무관).
 *
 * 시딩된 버전을 reference_data_version 에 기록해두고, 기동 시 코드의 목표 버전(DataDragonService.VERSION)과
 * 비교한다. 다르면(=패치 올림) 또는 비어 있으면 item.json(단일 호출)으로 재적재한다. 같으면 count 쿼리 한 번으로 건너뛴다.
 * 챔피언은 챔피언당 1회 HTTP(N+1)로 느려서 여기서 자동 적재하지 않는다(기동 지연 방지).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceDataInitializer {

    private static final String ITEM_RESOURCE = "item";

    private final ItemRepository itemRepository;
    private final ReferenceDataVersionRepository versionRepository;
    private final DataDragonService dataDragonService;

    @EventListener(ApplicationReadyEvent.class)
    public void seedItemsIfOutdated() {
        String targetVersion = dataDragonService.dataDragonVersion();
        String seededVersion = versionRepository.findById(ITEM_RESOURCE)
                .map(ReferenceDataVersion::getVersion)
                .orElse(null);

        boolean upToDate = targetVersion.equals(seededVersion) && itemRepository.count() > 0;
        if (upToDate) {
            return;
        }
        try {
            log.info("Item 메타데이터를 {} 버전으로 적재한다(현재 시딩 버전: {}).", targetVersion, seededVersion);
            dataDragonService.fetchAndSaveAllItems();
            versionRepository.save(ReferenceDataVersion.of(ITEM_RESOURCE, targetVersion));
            log.info("Item 메타데이터 적재 완료: {} 건 ({} 버전).", itemRepository.count(), targetVersion);
        } catch (Exception e) {
            // 적재 실패해도 앱은 계속 뜬다(버전 기록을 안 남기므로 다음 기동 때 다시 시도).
            log.error("Item 메타데이터 적재 실패 — 챔피언 아이템 빌드가 비어 보일 수 있다.", e);
        }
    }
}
