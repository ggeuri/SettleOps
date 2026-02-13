package com.settleops.global.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    private Long auditId;
    private String requestId;

//    CREATE TABLE IF NOT EXISTS audit_log (
//            audit_id       BIGINT      NOT NULL AUTO_INCREMENT,
//            request_id     CHAR(36)    NOT NULL,
//    occurred_at    DATETIME    NOT NULL,
//
//    actor_type     VARCHAR(32) NOT NULL,
//    actor_id       VARCHAR(32) NOT NULL,
//
//    action         VARCHAR(64) NOT NULL,
//    entity_type    VARCHAR(32) NOT NULL,
//    entity_id      VARCHAR(36) NOT NULL,
//
//    status_before  VARCHAR(64) NULL,
//    status_after   VARCHAR(64) NULL,
//
//    merchant_id    VARCHAR(32) NULL,
//    meta_json      JSON        NULL COMMENT '[DEV] NULL 허용

}
