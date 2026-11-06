package com.admin.template.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigurationContractTest {
    @Test
    void resolvesSpringBootEnvironmentVariablesFromApplicationYaml() throws IOException {
        var loader = new YamlPropertySourceLoader();
        var applicationConfig = loader.load("application",
                new FileSystemResource("src/main/resources/application.yml")).getFirst();
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(applicationConfig);
        environment.getPropertySources().addLast(new SystemEnvironmentPropertySource("testEnvironment", Map.of(
                "SERVER_PORT", "3133",
                "SPRING_DATASOURCE_URL", "jdbc:mysql://db.example/admin_template",
                "SPRING_DATASOURCE_USERNAME", "spring-user",
                "SPRING_DATASOURCE_PASSWORD", "spring-password",
                "APP_JWT_SECRET", "spring-jwt-secret-with-more-than-32-bytes",
                "APP_USER_DEFAULT_PASSWORD", "spring-default-password",
                "APP_UPLOAD_DIRECTORY", "spring-uploads",
                "AI_RAG_BASE_URL", "http://rag.internal:8000",
                "AI_RAG_SERVICE_TOKEN", "internal-test-token")));

        assertThat(environment.getProperty("spring.config.import")).isNull();
        assertThat(environment.getProperty("server.port")).isEqualTo("3133");
        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:mysql://db.example/admin_template");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("spring-user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("spring-password");
        assertThat(environment.getProperty("app.jwt.secret"))
                .isEqualTo("spring-jwt-secret-with-more-than-32-bytes");
        assertThat(environment.getProperty("app.user.default-password")).isEqualTo("spring-default-password");
        assertThat(environment.getProperty("app.upload.directory")).isEqualTo("spring-uploads");
        assertThat(environment.getProperty("app.ai-rag.base-url")).isEqualTo("http://rag.internal:8000");
        assertThat(environment.getProperty("app.ai-rag.service-token")).isEqualTo("internal-test-token");
    }

    @Test
    void separatesDefaultTestAndProductionYamlProfiles() throws IOException {
        Path mainResources = Path.of("src/main/resources");
        Path testResources = Path.of("src/test/resources");
        assertThat(mainResources.resolve("application.yml")).isRegularFile();
        assertThat(mainResources.resolve("application-test.yml")).isRegularFile();
        assertThat(mainResources.resolve("application-prod.yml")).isRegularFile();
        assertThat(testResources.resolve("application-test.yml")).doesNotExist();
        assertThat(testResources.resolve("application.yml")).doesNotExist();
        assertThat(testResources.resolve("application-mysql-test.yml")).doesNotExist();
        assertThat(Path.of("config")).doesNotExist();

        var loader = new YamlPropertySourceLoader();
        var defaultConfig = loader.load("default",
                new FileSystemResource(mainResources.resolve("application.yml"))).getFirst();
        var testConfig = loader.load("test",
                new FileSystemResource(mainResources.resolve("application-test.yml"))).getFirst();
        var productionConfig = loader.load("prod",
                new FileSystemResource(mainResources.resolve("application-prod.yml"))).getFirst();

        var testEnvironment = new StandardEnvironment();
        testEnvironment.getPropertySources().addFirst(defaultConfig);
        testEnvironment.getPropertySources().addFirst(testConfig);
        assertThat(testEnvironment.getProperty("spring.config.activate.on-profile")).isEqualTo("test");
        assertThat(testEnvironment.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
        assertThat(testEnvironment.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.h2.Driver");

        var productionEnvironment = new StandardEnvironment();
        productionEnvironment.getPropertySources().addFirst(defaultConfig);
        productionEnvironment.getPropertySources().addFirst(productionConfig);
        productionEnvironment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("production", Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:mysql://production.example/admin_template",
                "SPRING_DATASOURCE_USERNAME", "production-user",
                "SPRING_DATASOURCE_PASSWORD", "production-password",
                "APP_JWT_SECRET", "production-jwt-secret-with-more-than-32-bytes",
                "APP_USER_DEFAULT_PASSWORD", "production-default-password")));
        assertThat(productionEnvironment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:mysql://production.example/admin_template");
        assertThat(productionEnvironment.getProperty("spring.datasource.username")).isEqualTo("production-user");
        assertThat(productionEnvironment.getProperty("spring.datasource.password")).isEqualTo("production-password");
        assertThat(productionEnvironment.getProperty("app.jwt.secret"))
                .isEqualTo("production-jwt-secret-with-more-than-32-bytes");
        assertThat(productionEnvironment.getProperty("app.user.default-password"))
                .isEqualTo("production-default-password");
    }
}
