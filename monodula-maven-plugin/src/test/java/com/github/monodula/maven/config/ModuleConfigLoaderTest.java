package com.github.monodula.maven.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Unit tests for ModuleConfigLoader logic.
 *
 * <p>Load priority (highest → lowest): Apollo / system env / command-line args profile config
 * (addBefore base → higher priority than base) base config (addLast → lowest among module sources)
 */
class ModuleConfigLoaderTest {

    // -----------------------------------------------------------------------
    // Inline implementation: mirrors the fixed ModuleConfigLoader template.
    // -----------------------------------------------------------------------
    abstract static class ModuleConfigLoader implements EnvironmentPostProcessor, Ordered {

        protected abstract String getBaseName();

        @Override
        public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            String baseName = getBaseName();

            // Base config: lowest priority among this module's sources
            List<PropertySource<?>> baseSources = loadYaml(loader, baseName, baseName + ".yml");
            baseSources.forEach(ps -> env.getPropertySources().addLast(ps));

            // Profile config: inserted before base so it overrides base values
            for (String profile : env.getActiveProfiles()) {
                String profileName = baseName + "-" + profile;
                List<PropertySource<?>> profileSources =
                        loadYaml(loader, profileName, profileName + ".yml");
                for (PropertySource<?> ps : profileSources) {
                    if (env.getPropertySources().contains(baseName)) {
                        env.getPropertySources().addBefore(baseName, ps);
                    } else {
                        env.getPropertySources().addLast(ps);
                    }
                }
            }
        }

        private List<PropertySource<?>> loadYaml(
                YamlPropertySourceLoader loader, String name, String location) {
            ClassPathResource resource = new ClassPathResource(location);
            if (!resource.exists()) {
                return List.of();
            }
            try {
                return loader.load(name, resource);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load config: " + location, e);
            }
        }

        @Override
        public int getOrder() {
            return ConfigDataEnvironmentPostProcessor.ORDER + 1;
        }
    }

    static class TestModuleConfigLoader extends ModuleConfigLoader {
        @Override
        protected String getBaseName() {
            return "module-test-config";
        }
    }

    private ConfigurableEnvironment emptyEnv() {
        return new StandardEnvironment();
    }

    private ConfigurableEnvironment envWithProfiles(String... profiles) {
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles(profiles);
        return env;
    }

    // -----------------------------------------------------------------------
    // TC-A-01: base YAML exists → loaded successfully
    // -----------------------------------------------------------------------
    @Test
    void tc_a01_base_yaml_is_loaded() {
        ConfigurableEnvironment env = emptyEnv();
        new TestModuleConfigLoader().postProcessEnvironment(env, null);

        assertThat(env.getProperty("base.key")).isEqualTo("base-value");
    }

    // -----------------------------------------------------------------------
    // TC-A-02: base YAML does not exist → silently skipped, no exception
    // -----------------------------------------------------------------------
    @Test
    void tc_a02_missing_base_yaml_is_silently_skipped() {
        ConfigurableEnvironment env = emptyEnv();
        ModuleConfigLoader loader =
                new ModuleConfigLoader() {
                    @Override
                    protected String getBaseName() {
                        return "non-existent-config";
                    }
                };

        loader.postProcessEnvironment(env, null);

        assertThat(env.getProperty("base.key")).isNull();
    }

    // -----------------------------------------------------------------------
    // TC-A-03: single active profile → profile YAML is loaded
    // -----------------------------------------------------------------------
    @Test
    void tc_a03_profile_yaml_is_loaded_for_active_profile() {
        ConfigurableEnvironment env = envWithProfiles("dev");
        new TestModuleConfigLoader().postProcessEnvironment(env, null);

        assertThat(env.getProperty("profile.key")).isEqualTo("dev-value");
    }

    // -----------------------------------------------------------------------
    // TC-A-05: profile YAML does not exist → only base config is loaded
    // -----------------------------------------------------------------------
    @Test
    void tc_a05_missing_profile_yaml_falls_back_to_base_only() {
        ConfigurableEnvironment env = envWithProfiles("staging");
        new TestModuleConfigLoader().postProcessEnvironment(env, null);

        assertThat(env.getProperty("base.key")).isEqualTo("base-value");
        assertThat(env.getProperty("staging.key")).isNull();
    }

    // -----------------------------------------------------------------------
    // TC-A-07: addLast semantics — a pre-existing higher-priority source wins
    // -----------------------------------------------------------------------
    @Test
    void tc_a07_addLast_does_not_override_existing_higher_priority_source() {
        ConfigurableEnvironment env = emptyEnv();
        env.getPropertySources()
                .addFirst(
                        new MapPropertySource(
                                "high-priority", Map.of("base.key", "override-value")));

        new TestModuleConfigLoader().postProcessEnvironment(env, null);

        assertThat(env.getProperty("base.key")).isEqualTo("override-value");
    }

    // -----------------------------------------------------------------------
    // TC-A-09: getOrder() returns ConfigDataEnvironmentPostProcessor.ORDER + 1
    // -----------------------------------------------------------------------
    @Test
    void tc_a09_getOrder_returns_config_data_order_plus_one() {
        assertThat(new TestModuleConfigLoader().getOrder())
                .isEqualTo(ConfigDataEnvironmentPostProcessor.ORDER + 1);
    }

    // -----------------------------------------------------------------------
    // TC-C-25: same key in base and profile → profile value wins (fix verified)
    //
    // module-test-config.yml     → shared.key = base-shared
    // module-test-config-dev.yml → shared.key = dev-shared
    //
    // Profile source is inserted via addBefore(baseName), giving it higher
    // priority than the base source. Therefore dev-shared is returned.
    // -----------------------------------------------------------------------
    @Test
    void tc_c25_profile_value_overrides_base_value() {
        ConfigurableEnvironment env = envWithProfiles("dev");
        new TestModuleConfigLoader().postProcessEnvironment(env, null);

        assertThat(env.getProperty("shared.key")).isEqualTo("dev-shared");
    }
}
