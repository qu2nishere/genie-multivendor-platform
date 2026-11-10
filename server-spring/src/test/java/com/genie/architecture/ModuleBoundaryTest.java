package com.genie.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;

/**
 * Luật giao tiếp giữa module — TECH_STACK_VA_QUY_UOC.md mục 4.4 (chạy trong CI).
 */
@AnalyzeClasses(packages = "com.genie", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

    static final List<String> MODULES = List.of("auth", "catalog", "order", "payment", "vendor", "shipping",
            "promotion", "social", "support", "notification", "admin");

    // bên ngoài module X không được chạm domain / infra / api của X — chỉ *Facade (application) và event/
    @ArchTest
    static void modules_only_talk_through_facades(JavaClasses classes) {
        for (String m : MODULES) {
            noClasses().that().resideOutsideOfPackage("com.genie." + m + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.genie." + m + "..domain..", "com.genie." + m + "..infra..", "com.genie." + m + "..api..")
                    .because("module " + m + " chỉ được gọi qua *Facade hoặc sự kiện trong event/")
                    .allowEmptyShould(true)
                    .check(classes);
        }
    }

    @ArchTest
    static final ArchRule api_never_touches_infra =
            noClasses().that().resideInAPackage("..api..").should().dependOnClassesThat().resideInAPackage("..infra..");

    @ArchTest
    static final ArchRule domain_is_pure =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("..api..", "..application..", "..infra..");

    @ArchTest
    static final ArchRule modules_free_of_cycles =
            slices().matching("com.genie.(*)..").should().beFreeOfCycles();
}
