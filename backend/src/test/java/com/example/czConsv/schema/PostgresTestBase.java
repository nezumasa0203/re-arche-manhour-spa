package com.example.czConsv.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * スキーマテスト共通基底クラス。
 * Testcontainers で PostgreSQL 16 コンテナをシングルトン起動し、
 * 全サブクラスが同一コンテナを共有する。
 *
 * <p>各テストはトランザクション内で実行し、終了後にロールバックする。
 * これにより、テスト間のデータ汚染を防止する。
 */
abstract class PostgresTestBase {

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

    /** テスト共有コネクション（トランザクション制御用） */
    protected Connection sharedConn;
    private Savepoint savepoint;

    @BeforeEach
    void setUpTransaction() throws SQLException {
        sharedConn = POSTGRES.createConnection("");
        sharedConn.setAutoCommit(false);
        savepoint = sharedConn.setSavepoint("test_savepoint");
    }

    @AfterEach
    void rollbackTransaction() throws SQLException {
        if (sharedConn != null) {
            try {
                sharedConn.rollback(savepoint);
            } finally {
                sharedConn.setAutoCommit(true);
                sharedConn.close();
            }
        }
    }

    protected Connection getConnection() throws SQLException {
        return POSTGRES.createConnection("");
    }
}
