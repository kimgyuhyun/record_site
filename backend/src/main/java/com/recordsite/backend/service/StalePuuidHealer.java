package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotSummonerResponse;
import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.repository.ParticipantRepository;
import com.recordsite.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장된 puuid 가 Riot 쪽에서 바뀐(= 복호화 불가) 경우를 자가치유한다.
 *
 * Riot 계정의 puuid 는 보통 불변이지만, 드물게 변경되면 옛 puuid 로 보내는 모든 호출이
 * 400("Exception decrypting ...") 로 실패한다. 이때 저장된 이름#태그로 현재 puuid 를 다시
 * 해소해 소환사·참가자 기록을 새 puuid 로 이관하면, 과거 전적을 유지한 채 갱신이 복구된다.
 *
 * 재해소·이관을 독립 트랜잭션(REQUIRES_NEW)으로 커밋해, 호출 측(갱신)이 이어서 새 puuid 로
 * 조회할 때 영속성 컨텍스트에 남은 옛 값이 되살아나지 않도록 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StalePuuidHealer {

    private final SummonerRepository summonerRepository;
    private final ParticipantRepository participantRepository;
    private final RiotSummonerClient riotSummonerClient;

    // 이관 성공 시 새 puuid, 치유 불가(행 없음·재해소 실패·puuid 동일) 시 null 반환.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String heal(String stalePuuid) {
        Summoner summoner = summonerRepository.findBypuuid(stalePuuid);
        if (summoner == null) {
            return null; // 우리가 모르는 puuid → 이름#태그를 알 수 없어 재해소 불가
        }

        RiotSummonerResponse res =
                riotSummonerClient.getSummonerByRiotId(summoner.getName(), summoner.getTagLine());
        String newPuuid = res == null ? null : res.getPuuid();
        if (newPuuid == null || newPuuid.isBlank() || newPuuid.equals(stalePuuid)) {
            return null; // 재해소 실패거나 실제로 바뀐 게 아니면 그대로 둔다
        }

        int movedMatches = participantRepository.repointPuuid(stalePuuid, newPuuid);
        summonerRepository.repointPuuid(stalePuuid, newPuuid);
        log.warn("puuid 자가치유: {}#{} {} → {} (매치 {}건 이관)",
                summoner.getName(), summoner.getTagLine(), stalePuuid, newPuuid, movedMatches);
        return newPuuid;
    }
}
