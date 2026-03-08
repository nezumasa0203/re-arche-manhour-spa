package com.example.czConsv.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-027: 論理削除ポリシー適用検証テスト
 * 全 DAO の全検索クエリが delflg = '0' 条件を含むことを検証。
 * 物理削除メソッドが存在しないことを確認。
 */
class LogicalDeletePolicyTest {

    /** delflg カラムを持たないテーブルの DAO（除外対象） */
    private static final Set<String> NO_DELFLG_DAOS = Set.of(
            "Mcz12OrgnKrDao", "Mcz21KanriTaisyoDao", "BatchExecutionLogDao"
    );

    @Test
    @DisplayName("delflg を持つテーブルの全 SELECT SQL に delflg = '0' が含まれる")
    void allSelectSqlContainDelflgCondition() throws Exception {
        Path sqlRoot = findSqlRoot();
        List<String> violations = new ArrayList<>();

        Files.walkFileTree(sqlRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                String daoDir = file.getParent().getFileName().toString();

                if (!fileName.endsWith(".sql")) return FileVisitResult.CONTINUE;
                if (!fileName.startsWith("select")) return FileVisitResult.CONTINUE;
                if (NO_DELFLG_DAOS.contains(daoDir)) return FileVisitResult.CONTINUE;

                String content = Files.readString(file);
                if (!content.contains("delflg = '0'")) {
                    violations.add(daoDir + "/" + fileName);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        assertTrue(violations.isEmpty(),
                "以下の SELECT SQL に delflg = '0' が不足: " + violations);
    }

    @Test
    @DisplayName("delflg を持たないテーブルの SQL に delflg 条件がない")
    void noDelflgDaosShouldNotHaveDelflgCondition() throws Exception {
        Path sqlRoot = findSqlRoot();
        List<String> violations = new ArrayList<>();

        for (String daoName : NO_DELFLG_DAOS) {
            Path daoDir = sqlRoot.resolve(daoName);
            if (!Files.exists(daoDir)) continue;

            try (var stream = Files.list(daoDir)) {
                stream.filter(f -> f.toString().endsWith(".sql")).forEach(file -> {
                    try {
                        String content = Files.readString(file);
                        if (content.contains("delflg")) {
                            violations.add(daoName + "/" + file.getFileName());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        assertTrue(violations.isEmpty(),
                "delflg カラムがないテーブルの SQL に delflg 条件が存在: " + violations);
    }

    @ParameterizedTest
    @DisplayName("物理削除メソッド (@Delete) が DAO に存在しない")
    @ValueSource(classes = {
            Tcz01HosyuKousuuDao.class,
            Mcz04CtrlDao.class,
            Mcz02HosyuKategoriDao.class,
            Mav01SysDao.class,
            Mav03SubsysDao.class,
            Mcz15TsSysDao.class,
            Mcz03AplBunruiGrpDao.class,
            Mcz17HshkBunruiGrpDao.class,
            Mcz12OrgnKrDao.class,
            Mcz24TankaDao.class,
            Mcz21KanriTaisyoDao.class,
            Tcz13SubsysSumDao.class,
            Tcz14GrpKeyDao.class,
            Tcz19MySysDao.class,
            Tcz16TntBusyoRirekiDao.class,
            BatchExecutionLogDao.class
    })
    void noPhysicalDeleteMethod(Class<?> daoClass) {
        for (Method method : daoClass.getDeclaredMethods()) {
            assertFalse(
                    method.isAnnotationPresent(org.seasar.doma.Delete.class),
                    daoClass.getSimpleName() + " に @Delete メソッドが存在: " + method.getName()
            );
        }
    }

    @Test
    @DisplayName("logicalDelete SQL に delflg = '1' への UPDATE が含まれる")
    void logicalDeleteSqlSetsDelflgToOne() throws Exception {
        Path sqlRoot = findSqlRoot();
        List<String> violations = new ArrayList<>();

        Files.walkFileTree(sqlRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.getFileName().toString().equals("logicalDelete.sql"))
                    return FileVisitResult.CONTINUE;

                String content = Files.readString(file);
                if (!content.contains("delflg = '1'")) {
                    violations.add(file.getParent().getFileName() + "/logicalDelete.sql");
                }
                return FileVisitResult.CONTINUE;
            }
        });

        assertTrue(violations.isEmpty(),
                "logicalDelete SQL に delflg = '1' 設定が不足: " + violations);
    }

    private Path findSqlRoot() {
        // テスト実行環境に応じて SQL ルートを探索
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Path sqlRoot = projectRoot.resolve("src/main/resources/META-INF/com/example/czConsv/dao");
        if (Files.exists(sqlRoot)) return sqlRoot;

        // フォールバック: 相対パス
        sqlRoot = Path.of("src/main/resources/META-INF/com/example/czConsv/dao");
        assertTrue(Files.exists(sqlRoot), "SQL ルートディレクトリが見つかりません: " + sqlRoot);
        return sqlRoot;
    }
}
