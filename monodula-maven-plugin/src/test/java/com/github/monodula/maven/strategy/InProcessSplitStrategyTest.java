package com.github.monodula.maven.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import java.util.List;
import org.junit.jupiter.api.Test;

class InProcessSplitStrategyTest {

    private static final String ROOT = "myproject";
    private static final String GROUP = "com.example";
    private static final String VERSION = "1.0.0";
    private static final String BASE_PKG = "com.example.myproject";

    private SplitContext ctx(String module, List<MavenModule> modules) {
        return new SplitContext(module, modules, ROOT, GROUP, VERSION, BASE_PKG);
    }

    // --- Dependency list tests ---

    @Test
    void split_includes_target_app_and_core() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE),
                        new MavenModule("finance-api", ModuleType.API));
        SplitResult result = new InProcessSplitStrategy().split(ctx("finance", modules));
        assertThat(result.getDependencies()).contains("finance-app", "finance-core");
    }

    @Test
    void split_includes_other_business_api_and_core() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE),
                        new MavenModule("finance-api", ModuleType.API),
                        new MavenModule("account-api", ModuleType.API),
                        new MavenModule("account-core", ModuleType.CORE),
                        new MavenModule("account-app", ModuleType.APP),
                        new MavenModule("order-api", ModuleType.API),
                        new MavenModule("order-core", ModuleType.CORE));
        SplitResult result = new InProcessSplitStrategy().split(ctx("finance", modules));
        assertThat(result.getDependencies())
                .contains("account-api", "account-core", "order-api", "order-core");
    }

    @Test
    void split_excludes_other_business_app() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE),
                        new MavenModule("account-app", ModuleType.APP),
                        new MavenModule("account-core", ModuleType.CORE));
        SplitResult result = new InProcessSplitStrategy().split(ctx("finance", modules));
        assertThat(result.getDependencies()).doesNotContain("account-app");
    }

    @Test
    void split_includes_all_common_modules() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE),
                        new MavenModule("common-api", ModuleType.COMMON),
                        new MavenModule("common-core", ModuleType.COMMON),
                        new MavenModule("common-app", ModuleType.COMMON));
        SplitResult result = new InProcessSplitStrategy().split(ctx("finance", modules));
        assertThat(result.getDependencies()).contains("common-api", "common-core", "common-app");
    }

    @Test
    void split_excludes_target_module_api_from_other_list() {
        // finance-api should not appear twice in dependencies
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE),
                        new MavenModule("finance-api", ModuleType.API));
        SplitResult result = new InProcessSplitStrategy().split(ctx("finance", modules));
        long financeApiCount =
                result.getDependencies().stream().filter("finance-api"::equals).count();
        assertThat(financeApiCount).isLessThanOrEqualTo(1);
    }

    @Test
    void split_throws_when_target_has_no_app() {
        List<MavenModule> modules = List.of(new MavenModule("finance-core", ModuleType.CORE));
        assertThatThrownBy(() -> new InProcessSplitStrategy().split(ctx("finance", modules)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void split_throws_when_target_has_no_core() {
        List<MavenModule> modules = List.of(new MavenModule("finance-app", ModuleType.APP));
        assertThatThrownBy(() -> new InProcessSplitStrategy().split(ctx("finance", modules)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Output path tests ---

    @Test
    void split_generates_module_dir_under_application() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE));
        SplitResult result = new InProcessSplitStrategy().split(ctx("finance", modules));
        assertThat(result.getOutputDir()).endsWith("application/finance-application");
    }

    @Test
    void split_generates_main_class_with_correct_package_path() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE));
        SplitResult result = new InProcessSplitStrategy().split(ctx("finance", modules));
        assertThat(result.getMainClassPath())
                .endsWith(
                        "src/main/java/com/example/myproject/app/finance/FinanceStandaloneApplication.java");
    }

    @Test
    void split_generates_dockerfile_in_module_root() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE));
        SplitResult result = new InProcessSplitStrategy().split(ctx("finance", modules));
        assertThat(result.getDockerfilePath())
                .endsWith("application/finance-application/Dockerfile");
    }

    @Test
    void split_generates_application_yml_in_resources() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE));
        SplitResult result = new InProcessSplitStrategy().split(ctx("finance", modules));
        assertThat(result.getApplicationYmlPath()).endsWith("src/main/resources/application.yml");
    }

    // --- Hyphenated module name tests ---

    @Test
    void split_handles_hyphenated_module_name_in_paths() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("payment-gateway-app", ModuleType.APP),
                        new MavenModule("payment-gateway-core", ModuleType.CORE));
        SplitResult result = new InProcessSplitStrategy().split(ctx("payment-gateway", modules));
        assertThat(result.getMainClassPath())
                .endsWith(
                        "src/main/java/com/example/myproject/app/paymentgateway"
                                + "/PaymentGatewayStandaloneApplication.java");
    }

    // --- Static utility method tests ---

    @Test
    void toPackageSegment_removes_hyphens_and_lowercases() {
        assertThat(InProcessSplitStrategy.toPackageSegment("payment-gateway"))
                .isEqualTo("paymentgateway");
        assertThat(InProcessSplitStrategy.toPackageSegment("finance")).isEqualTo("finance");
        assertThat(InProcessSplitStrategy.toPackageSegment("my-long-module"))
                .isEqualTo("mylongmodule");
    }

    @Test
    void toClassName_capitalizes_each_segment() {
        assertThat(InProcessSplitStrategy.toClassName("payment-gateway"))
                .isEqualTo("PaymentGateway");
        assertThat(InProcessSplitStrategy.toClassName("finance")).isEqualTo("Finance");
        assertThat(InProcessSplitStrategy.toClassName("my-long-module")).isEqualTo("MyLongModule");
    }
}
