package com.example.czConsv.service;

import com.example.czConsv.dao.Tcz01HosyuKousuuDao;
import com.example.czConsv.dto.request.WorkHoursCreateRequest;
import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.exception.CzBusinessException;
import com.example.czConsv.exception.OptimisticLockException;
import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.model.CzPermissions;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.security.model.DataAuthority;
import com.example.czConsv.security.model.EmploymentType;
import com.example.czConsv.security.model.TabPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WorkHoursService の単体テスト。
 *
 * <p>Mockito で Tcz01HosyuKousuuDao / ValidationService をモック化し、
 * CzSecurityContext を直接設定してビジネスロジックを検証する。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkHoursService: 工数管理ビジネスロジック")
class WorkHoursServiceTest {

    @Mock
    private Tcz01HosyuKousuuDao workHoursDao;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private WorkHoursService service;

    @AfterEach
    void tearDown() {
        CzSecurityContext.clear();
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    private CzPrincipal createPrincipal(boolean canManage, boolean canFullManage,
                                        boolean jinjiMode) {
        TabPermission tab010 = new TabPermission(
                Map.of("bit0", true, "bit1", canManage, "bit2", canFullManage));
        TabPermission tab011 = new TabPermission(Map.of("bit0", true, "bit1", true));
        TabPermission tab012 = new TabPermission(Map.of("bit0", true, "bit1", false));
        DataAuthority da = new DataAuthority("KA", "KA", "KA");
        CzPermissions perms = new CzPermissions(
                jinjiMode, tab010, tab011, tab012, da,
                EmploymentType.OFFICIAL, null, false);
        return new CzPrincipal(
                "user1", "テストユーザー", "user@test.com",
                "100210", "IT部", perms);
    }

    private Tcz01HosyuKousuu buildEntity(Long seqNo, String status) {
        Tcz01HosyuKousuu e = new Tcz01HosyuKousuu();
        e.seqNo = seqNo;
        e.skbtcd = "00";
        e.hssgytntEsqid = "user1";
        e.hssgytntName = "テストユーザー";
        e.yearHalf = "2025-02";
        e.status = status;
        e.delflg = "0";
        e.kousuu = BigDecimal.valueOf(210); // 3:30
        e.upddate = LocalDateTime.of(2025, 2, 25, 10, 30, 0);
        return e;
    }

    // ========================================================================
    // fetchByMonth
    // ========================================================================

    @Nested
    @DisplayName("fetchByMonth: 月次一覧取得")
    class FetchByMonth {

        @Test
        @DisplayName("正常系: DAO の結果をそのまま返す")
        void happyPath() {
            List<Tcz01HosyuKousuu> expected = List.of(buildEntity(1L, "0"));
            when(workHoursDao.selectByTntAndPeriod("user1", "2025-02"))
                    .thenReturn(expected);

            List<Tcz01HosyuKousuu> result = service.fetchByMonth("user1", "2025-02");

            assertEquals(1, result.size());
            verify(workHoursDao).selectByTntAndPeriod("user1", "2025-02");
        }

        @Test
        @DisplayName("正常系: 空リストの場合も正常に返す")
        void emptyResult() {
            when(workHoursDao.selectByTntAndPeriod("user1", "2025-02"))
                    .thenReturn(List.of());

            List<Tcz01HosyuKousuu> result = service.fetchByMonth("user1", "2025-02");

            assertEquals(0, result.size());
        }
    }

    // ========================================================================
    // create
    // ========================================================================

    @Nested
    @DisplayName("create: 工数レコード新規作成")
    class Create {

        @BeforeEach
        void setUp() {
            CzSecurityContext.set(createPrincipal(false, false, false));
        }

        @Test
        @DisplayName("正常系(下書きモード): yearMonth のみ指定 → バリデーションなしで STATUS_0 作成")
        void draftMode() {
            when(workHoursDao.insert(any())).thenReturn(1);
            WorkHoursCreateRequest req = new WorkHoursCreateRequest(
                    "2025-02", null, null, null, null, null, null, null, null, null);

            Tcz01HosyuKousuu result = service.create(req);

            assertNotNull(result);
            assertEquals("0", result.status);
            assertEquals("0", result.delflg);
            assertEquals("user1", result.hssgytntEsqid);
            verify(validationService, never())
                    .validateWorkHoursInput(any(), any(), any(), any(), any(),
                            any(), any(), any(), any(), any(), any(), anyInt());
            verify(workHoursDao).insert(any());
        }

        @Test
        @DisplayName("正常系(フルモード): 全フィールド指定 → バリデーション後 STATUS_0 作成")
        void fullMode() {
            when(workHoursDao.insert(any())).thenReturn(1);
            when(validationService.validateWorkHoursInput(
                    any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());
            when(validationService.parseHoursToMinutes("03:30")).thenReturn(210);

            WorkHoursCreateRequest req = new WorkHoursCreateRequest(
                    "2025-02", "2025-02-25", "SUB001", "SUB002",
                    "01", "障害対応", "03:30", "12345", "1234567", "山田太郎");

            Tcz01HosyuKousuu result = service.create(req);

            assertNotNull(result);
            assertEquals("0", result.status);
            verify(validationService).validateWorkHoursInput(
                    any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), anyInt());
            verify(workHoursDao).insert(any());
        }

        @Test
        @DisplayName("異常系(フルモード): バリデーションエラー → CzBusinessException")
        void fullModeValidationError() {
            ValidationError error = new ValidationError("CZ-126", "作業日は必須です", "workDate");
            when(validationService.validateWorkHoursInput(
                    any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of(error));

            WorkHoursCreateRequest req = new WorkHoursCreateRequest(
                    "2025-02", null, "SUB001", "SUB002",
                    "01", "障害対応", "03:30", null, null, null);

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.create(req));

            assertEquals("CZ-126", ex.getCode());
            verify(workHoursDao, never()).insert(any());
        }

        @Test
        @DisplayName("正常系: jinjiMode=true → skbtcd=01 でエンティティ生成")
        void jinjiMode() {
            when(workHoursDao.insert(any())).thenReturn(1);
            CzSecurityContext.set(createPrincipal(false, false, true));
            WorkHoursCreateRequest req = new WorkHoursCreateRequest(
                    "2025-02", null, null, null, null, null, null, null, null, null);

            Tcz01HosyuKousuu result = service.create(req);

            assertEquals("01", result.skbtcd);
        }
    }

    // ========================================================================
    // updateField
    // ========================================================================

    @Nested
    @DisplayName("updateField: フィールド単位インライン更新")
    class UpdateField {

        @BeforeEach
        void setUp() {
            CzSecurityContext.set(createPrincipal(false, false, false));
        }

        @Test
        @DisplayName("正常系: hours フィールド更新 → DAO update が呼ばれる")
        void happyPath() {
            Tcz01HosyuKousuu entity = buildEntity(1L, "0");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));
            when(validationService.validateFieldUpdate(eq("hours"), eq("04:00"),
                    anyInt(), any())).thenReturn(List.of());
            when(validationService.parseHoursToMinutes("04:00")).thenReturn(240);
            when(workHoursDao.update(any())).thenReturn(1);

            Tcz01HosyuKousuu result = service.updateField(
                    1L, "00", "hours", "04:00",
                    LocalDateTime.of(2025, 2, 25, 10, 30, 0));

            assertNotNull(result);
            verify(workHoursDao).update(entity);
        }

        @Test
        @DisplayName("異常系: 楽観ロック衝突 → OptimisticLockException")
        void optimisticLockConflict() {
            Tcz01HosyuKousuu entity = buildEntity(1L, "0");
            entity.upddate = LocalDateTime.of(2025, 2, 25, 11, 0, 0); // 別の時刻
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));

            assertThrows(OptimisticLockException.class,
                    () -> service.updateField(1L, "00", "hours", "04:00",
                            LocalDateTime.of(2025, 2, 25, 10, 30, 0)));

            verify(workHoursDao, never()).update(any());
        }

        @Test
        @DisplayName("異常系: レコード未存在 → CZ-104")
        void recordNotFound() {
            when(workHoursDao.selectById(99L, "00")).thenReturn(Optional.empty());

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.updateField(99L, "00", "hours", "04:00",
                            LocalDateTime.now()));

            assertEquals("CZ-104", ex.getCode());
        }

        @Test
        @DisplayName("異常系: 不明フィールド名 → CZ-125")
        void unknownField() {
            Tcz01HosyuKousuu entity = buildEntity(1L, "0");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));
            when(validationService.validateFieldUpdate(eq("unknownField"), any(),
                    anyInt(), any())).thenReturn(List.of());

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.updateField(1L, "00", "unknownField", "val",
                            LocalDateTime.of(2025, 2, 25, 10, 30, 0)));

            assertEquals("CZ-125", ex.getCode());
        }
    }

    // ========================================================================
    // delete
    // ========================================================================

    @Nested
    @DisplayName("delete: 一括論理削除")
    class Delete {

        @BeforeEach
        void setUp() {
            CzSecurityContext.set(createPrincipal(false, false, false));
        }

        @Test
        @DisplayName("正常系: 全件 STATUS_0 → 論理削除実行")
        void happyPath() {
            Tcz01HosyuKousuu e1 = buildEntity(1L, "0");
            Tcz01HosyuKousuu e2 = buildEntity(2L, "0");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(e1));
            when(workHoursDao.selectById(2L, "00")).thenReturn(Optional.of(e2));
            when(workHoursDao.logicalDelete(any())).thenReturn(1);

            int count = service.delete(List.of(1L, 2L), "00");

            assertEquals(2, count);
            verify(workHoursDao, times(2)).logicalDelete(any());
        }

        @Test
        @DisplayName("異常系: STATUS_0 以外を含む → CZ-106、削除中断")
        void containsNonDraft() {
            Tcz01HosyuKousuu e1 = buildEntity(1L, "0");
            Tcz01HosyuKousuu e2 = buildEntity(2L, "1"); // STATUS_1
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(e1));
            when(workHoursDao.selectById(2L, "00")).thenReturn(Optional.of(e2));

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.delete(List.of(1L, 2L), "00"));

            assertEquals("CZ-106", ex.getCode());
            verify(workHoursDao, never()).logicalDelete(any());
        }
    }

    // ========================================================================
    // copy
    // ========================================================================

    @Nested
    @DisplayName("copy: レコードコピー")
    class Copy {

        @Test
        @DisplayName("正常系: コピー後 STATUS=0 にリセットされる")
        void happyPath() {
            CzSecurityContext.set(createPrincipal(false, false, false));
            Tcz01HosyuKousuu source = buildEntity(1L, "1"); // STATUS_1 のレコードをコピー
            source.kenmei = "月次処理エラー修正";
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(source));
            when(workHoursDao.insert(any())).thenReturn(1);

            List<Tcz01HosyuKousuu> result = service.copy(List.of(1L), "00");

            assertEquals(1, result.size());
            assertEquals("0", result.get(0).status); // STATUS リセット確認
            assertEquals("月次処理エラー修正", result.get(0).kenmei);
            assertNull(result.get(0).seqNo); // seqNo は null（DB 採番）
        }
    }

    // ========================================================================
    // transferNextMonth
    // ========================================================================

    @Nested
    @DisplayName("transferNextMonth: 翌月転写")
    class TransferNextMonth {

        @Test
        @DisplayName("正常系: 指定月にコピー、作業日(sgyymd)がクリアされる")
        void happyPath() {
            CzSecurityContext.set(createPrincipal(false, false, false));
            Tcz01HosyuKousuu source = buildEntity(1L, "0");
            source.sgyymd = LocalDate.of(2025, 2, 25);
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(source));
            when(workHoursDao.insert(any())).thenReturn(1);

            List<Tcz01HosyuKousuu> result =
                    service.transferNextMonth(List.of(1L), "00", List.of("2025-03"));

            assertEquals(1, result.size());
            assertEquals("2025-03", result.get(0).yearHalf);
            assertNull(result.get(0).sgyymd); // 作業日クリア確認
        }

        @Test
        @DisplayName("正常系: 複数月に転写 → 月数分エンティティが作成される")
        void multipleMonths() {
            CzSecurityContext.set(createPrincipal(false, false, false));
            Tcz01HosyuKousuu source = buildEntity(1L, "0");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(source));
            when(workHoursDao.insert(any())).thenReturn(1);

            List<Tcz01HosyuKousuu> result =
                    service.transferNextMonth(List.of(1L), "00",
                            List.of("2025-03", "2025-04"));

            assertEquals(2, result.size());
        }
    }

    // ========================================================================
    // batchConfirm
    // ========================================================================

    @Nested
    @DisplayName("batchConfirm: 一括確認（STATUS_0→1）")
    class BatchConfirm {

        @BeforeEach
        void setUp() {
            CzSecurityContext.set(createPrincipal(false, false, false));
        }

        @Test
        @DisplayName("正常系: 下書きゼロ → 0 を返す")
        void noDrafts() {
            when(workHoursDao.selectByTntPeriodAndStatus("user1", "2025-02", "0"))
                    .thenReturn(List.of());

            int count = service.batchConfirm("2025-02");

            assertEquals(0, count);
            verify(workHoursDao, never()).update(any());
        }

        @Test
        @DisplayName("正常系: 全件バリデーション通過 → STATUS 0→1 に更新")
        void allValid() {
            Tcz01HosyuKousuu e1 = buildEntity(1L, "0");
            Tcz01HosyuKousuu e2 = buildEntity(2L, "0");
            e1.sgyymd = LocalDate.of(2025, 2, 25);
            e2.sgyymd = LocalDate.of(2025, 2, 26);
            when(workHoursDao.selectByTntPeriodAndStatus("user1", "2025-02", "0"))
                    .thenReturn(List.of(e1, e2));
            when(validationService.validateWorkHoursInput(
                    any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());
            when(workHoursDao.update(any())).thenReturn(1);

            int count = service.batchConfirm("2025-02");

            assertEquals(2, count);
            assertEquals("1", e1.status);
            assertEquals("1", e2.status);
        }

        @Test
        @DisplayName("異常系: バリデーションエラー → CzBusinessException（recordId 付き）")
        void validationError() {
            Tcz01HosyuKousuu e1 = buildEntity(101L, "0");
            e1.sgyymd = null; // 作業日なし → バリデーションエラー
            when(workHoursDao.selectByTntPeriodAndStatus("user1", "2025-02", "0"))
                    .thenReturn(List.of(e1));
            ValidationError error = new ValidationError("CZ-126", "作業日は必須です", "workDate");
            when(validationService.validateWorkHoursInput(
                    any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of(error));

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.batchConfirm("2025-02"));

            assertEquals("CZ-126", ex.getCode());
            assertEquals(101L, ex.getRecordId()); // recordId が設定されていること
            verify(workHoursDao, never()).update(any()); // STATUS 更新は行われない
        }
    }

    // ========================================================================
    // batchRevert
    // ========================================================================

    @Nested
    @DisplayName("batchRevert: 一括差戻し（STATUS_1→0）")
    class BatchRevert {

        @Test
        @DisplayName("正常系: canManageReports 権限あり → STATUS 1→0 に更新")
        void withManagePermission() {
            CzSecurityContext.set(createPrincipal(true, false, false));
            Tcz01HosyuKousuu e1 = buildEntity(1L, "1");
            when(workHoursDao.selectByTntPeriodAndStatus("user1", "2025-02", "1"))
                    .thenReturn(List.of(e1));
            when(workHoursDao.update(any())).thenReturn(1);

            int count = service.batchRevert("2025-02");

            assertEquals(1, count);
            assertEquals("0", e1.status);
        }

        @Test
        @DisplayName("正常系: canFullManage 権限あり → STATUS 1→0 に更新")
        void withFullManagePermission() {
            CzSecurityContext.set(createPrincipal(false, true, false));
            Tcz01HosyuKousuu e1 = buildEntity(1L, "1");
            when(workHoursDao.selectByTntPeriodAndStatus("user1", "2025-02", "1"))
                    .thenReturn(List.of(e1));
            when(workHoursDao.update(any())).thenReturn(1);

            int count = service.batchRevert("2025-02");

            assertEquals(1, count);
        }

        @Test
        @DisplayName("異常系: 権限なし → CZ-106")
        void noPermission() {
            CzSecurityContext.set(createPrincipal(false, false, false));

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.batchRevert("2025-02"));

            assertEquals("CZ-106", ex.getCode());
            verify(workHoursDao, never()).update(any());
        }

        @Test
        @DisplayName("正常系: 確認済みレコードなし → 0 を返す")
        void noConfirmed() {
            CzSecurityContext.set(createPrincipal(true, false, false));
            when(workHoursDao.selectByTntPeriodAndStatus("user1", "2025-02", "1"))
                    .thenReturn(List.of());

            int count = service.batchRevert("2025-02");

            assertEquals(0, count);
        }
    }
}
