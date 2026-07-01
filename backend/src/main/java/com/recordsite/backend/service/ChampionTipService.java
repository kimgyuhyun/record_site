package com.recordsite.backend.service;

import com.recordsite.backend.dto.ChampionTipCreateRequest;
import com.recordsite.backend.dto.ChampionTipPageResponse;
import com.recordsite.backend.dto.ChampionTipResponse;
import com.recordsite.backend.dto.ChampionTipUpdateRequest;
import com.recordsite.backend.entity.ChampionTip;
import com.recordsite.backend.repository.ChampionTipRepository;
import com.recordsite.backend.support.TipPasswordEncoder;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public void vote(Long tipId, String direction) {
        ChampionTip tip = findOrThrow(tipId);
        if ("UP".equalsIgnoreCase(direction)) {
            tip.upvote();
        } else if ("DOWN".equalsIgnoreCase(direction)) {
            tip.downvote();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction 은 UP 또는 DOWN 이어야 합니다.");
        }
    }

    @Transactional
    public void report(Long tipId) {
        findOrThrow(tipId).report();
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
