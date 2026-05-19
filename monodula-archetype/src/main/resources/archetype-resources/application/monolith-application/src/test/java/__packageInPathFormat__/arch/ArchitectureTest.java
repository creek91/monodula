package ${package}.arch;

import com.github.monodula.archunit.MonodulaRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "${package}",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // R001: core cannot depend on another module's core
    @ArchTest
    ArchRule r001 = MonodulaRules.coreNotDependOnOtherCore();

    // R002: core cannot depend on another module's app
    @ArchTest
    ArchRule r002 = MonodulaRules.coreNotDependOnOtherApp();

    // R003: api cannot depend on any core
    @ArchTest
    ArchRule r003 = MonodulaRules.apiNotDependOnCore();

    // R004: api cannot depend on any app
    @ArchTest
    ArchRule r004 = MonodulaRules.apiNotDependOnApp();

    // R005: common cannot depend on business modules
    @ArchTest
    ArchRule r005 = MonodulaRules.commonNotDependOnBusiness();

    // R006: app cannot depend on another module's app
    @ArchTest
    ArchRule r006 = MonodulaRules.appNotDependOnOtherApp();
}
