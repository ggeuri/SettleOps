package com.settleops.arch;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
}