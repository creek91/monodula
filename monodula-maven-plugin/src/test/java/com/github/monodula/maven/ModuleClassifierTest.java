package com.github.monodula.maven;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.monodula.maven.analyzer.ModuleClassifier;
import com.github.monodula.maven.model.ModuleType;
import org.junit.jupiter.api.Test;

class ModuleClassifierTest {

    @Test
    void shouldClassifyModuleAsApi() {
        assertThat(ModuleClassifier.classify("order-api")).isEqualTo(ModuleType.API);
    }

    @Test
    void shouldClassifyModuleAsCore() {
        assertThat(ModuleClassifier.classify("order-core")).isEqualTo(ModuleType.CORE);
    }

    @Test
    void shouldClassifyModuleAsApp() {
        assertThat(ModuleClassifier.classify("order-app")).isEqualTo(ModuleType.APP);
    }

    @Test
    void shouldClassifyModuleAsCommon() {
        assertThat(ModuleClassifier.classify("common-api")).isEqualTo(ModuleType.COMMON);
    }

    @Test
    void shouldClassifyModuleAsCommonCore() {
        assertThat(ModuleClassifier.classify("common-core")).isEqualTo(ModuleType.COMMON);
    }
}
