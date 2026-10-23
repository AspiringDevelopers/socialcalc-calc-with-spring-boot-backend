package com.aspiringinvestments.backend.security;

import com.aspiringinvestments.backend.AspiringInvestmentsApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretConfigurationTests {

    // Passed as command-line arguments so they take precedence over application.properties.
    private static final String[] H2 = {
        "--spring.datasource.url=jdbc:h2:mem:jwtconfig;DB_CLOSE_DELAY=-1",
        "--spring.datasource.username=sa",
        "--spring.datasource.password=",
        "--spring.datasource.driver-class-name=org.h2.Driver",
        "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "--spring.jpa.hibernate.ddl-auto=create-drop"
    };

    private ConfigurableApplicationContext start(String jwtSecret, String... profiles) {
        return new SpringApplicationBuilder(AspiringInvestmentsApplication.class)
            .web(WebApplicationType.NONE)
            .profiles(profiles)
            .run(concat(H2, "--jwt.secret=" + jwtSecret));
    }

    private static String[] concat(String[] args, String extra) {
        String[] all = java.util.Arrays.copyOf(args, args.length + 1);
        all[args.length] = extra;
        return all;
    }

    @Test
    void refusesToStartWithoutJwtSecret() {
        assertThatThrownBy(() -> start("").close())
            .hasStackTraceContaining("JWT_SECRET");
    }

    @Test
    void refusesToStartWithShortJwtSecret() {
        assertThatThrownBy(() -> start("too-short").close())
            .hasStackTraceContaining("at least 32 bytes");
    }

    @Test
    void startsWithStrongJwtSecret() {
        try (ConfigurableApplicationContext ctx = start("test-only-jwt-secret-0123456789abcdef")) {
            JwtTokenProvider provider = ctx.getBean(JwtTokenProvider.class);
            String token = provider.createToken("00000000-0000-0000-0000-000000000001", "a@example.com");
            assertThat(provider.validateToken(token)).isTrue();
        }
    }

    @Test
    void devProfileStartsWithoutJwtSecret() {
        try (ConfigurableApplicationContext ctx = start("", "dev")) {
            JwtTokenProvider provider = ctx.getBean(JwtTokenProvider.class);
            String token = provider.createToken("00000000-0000-0000-0000-000000000001", "a@example.com");
            assertThat(provider.validateToken(token)).isTrue();
        }
    }
}
