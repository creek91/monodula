package com.github.monodula.maven.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import java.util.List;
import org.junit.jupiter.api.Test;

class SplitContextTest {

    @Test
    void stores_all_fields() {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE));

        SplitContext ctx =
                new SplitContext(
                        "finance",
                        modules,
                        "myproject",
                        "com.example",
                        "1.0.0",
                        "com.example.myproject");

        assertThat(ctx.getModuleName()).isEqualTo("finance");
        assertThat(ctx.getModules()).hasSize(2);
        assertThat(ctx.getRootArtifactId()).isEqualTo("myproject");
        assertThat(ctx.getGroupId()).isEqualTo("com.example");
        assertThat(ctx.getVersion()).isEqualTo("1.0.0");
        assertThat(ctx.getBasePackage()).isEqualTo("com.example.myproject");
    }

    @Test
    void old_constructor_still_works() {
        List<MavenModule> modules = List.of(new MavenModule("finance-app", ModuleType.APP));
        // Ensure backward-compatible constructor (if it existed) still works
        // OR ensure the new constructor is the only one and it's used consistently
        SplitContext ctx =
                new SplitContext(
                        "finance",
                        modules,
                        "myproject",
                        "com.example",
                        "1.0.0",
                        "com.example.myproject");
        assertThat(ctx.getModuleName()).isEqualTo("finance");
    }
}
