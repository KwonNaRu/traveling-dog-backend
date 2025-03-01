/* src/test/resources/schema.sql */
-- H2 데이터베이스 테스트용 스키마
-- 필요한 테이블 스키마만 정의합니다.

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 여행 계획 테이블
CREATE TABLE IF NOT EXISTS travel_plans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    country VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 여행 장소 테이블
CREATE TABLE IF NOT EXISTS travel_locations (
    id BIGSERIAL PRIMARY KEY,
    travel_plan_id BIGINT,
    place_name VARCHAR(255) NOT NULL,
    description TEXT,
    longitude DOUBLE PRECISION,
    latitude DOUBLE PRECISION,
    location_order INT,
    available_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (travel_plan_id) REFERENCES travel_plans(id)
);