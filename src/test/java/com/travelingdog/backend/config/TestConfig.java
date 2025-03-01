package com.travelingdog.backend.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.travelingdog.backend.service.TripPlanService;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public TripPlanService tripPlanService() {
        return Mockito.mock(TripPlanService.class);
    }
}