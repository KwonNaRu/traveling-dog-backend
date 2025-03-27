package com.travelingdog.backend.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.h2gis.functions.factory.H2GISFunctions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

@Configuration
@Profile("test")
public class H2GisTestConfig {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        try {
            // 이미 설정된 DataSource 사용
            Connection connection = dataSource.getConnection();
            H2GISFunctions.load(connection);
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("H2GIS 초기화 실패", e);
        }
    }
}