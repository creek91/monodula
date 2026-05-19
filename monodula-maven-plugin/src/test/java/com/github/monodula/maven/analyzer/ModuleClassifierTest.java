package com.github.monodula.maven.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.monodula.maven.model.ModuleType;
import org.junit.jupiter.api.Test;

class ModuleClassifierTest {

    @Test
    void classify_common_app_returns_COMMON() {
        assertThat(ModuleClassifier.classify("common-app")).isEqualTo(ModuleType.COMMON);
    }

    @Test
    void classify_common_core_returns_COMMON() {
        assertThat(ModuleClassifier.classify("common-core")).isEqualTo(ModuleType.COMMON);
    }

    @Test
    void classify_common_api_returns_COMMON() {
        assertThat(ModuleClassifier.classify("common-api")).isEqualTo(ModuleType.COMMON);
    }

    @Test
    void classify_business_api_returns_API() {
        assertThat(ModuleClassifier.classify("finance-api")).isEqualTo(ModuleType.API);
    }

    @Test
    void classify_business_core_returns_CORE() {
        assertThat(ModuleClassifier.classify("finance-core")).isEqualTo(ModuleType.CORE);
    }

    @Test
    void classify_business_app_returns_APP() {
        assertThat(ModuleClassifier.classify("finance-app")).isEqualTo(ModuleType.APP);
    }

    @Test
    void classify_aggregator_returns_UNKNOWN() {
        assertThat(ModuleClassifier.classify("myproject-finance")).isEqualTo(ModuleType.UNKNOWN);
        assertThat(ModuleClassifier.classify("myproject-application"))
                .isEqualTo(ModuleType.UNKNOWN);
    }
}
