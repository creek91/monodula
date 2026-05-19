package com.github.monodula.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.strategy.InProcessSplitStrategy;
import com.github.monodula.maven.strategy.SplitContext;
import com.github.monodula.maven.strategy.SplitResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class InProcessSplitStrategyTest {

    private final InProcessSplitStrategy strategy = new InProcessSplitStrategy();

    @Test
    void shouldReturnInProcessStrategyName() {
        assertThat(strategy.getName()).isEqualTo("in-process");
    }

    @Test
    void shouldFailWhenModuleNotFound() {
        SplitContext context = new SplitContext("nonexistent", List.of());
        assertThatThrownBy(() -> strategy.split(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void shouldFailWhenModuleHasNoApp() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("order-core", ModuleType.CORE),
                        new MavenModule("order-api", ModuleType.API));
        SplitContext context = new SplitContext("order", modules);
        assertThatThrownBy(() -> strategy.split(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app");
    }

    @Test
    void shouldFailWhenModuleHasNoCore() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("order-app", ModuleType.APP),
                        new MavenModule("order-api", ModuleType.API));
        SplitContext context = new SplitContext("order", modules);
        assertThatThrownBy(() -> strategy.split(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("core");
    }

    @Test
    void shouldGenerateStandalonePomWithOwnAppAndCore() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("order-api", ModuleType.API),
                        new MavenModule("order-core", ModuleType.CORE),
                        new MavenModule("order-app", ModuleType.APP),
                        new MavenModule("common-app", ModuleType.COMMON));
        SplitContext context = new SplitContext("order", modules);
        SplitResult result = strategy.split(context);

        assertThat(result).isNotNull();
        assertThat(result.getPomXml()).isNotNull();
        assertThat(result.getDockerfile()).isNotNull();
        assertThat(result.getMainClass()).isNotNull();
    }

    @Test
    void shouldIncludeCrossModuleApiAndCore() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("order-api", ModuleType.API),
                        new MavenModule("order-core", ModuleType.CORE),
                        new MavenModule("order-app", ModuleType.APP),
                        new MavenModule("user-api", ModuleType.API),
                        new MavenModule("user-core", ModuleType.CORE),
                        new MavenModule("common-app", ModuleType.COMMON));
        SplitContext context = new SplitContext("order", modules);
        SplitResult result = strategy.split(context);

        assertThat(result.getDependencies())
                .containsExactlyInAnyOrder(
                        "order-app", "order-core", "user-api", "user-core", "common-app");
    }

    @Test
    void shouldNotIncludeForeignAppInStandaloneDeps() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("order-api", ModuleType.API),
                        new MavenModule("order-core", ModuleType.CORE),
                        new MavenModule("order-app", ModuleType.APP),
                        new MavenModule("user-api", ModuleType.API),
                        new MavenModule("user-core", ModuleType.CORE),
                        new MavenModule("user-app", ModuleType.APP),
                        new MavenModule("common-app", ModuleType.COMMON));
        SplitContext context = new SplitContext("order", modules);
        SplitResult result = strategy.split(context);

        assertThat(result.getDependencies()).doesNotContain("user-app");
    }
}
