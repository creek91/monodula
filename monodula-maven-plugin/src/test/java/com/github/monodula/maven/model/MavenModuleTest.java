package com.github.monodula.maven.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

class MavenModuleTest {

    @Test
    void stores_groupId_version_basedir() {
        File basedir = new File("/tmp/myproject/finance-core");
        MavenModule module =
                new MavenModule("finance-core", ModuleType.CORE, "com.example", "1.0.0", basedir);

        assertThat(module.getArtifactId()).isEqualTo("finance-core");
        assertThat(module.getType()).isEqualTo(ModuleType.CORE);
        assertThat(module.getGroupId()).isEqualTo("com.example");
        assertThat(module.getVersion()).isEqualTo("1.0.0");
        assertThat(module.getBasedir()).isEqualTo(basedir);
    }
}
