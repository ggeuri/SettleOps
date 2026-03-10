package com.settleops.global.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.settleops.global.error.BadRequestException;

public final class AuditMetaFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuditMetaFactory() {
    }

    /**
     * 성공 기본 메타
     * 예: {"noOp": false}
     */
    public static ObjectNode success() {
        ObjectNode meta = MAPPER.createObjectNode();
        meta.put("noOp", false);
        return meta;
    }

    /**
     * 성공 + comment
     * 예: {"noOp": false, "comment": "..."}
     */
    public static ObjectNode successWithComment(String comment) {
        ObjectNode meta = success();
        putIfHasText(meta, "comment", comment);
        return meta;
    }

    /**
     * 성공 + reasonCode + comment
     * 예: {"noOp": false, "reasonCode": "MANUAL_REVIEW", "comment": "..."}
     */
    public static ObjectNode successWithReasonAndComment(String reasonCode, String comment) {
        ObjectNode meta = success();
        putIfHasText(meta, "reasonCode", reasonCode);
        putIfHasText(meta, "comment", comment);
        return meta;
    }

    /**
     * no-op 기본 메타
     * 예: {"noOp": true, "noOpReason": "ALREADY_CONFIRMED"}
     */
    public static ObjectNode noOp(NoOpReason noOpReason) {
        ObjectNode meta = MAPPER.createObjectNode();
        meta.put("noOp", true);
        meta.put("noOpReason", noOpReason.name());
        return meta;
    }

    /**
     * no-op + 멱등 재시도 여부 포함 메타 생성
     * 예: {"noOp": true, "noOpReason": "ALREADY_CONFIRMED", "idempotent": true}
     */
    public static ObjectNode noOp(NoOpReason noOpReason, boolean idempotent) {
        ObjectNode meta = noOp(noOpReason);
        meta.put("idempotent", idempotent);
        return meta;
    }

    /**
     * no-op + 현재 상태 스냅샷
     * 예:
     * {
     *   "noOp": true,
     *   "noOpReason": "ALREADY_APPROVED",
     *   "after": {"status": "APPROVED"}
     * }
     */
    public static ObjectNode noOpWithStatus(NoOpReason noOpReason, String currentStatus) {
        ObjectNode meta = noOp(noOpReason);
        meta.set("after", statusNode(currentStatus));
        return meta;
    }

    /**
     * no-op + 현재 상태(after.status) + 멱등 재시도 여부 포함 메타 생성
     * 예:
     * {
     *   "noOp": true,
     *   "noOpReason": "ALREADY_PAY_REQUESTED",
     *   "idempotent": true,
     *   "after": {"status": "PAY_REQUESTED"}
     * }
     */
    public static ObjectNode noOpWithStatus(NoOpReason noOpReason, String currentStatus, boolean idempotent) {
        ObjectNode meta = noOp(noOpReason, idempotent);
        meta.set("after", statusNode(currentStatus));
        return meta;
    }

    /**
     * 상태 전이 기본 메타
     * 예:
     * {
     *   "noOp": false,
     *   "before": {"status": "READY"},
     *   "after": {"status": "PAY_REQUESTED"}
     * }
     */
    public static ObjectNode statusChange(String beforeStatus, String afterStatus) {
        ObjectNode meta = success();
        meta.set("before", statusNode(beforeStatus));
        meta.set("after", statusNode(afterStatus));
        return meta;
    }

    /**
     * 상태 전이 + comment
     */
    public static ObjectNode statusChange(String beforeStatus, String afterStatus, String comment) {
        requireComment(comment);

        ObjectNode meta = statusChange(beforeStatus, afterStatus);
        meta.put("comment", comment);
        return meta;
    }

    /**
     * 상태 전이 + reasonCode + comment
     */
    public static ObjectNode statusChange(
            String beforeStatus,
            String afterStatus,
            String reasonCode,
            String comment
    ) {
        requireComment(comment);

        ObjectNode meta = statusChange(beforeStatus, afterStatus);
        putIfHasText(meta, "reasonCode", reasonCode);
        meta.put("comment", comment);
        return meta;
    }

    /**
     * 상태값 JSON 노드 생성
     * 예: {"status": "READY"}
     */
    private static ObjectNode statusNode(String status) {
        ObjectNode node = MAPPER.createObjectNode();
        putIfHasText(node, "status", status);
        return node;
    }

    /**
     * 문자열 값 존재 시 JSON 필드 추가
     * null/blank 값은 저장하지 않음
     */
    private static void putIfHasText(ObjectNode node, String fieldName, String value) {
        if (value != null && !value.isBlank()) {
            node.put(fieldName, value);
        }
    }

    /**
     * comment 필수값 검증
     * null/blank 입력 시 BadRequestException 발생
     */
    private static void requireComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BadRequestException("comment is null/blank");
        }
    }
}