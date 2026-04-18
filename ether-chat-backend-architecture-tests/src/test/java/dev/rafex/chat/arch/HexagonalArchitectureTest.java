package dev.rafex.chat.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "dev.rafex.chat")
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule core_does_not_depend_on_infra =
        noClasses().that().resideInAPackage("dev.rafex.chat..service..")
            .should().dependOnClassesThat().resideInAPackage("dev.rafex.chat..infra..");

    @ArchTest
    static final ArchRule ports_do_not_depend_on_infra =
        noClasses().that().resideInAPackage("dev.rafex.chat..port..")
            .should().dependOnClassesThat().resideInAPackage("dev.rafex.chat..infra..");

    @ArchTest
    static final ArchRule infra_does_not_depend_on_bootstrap =
        noClasses().that().resideInAPackage("dev.rafex.chat..infra..")
            .should().dependOnClassesThat().resideInAPackage("dev.rafex.chat.bootstrap..");
}
