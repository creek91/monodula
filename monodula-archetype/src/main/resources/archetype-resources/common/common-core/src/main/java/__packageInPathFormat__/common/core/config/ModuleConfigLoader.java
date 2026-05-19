package ${package}.common.core.config;

import java.io.IOException;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

public abstract class ModuleConfigLoader implements EnvironmentPostProcessor, Ordered {

    protected abstract String getBaseName();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        String baseName = getBaseName();

        // Load base config last (lowest priority among this module's sources)
        List<PropertySource<?>> baseSources = loadYaml(loader, baseName, baseName + ".yml");
        baseSources.forEach(ps -> env.getPropertySources().addLast(ps));

        // Load profile-specific config before the base source so it takes higher priority,
        // allowing profile values to override base values.
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
