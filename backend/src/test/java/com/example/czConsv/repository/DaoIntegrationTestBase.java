package com.example.czConsv.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * DAO 統合テスト共通基底クラス。
 *
 * <p>シングルトンパターンで PostgreSQL 16 コンテナを JVM 全体で1つだけ起動する。
 * 全サブクラスが同一コンテナを共有し、Spring テストコンテキストのキャッシュと共存する。
 * 各テストは {@code @Transactional} でロールバックされる。
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public abstract class DaoIntegrationTestBase {

    @SuppressWarnings({"resource", "rawtypes"})
    static final PostgreSQLContainer POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("cz_test")
                .withUsername("postgres")
                .withPassword("postgres")
                .withInitScript("sql/init.sql");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
