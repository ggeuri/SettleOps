package com.settleops.arch;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

// 아키텍처 규칙(레이어 분리, AuditLogRepository 단일 진입점=AuditLogger만 사용)을 ArchUnit 테스트로 강제하고,
// 위반 시 빌드에서 즉시 실패시키는 클래스.
class ArchitectureTest {

    @Test
    void api_layer_should_not_access_repository_directly() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.settleops");

        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..api..")
                .should().accessClassesThat()
                .resideInAnyPackage("..infra..repository..", "..repository..")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    void audit_repository_should_only_be_used_inside_audit_package() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.settleops");

        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..global.audit..")
                .should().accessClassesThat()
                .haveSimpleName("AuditLogRepository")
                .allowEmptyShould(true);

        rule.check(classes);
    }
}