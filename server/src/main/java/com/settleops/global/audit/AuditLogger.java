package com.settleops.global.audit;

import jakarta.transaction.Transactional;

public class AuditLogger {

    @Transactional
    public void log(AuditLogCommand cmd) {
//        if(cmd == null) throw new IllegalArgumentException("AuditLogCommand is null");
//        if(cmd.getRequestId().isBlank()) throw new IllegalArgumentException("RequestId is null");
//        if(cmd.getAction() == null) throw new IllegalArgumentException("action is null");
//        if(cmd.get() == null) throw new IllegalArgumentException("action is null");

    }
}
