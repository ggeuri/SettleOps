package com.settleops.domain.settlement.api;

import com.settleops.domain.settlement.application.SettlementBatchQueryService;
import com.settleops.domain.settlement.dto.SettlementBatchHistoryResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
public class AdminSettlementBatchQueryController {

    private static final int MAX_PAGE_SIZE = 50; // 운영 조회 폭발 방지 가드레일

    private final SettlementBatchQueryService batchQueryService;

    public AdminSettlementBatchQueryController(SettlementBatchQueryService batchQueryService) {
        this.batchQueryService = batchQueryService;
    }

    /**
     * A2: 배치 이력 통합 조회 (OK/FAIL + SKIP)
     * GET /api/admin/settlement-batches/history?from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20
     *
     * LOCKED:
     * - SKIP 이력은 audit_log(BATCH_RUN_SKIPPED) 기반
     * - from/to 누락 시 최근 7일 기본값(운영 안전)
     * - Admin 응답은 no-store 헤더를 실제 응답에 적용한다.
     */
    @GetMapping("/settlement-batches/history")
    public ResponseEntity<SettlementBatchHistoryResponse> getHistory(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            Pageable pageable
    ){
        Pageable guarded = guardPageable(pageable);
        SettlementBatchHistoryResponse response = batchQueryService.getHistory(from, to, guarded);

        return ResponseEntity.ok()
                .headers(adminNoCacheSecurityHeaders())
                .body(response);
    }

    private Pageable guardPageable(Pageable pageable){
        int page = Math.max(0, pageable.getPageNumber());
        int size = pageable.getPageSize();
        if (size <= 0) size = 20;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;

        // 정렬은 서비스에서 occurredAt desc로 "전역 정렬"을 보장하므로, 여기서는 sort 무시(혼선 방지)
        return PageRequest.of(page, size, Sort.unsorted());
    }

    /**
     * Admin 응답은 캐시되면 운영/보안 리스크가 커서 강제 no-store 권장.
     * (프론트/프록시/브라우저 캐시 모두 차단)
     */
    private HttpHeaders adminNoCacheSecurityHeaders(){
        HttpHeaders h = new HttpHeaders();

        // Cache 방지 (가장 중요)
        h.setCacheControl(CacheControl.noStore().mustRevalidate().cachePrivate().getHeaderValue());
        h.add(HttpHeaders.PRAGMA, "no-cache");
        h.add(HttpHeaders.EXPIRES, "0");

        // 최소 보안 헤더(전역으로도 넣겠지만, 엔드포인트 단에서 확실히)
        h.add("X-Content-Type-Options", "nosniff");
        h.add("X-Frame-Options", "SAMEORIGIN"); // DENY는 개발 중 H2 console/내부 프레임 사용 시 깨질 수 있어 SAMEORIGIN 추천
        h.add("Referrer-Policy", "no-referrer");

        // Permissions-Policy는 일단 안전한 최소값(필요 시 확장)
        h.add("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        return h;
    }
}