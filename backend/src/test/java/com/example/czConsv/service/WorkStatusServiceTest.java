package com.example.czConsv.service;

import com.example.czConsv.dao.Tcz01HosyuKousuuDao;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WorkStatusService の単体テスト。
 *
 * <p>Mockito で Tcz01HosyuKousuuDao / ValidationService をモック化し、
 * CzSecurityContext を直接設定して権限チェック・ステータス遷移・
 * 楽観ロックを検証する。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkStatusService: 作業ステータス管理")
class WorkStatusServiceTest {

    @Mock
    private Tcz01HosyuKousuuDao workHoursDao;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private WorkStatusService service;

    @AfterEach
    void tearDown() {
        CzSecurityContext.clear();
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    /**
     * テスト用 CzPrincipal を生成する。
     *
     * @param canManage     canManageReports (tab010 bit1)
     * @param canFullManage canFullManage (tab010 bit2)
     */
    private CzPrincipal createPrincipal(boolean canManage, boolean canFullManage) {
        TabPermission tab010 = new TabPermission(
                Map.of("bit0", true, "bit1", canManage, "bit2", canFullManage));
        TabPermission tab011 = new TabPermission(Map.of("bit0", true, "bit1", true));
        TabPermission tab012 = new TabPermission(Map.of("bit0", true, "bit1", false));
        DataAuthority da = new DataAuthority("KA", "KA", "KA");
        CzPermissions perms = new CzPermissions(
                false, tab010, tab011, tab012, da,
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
        e.yearHalf = "2025-02";
        e.status = status;
        e.delflg = "0";
        e.kousuu = BigDecimal.valueOf(210);
        e.upddate = LocalDateTime.of(2025, 2, 25, 10, 30, 0);
        return e;
    }

    // ========================================================================
    // search
    // ========================================================================

    @Nested
    @DisplayName("search: 作業ステータス検索")
    class Search {

        @BeforeEach
        void setUp() {
            CzSecurityContext.set(createPrincipal(false, false));
        }

        @Test
        @DisplayName("正常系: yearMonth + skbtcd + staffId でフィルタされる")
        void withStaffId() {
            Tcz01HosyuKousuu match = buildEntity(1L, "0");
            Tcz01HosyuKousuu other = buildEntity(2L, "0");
            other.hssgytntEsqid = "user2";
            Tcz01HosyuKousuu deleted = buildEntity(3L, "0");
            deleted.delflg = "1"; // 論理削除済み
            when(workHoursDao.selectAll()).thenReturn(List.of(match, other, deleted));

            List<Tcz01HosyuKousuu> result =
                    service.search("2025-02", "00", "user1");

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).seqNo);
        }

        @Test
        @DisplayName("正常系: staffId=null の場合は全担当者を返す")
        void staffIdNull() {
            Tcz01HosyuKousuu e1 = buildEntity(1L, "0");
            Tcz01HosyuKousuu e2 = buildEntity(2L, "1");
            e2.hssgytntEsqid = "user2";
            when(workHoursDao.selectAll()).thenReturn(List.of(e1, e2));

            List<Tcz01HosyuKousuu> result =
                    service.search("2025-02", "00", null);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("正常系: 論理削除済み(delflg!=0)は除外される")
        void excludesDeleted() {
            Tcz01HosyuKousuu active = buildEntity(1L, "0");
            Tcz01HosyuKousuu deleted = buildEntity(2L, "0");
            deleted.delflg = "1";
            when(workHoursDao.selectAll()).thenReturn(List.of(active, deleted));

            List<Tcz01HosyuKousuu> result =
                    service.search("2025-02", "00", null);

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).seqNo);
        }

        @Test
        @DisplayName("正常系: yearMonth が一致しないレコードは除外される")
        void excludesDifferentYearMonth() {
            Tcz01HosyuKousuu match = buildEntity(1L, "0");
            Tcz01HosyuKousuu other = buildEntity(2L, "0");
            other.yearHalf = "2025-03"; // 別月
            when(workHoursDao.selectAll()).thenReturn(List.of(match, other));

            List<Tcz01HosyuKousuu> result =
                    service.search("2025-02", "00", null);

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).seqNo);
        }
    }

    // ========================================================================
    // updateHours
    // ========================================================================

    @Nested
    @DisplayName("updateHours: 工数インライン編集")
    class UpdateHours {

        @Test
        @DisplayName("正常系: canManageReports 権限 → 工数更新")
        void happyPath() {
            CzSecurityContext.set(createPrincipal(true, false));
            Tcz01HosyuKousuu entity = buildEntity(1L, "1");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));
            when(validationService.validateFieldUpdate(eq("hours"), eq("04:00"),
                    anyInt(), any())).thenReturn(List.of());
            when(validationService.parseHoursToMinutes("04:00")).thenReturn(240);
            when(workHoursDao.update(any())).thenReturn(1);

            Tcz01HosyuKousuu result = service.updateHours(
                    1L, "00", "04:00",
                    LocalDateTime.of(2025, 2, 25, 10, 30, 0));

            assertNotNull(result);
            assertEquals(BigDecimal.valueOf(240), result.kousuu);
            verify(workHoursDao).update(entity);
        }

        @Test
        @DisplayName("異常系: 権限なし → CZ-106")
        void noPermission() {
            CzSecurityContext.set(createPrincipal(false, false));

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.updateHours(1L, "00", "04:00", LocalDateTime.now()));

            assertEquals("CZ-106", ex.getCode());
            verify(workHoursDao, never()).update(any());
        }

        @Test
        @DisplayName("異常系: 楽観ロック衝突 → OptimisticLockException")
        void optimisticLockConflict() {
            CzSecurityContext.set(createPrincipal(true, false));
            Tcz01HosyuKousuu entity = buildEntity(1L, "1");
            entity.upddate = LocalDateTime.of(2025, 2, 25, 11, 0, 0); // 別の時刻
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));

            assertThrows(OptimisticLockException.class,
                    () -> service.updateHours(1L, "00", "04:00",
                            LocalDateTime.of(2025, 2, 25, 10, 30, 0)));

            verify(workHoursDao, never()).update(any());
        }

        @Test
        @DisplayName("異常系: バリデーションエラー → CzBusinessException")
        void validationError() {
            CzSecurityContext.set(createPrincipal(true, false));
            Tcz01HosyuKousuu entity = buildEntity(1L, "1");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));
            ValidationError error = new ValidationError("CZ-147", "工数は15分単位で入力してください", "hours");
            when(validationService.validateFieldUpdate(eq("hours"), any(),
                    anyInt(), any())).thenReturn(List.of(error));

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.updateHours(1L, "00", "03:10",
                            LocalDateTime.of(2025, 2, 25, 10, 30, 0)));

            assertEquals("CZ-147", ex.getCode());
            verify(workHoursDao, never()).update(any());
        }

        @Test
        @DisplayName("異常系: レコード未存在 → CZ-300")
        void recordNotFound() {
            CzSecurityContext.set(createPrincipal(true, false));
            when(workHoursDao.selectById(99L, "00")).thenReturn(Optional.empty());

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.updateHours(99L, "00", "04:00", LocalDateTime.now()));

            assertEquals("CZ-300", ex.getCode());
        }

        @Test
        @DisplayName("正常系: canFullManage 権限でも更新できる")
        void withFullManagePermission() {
            CzSecurityContext.set(createPrincipal(false, true));
            Tcz01HosyuKousuu entity = buildEntity(1L, "1");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));
            when(validationService.validateFieldUpdate(any(), any(), anyInt(), any()))
                    .thenReturn(List.of());
            when(validationService.parseHoursToMinutes("02:00")).thenReturn(120);
            when(workHoursDao.update(any())).thenReturn(1);

            Tcz01HosyuKousuu result = service.updateHours(
                    1L, "00", "02:00",
                    LocalDateTime.of(2025, 2, 25, 10, 30, 0));

            assertNotNull(result);
        }
    }

    // ========================================================================
    // approve
    // ========================================================================

    @Nested
    @DisplayName("approve: ステータス承認（STATUS_1→2）")
    class Approve {

        @Test
        @DisplayName("正常系: STATUS_1 → STATUS_2 に更新される")
        void happyPath() {
            // canManage=true, canFullManage=false → useTanSeries()=false → MAN_MATRIX
            // STATUS_1 の statusKey="100", MAN_MATRIX statusUpdate[index=3]=1=ENABLED
            CzSecurityContext.set(createPrincipal(true, false));
            Tcz01HosyuKousuu entity = buildEntity(1L, "1");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));
            when(workHoursDao.update(any())).thenReturn(1);

            int count = service.approve(List.of(1L), "00");

            assertEquals(1, count);
            assertEquals("2", entity.status);
            verify(workHoursDao).update(entity);
        }

        @Test
        @DisplayName("正常系: 複数レコードを一括承認")
        void multipleRecords() {
            CzSecurityContext.set(createPrincipal(true, false));
            Tcz01HosyuKousuu e1 = buildEntity(1L, "1");
            Tcz01HosyuKousuu e2 = buildEntity(2L, "1");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(e1));
            when(workHoursDao.selectById(2L, "00")).thenReturn(Optional.of(e2));
            when(workHoursDao.update(any())).thenReturn(1);

            int count = service.approve(List.of(1L, 2L), "00");

            assertEquals(2, count);
            assertEquals("2", e1.status);
            assertEquals("2", e2.status);
        }

        @Test
        @DisplayName("異常系: STATUS_1 以外のレコード → CZ-107")
        void wrongStatus() {
            CzSecurityContext.set(createPrincipal(true, false));
            Tcz01HosyuKousuu entity = buildEntity(1L, "0"); // STATUS_0
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.approve(List.of(1L), "00"));

            assertEquals("CZ-107", ex.getCode());
            verify(workHoursDao, never()).update(any());
        }

        @Test
        @DisplayName("異常系: 権限なし → CZ-106")
        void noPermission() {
            CzSecurityContext.set(createPrincipal(false, false));

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.approve(List.of(1L), "00"));

            assertEquals("CZ-106", ex.getCode());
        }

        @Test
        @DisplayName("異常系: レコード未存在 → CZ-300")
        void recordNotFound() {
            CzSecurityContext.set(createPrincipal(true, false));
            when(workHoursDao.selectById(99L, "00")).thenReturn(Optional.empty());

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.approve(List.of(99L), "00"));

            assertEquals("CZ-300", ex.getCode());
        }
    }

    // ========================================================================
    // revert
    // ========================================================================

    @Nested
    @DisplayName("revert: 承認取消（STATUS_2→1）")
    class Revert {

        @Test
        @DisplayName("正常系: STATUS_2 → STATUS_1 に更新される")
        void happyPath() {
            CzSecurityContext.set(createPrincipal(true, false));
            Tcz01HosyuKousuu entity = buildEntity(1L, "2");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));
            when(workHoursDao.update(any())).thenReturn(1);

            int count = service.revert(List.of(1L), "00");

            assertEquals(1, count);
            assertEquals("1", entity.status);
            verify(workHoursDao).update(entity);
        }

        @Test
        @DisplayName("正常系: 複数レコードを一括取消")
        void multipleRecords() {
            CzSecurityContext.set(createPrincipal(true, false));
            Tcz01HosyuKousuu e1 = buildEntity(1L, "2");
            Tcz01HosyuKousuu e2 = buildEntity(2L, "2");
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(e1));
            when(workHoursDao.selectById(2L, "00")).thenReturn(Optional.of(e2));
            when(workHoursDao.update(any())).thenReturn(1);

            int count = service.revert(List.of(1L, 2L), "00");

            assertEquals(2, count);
        }

        @Test
        @DisplayName("異常系: STATUS_2 以外のレコード → CZ-108")
        void wrongStatus() {
            CzSecurityContext.set(createPrincipal(true, false));
            Tcz01HosyuKousuu entity = buildEntity(1L, "1"); // STATUS_1
            when(workHoursDao.selectById(1L, "00")).thenReturn(Optional.of(entity));

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.revert(List.of(1L), "00"));

            assertEquals("CZ-108", ex.getCode());
            verify(workHoursDao, never()).update(any());
        }

        @Test
        @DisplayName("異常系: 権限なし → CZ-106")
        void noPermission() {
            CzSecurityContext.set(createPrincipal(false, false));

            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.revert(List.of(1L), "00"));

            assertEquals("CZ-106", ex.getCode());
        }
    }
}
