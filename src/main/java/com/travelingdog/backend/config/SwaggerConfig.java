package com.travelingdog.backend.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Traveling Dog API")
                .description("여행 계획을 생성하고 관리하는 API입니다.")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("Traveling Dog Team")
                        .email("contact@travelingdog.com")
                        .url("https://travelingdog.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://springdoc.org"));

        // JWT 인증 스키마 설정
        SecurityScheme jwtSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // Basic 인증 스키마 설정 추가
        SecurityScheme basicSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // JWT 보안 요구사항
        SecurityRequirement jwtSecurityRequirement = new SecurityRequirement().addList("bearerAuth");

        // Basic 보안 요구사항은 전역으로 적용하지 않음 (로그인 API에만 적용)
        return new OpenAPI()
                .info(info)
                .servers(Arrays.asList(
                        new Server().url("/").description("현재 서버"),
                        new Server().url("https://api.travelingdog.com").description("운영 서버")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", jwtSecurityScheme)
                        .addSecuritySchemes("basicAuth", basicSecurityScheme))
                .addSecurityItem(jwtSecurityRequirement);
    }
}
