package com.recordsite.backend.service;

import com.recordsite.backend.dto.ChampionTipCreateRequest;
import com.recordsite.backend.dto.ChampionTipPageResponse;
import com.recordsite.backend.dto.ChampionTipResponse;
import com.recordsite.backend.dto.ChampionTipUpdateRequest;
import com.recordsite.backend.entity.ChampionTip;
import com.recordsite.backend.entity.ChampionTipInteraction;
import com.recordsite.backend.entity.TipInteractionType;
import com.recordsite.backend.repository.ChampionTipInteractionRepository;
import com.recordsite.backend.repository.ChampionTipRepository;
import com.recordsite.backend.support.TipPasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 챔피언 운영 팁(코멘트)의 작성/조회/추천·비추천/신고. 로그인 없이 닉네임으로 남기며 대댓글은 없다.
@Service
@RequiredArgsConstructor
public class ChampionTipService {

    private static final int NICKNAME_MAX = 20;
    private static final int CONTENT_MAX = 500;
    private static final int PASSWORD_MIN = 4;
    private static final int PASSWORD_MAX = 30;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String DEFAULT_LANGUAGE = "한국어";

    private final ChampionTipRepository championTipRepository;
    private final ChampionTipInteractionRepository interactionRepository;
    private final TipPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public ChampionTipPageResponse getTips(int championId, String sort, String language,
                                           String patchVersion, int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), pageSize);
        String lang = blankToNull(language);         // "내 언어만 보기" 꺼지면 null
        String patch = blankToNull(patchVersion);    // "현재 버전만 보기" 꺼지면 null

        Page<ChampionTip> tips = "recent".equalsIgnoreCase(sort)
                ? championTipRepository.findRecent(championId, lang, patch, pageRequest)
                : championTipRepository.findPopular(championId, lang, patch, pageRequest); // 기본 인기순

        long totalCount = championTipRepository.countFiltered(championId, lang, patch);
        return new ChampionTipPageResponse(
                tips.map(ChampionTipResponse::from).getContent(),
                totalCount,
                tips.hasNext());
    }

    @Transactional
    public ChampionTipResponse createTip(ChampionTipCreateRequest request) {
        String nickname = require(request.nickname(), "닉네임", NICKNAME_MAX);
        String content = require(request.content(), "팁 내용", CONTENT_MAX);
        String passwordHash = passwordEncoder.encode(requirePassword(request.password()));
        String language = blankToNull(request.language()) == null ? DEFAULT_LANGUAGE : request.language().trim();

        ChampionTip tip = ChampionTip.of(
                request.championId(), nickname, content,
                DataDragonService.currentPatch(), language, passwordHash);
        return ChampionTipResponse.from(championTipRepository.save(tip));
    }

    @Transactional
    public ChampionTipResponse updateTip(Long tipId, ChampionTipUpdateRequest request) {
        ChampionTip tip = findOrThrow(tipId);
        if (request.password() == null || !passwordEncoder.matches(request.password(), tip.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비밀번호가 일치하지 않습니다.");
        }
        tip.editContent(require(request.content(), "팁 내용", CONTENT_MAX));
        return ChampionTipResponse.from(tip); // @Transactional 변경 감지로 저장됨
    }

    @Transactional
    public void deleteTip(Long tipId, String password) {
        ChampionTip tip = findOrThrow(tipId);
        if (password == null || !passwordEncoder.matches(password, tip.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비밀번호가 일치하지 않습니다.");
        }
        championTipRepository.delete(tip);
    }

    // 1인 1회 제약은 같은 사람의 중복만 막는다. 서로 다른 사람이 동시에 누르는 경합은 따로 막아야 한다 —
    // 엔티티를 읽어 올려 +1 하면 둘 다 같은 값을 읽고 같은 값을 써서 증가분이 유실된다(Lost Update).
    // 그래서 카운터는 DB 에서 바로 증가시킨다. 팁이 없으면 갱신 행 수가 0 이다.
    // 증가를 기록보다 먼저 하는 이유: 먼저 기록하면 자식 행 INSERT 가 부모에 공유 잠금을 잡은 뒤
    // 카운터 UPDATE 가 배타 잠금으로 올라가야 해서, 동시 요청끼리 잠금 승격 교착이 날 수 있다.
    @Transactional
    public void vote(Long tipId, String direction, String actorKey) {
        int updated;
        if ("UP".equalsIgnoreCase(direction)) {
            updated = championTipRepository.increaseUpvotes(tipId);
        } else if ("DOWN".equalsIgnoreCase(direction)) {
            updated = championTipRepository.increaseDownvotes(tipId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction 은 UP 또는 DOWN 이어야 합니다.");
        }
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "팁을 찾을 수 없습니다.");
        }
        // 중복이면 여기서 409 가 나고 위의 증가까지 같은 트랜잭션으로 롤백된다.
        recordInteractionOrThrow(tipId, actorKey, TipInteractionType.VOTE, "이미 이 팁에 투표했습니다.");
    }

    // 신고는 숨김 임계값 판정이 엔티티에 있어 읽고 써야 한다. 행을 잠가 동시 신고를 직렬화한다.
    @Transactional
    public void report(Long tipId, String actorKey) {
        ChampionTip tip = championTipRepository.findByIdForUpdate(tipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "팁을 찾을 수 없습니다."));
        recordInteractionOrThrow(tipId, actorKey, TipInteractionType.REPORT, "이미 이 팁을 신고했습니다.");
        tip.report();
    }

    // 1인 1회 제약. 조회 후 저장 사이의 경합은 유니크 제약이 막고, 그때 터지는
    // DataIntegrityViolationException 도 같은 409 로 변환해 응답을 일관되게 유지한다.
    private void recordInteractionOrThrow(Long tipId, String actorKey,
                                          TipInteractionType type, String duplicateMessage) {
        if (interactionRepository.existsByTipIdAndActorKeyAndInteractionType(tipId, actorKey, type)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, duplicateMessage);
        }
        try {
            interactionRepository.saveAndFlush(ChampionTipInteraction.of(tipId, actorKey, type));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, duplicateMessage);
        }
    }

    private ChampionTip findOrThrow(Long tipId) {
        return championTipRepository.findById(tipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "팁을 찾을 수 없습니다."));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    // 비밀번호는 앞뒤 공백까지 그대로 쓴다(공백도 유효 문자). 길이만 검증한다.
    private String requirePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호를 입력하세요.");
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "비밀번호는 " + PASSWORD_MIN + "~" + PASSWORD_MAX + "자여야 합니다.");
        }
        return password;
    }

    // 공백 제거 후 비어있지 않고 길이 제한 이내인지 검증한다. 위반 시 400.
    private String require(String value, String field, int max) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + "을(를) 입력하세요.");
        }
        if (trimmed.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + "은(는) " + max + "자 이내여야 합니다.");
        }
        return trimmed;
    }
}
