package com.travelingdog.backend.config;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Primary;
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
// import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
// import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.serializer.RedisSerializer;

// import io.lettuce.core.ClientOptions;
// import io.lettuce.core.SocketOptions;

// @Configuration
public class RedisConfig {

    // @Value("${spring.data.redis.host:localhost}")
    // private String redisHost;

    // @Value("${spring.data.redis.port:6379}")
    // private int redisPort;

    // @Value("${spring.data.redis.password:}")
    // private String redisPassword;

    // @Value("${spring.data.redis.ssl.enabled:false}")
    // private boolean sslEnabled;

    // @Bean
    // @Primary
    // public RedisConnectionFactory redisConnectionFactory() {
    // RedisStandaloneConfiguration redisConfig = new
    // RedisStandaloneConfiguration();
    // redisConfig.setHostName(redisHost);
    // redisConfig.setPort(redisPort);

    // if (redisPassword != null && !redisPassword.isEmpty()) {
    // redisConfig.setPassword(redisPassword);
    // }

    // // Azure의 타임아웃과 재시도 정책을 처리하기 위한 설정
    // SocketOptions socketOptions = SocketOptions.builder()
    // .connectTimeout(Duration.ofSeconds(10))
    // .build();

    // ClientOptions clientOptions = ClientOptions.builder()
    // .socketOptions(socketOptions)
    // .autoReconnect(true)
    // .build();

    // LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
    // LettuceClientConfiguration.builder()
    // .clientOptions(clientOptions)
    // .commandTimeout(Duration.ofSeconds(5));

    // if (sslEnabled) {
    // // SSL 사용 + 인증서 검증 비활성화
    // builder.useSsl().disablePeerVerification();
    // }

    // LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig,
    // builder.build());
    // factory.setValidateConnection(true);
    // return factory;
    // }

    // @Bean
    // public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory
    // connectionFactory) {
    // RedisTemplate<String, Object> template = new RedisTemplate<>();
    // template.setConnectionFactory(connectionFactory);
    // template.setKeySerializer(RedisSerializer.string());
    // template.setValueSerializer(RedisSerializer.json());
    // template.setHashKeySerializer(RedisSerializer.string());
    // template.setHashValueSerializer(RedisSerializer.json());

    // // 연결 확인을 위한 초기화
    // template.afterPropertiesSet();

    // return template;
    // }
}