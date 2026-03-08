package com.example.czConsv.schema;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Savepoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * スキーマテスト共通基底クラス。
 * Docker Compose で起動中の PostgreSQL に直接接続する。
 * テスト実行前に docker compose up db が完了していること。
 *
 * 各テストはトランザクション内で実行し、終了後にロールバックする。
 * これにより、テスト間のデータ汚染を防止する。
 *
 * コンテナ内実行時: jdbc:postgresql://db:5432/cz_migration_dev
 * ホスト実行時:     jdbc:postgresql://localhost:5433/cz_migration_dev
 */
abstract class PostgresTestBase {

    private static final String JDBC_URL;
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "postgres";

    static {
        String host = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "db";
        String port = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "5432";
        String dbName = System.getenv("POSTGRES_DB") != null ? System.getenv("POSTGRES_DB") : "cz_migration_dev";
        JDBC_URL = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    }

    /** テスト共有コネクション（トランザクション制御用） */
    protected Connection sharedConn;
    private Savepoint savepoint;

    @BeforeEach
    void setUpTransaction() throws SQLException {
        sharedConn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
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
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }
}
