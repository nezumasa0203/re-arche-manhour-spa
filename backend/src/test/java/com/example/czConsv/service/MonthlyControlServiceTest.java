package com.example.czConsv.service;

import com.example.czConsv.dao.Mcz04CtrlDao;
import com.example.czConsv.entity.Mcz04Ctrl;
import com.example.czConsv.exception.CzBusinessException;
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

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MonthlyControlService の単体テスト。
 *
 * <p>Mockito で Mcz04CtrlDao をモック化し、CzSecurityContext を直接設定して
 * 権限チェック・ビジネスルール・フラグ更新を検証する。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonthlyControlService: 月次制御操作")
class MonthlyControlServiceTest {

    @Mock
    private Mcz04CtrlDao ctrlDao;

    @InjectMocks
    private MonthlyControlService service;

    @AfterEach
    void tearDown() {
        CzSecurityContext.clear();
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    /**
     * テスト用の CzPrincipal を生成する。
     *
     * @param canInputPeriod canInputPeriodCondition (tab012 bit0)
     * @param canAggregate   canAggregatePeriod (tab012 bit1)
     * @param jinjiMode      人事モード
     * @return CzPrincipal
     */
    private CzPrincipal createPrincipal(boolean canInputPeriod,
                                        boolean canAggregate,
                                        boolean jinjiMode) {
        TabPermission tab010 = new TabPermission(
                Map.of("bit0", true, "bit1", true, "bit2", false));
        TabPermission tab011 = new TabPermission(
                Map.of("bit0", true, "bit1", true));
        TabPermission tab012 = new TabPermission(
                Map.of("bit0", canInputPeriod, "bit1", canAggregate));
        DataAuthority da = new DataAuthority("KA", "KA", "KA");
        CzPermissions perms = new CzPermissions(
                jinjiMode, tab010, tab011, tab012, da,
                EmploymentType.OFFICIAL, null, false);
        return new CzPrincipal(
                "user1", "User", "user@test.com",
                "100210", "IT部", perms);
    }

    /**
     * テスト用 Mcz04Ctrl エンティティを生成する。
     */
    private Mcz04Ctrl createCtrl(String sysid, String yyyymm,
                                 String gjktFlg, String dataSkFlg) {
        Mcz04Ctrl ctrl = new Mcz04Ctrl();
        ctrl.sysid = sysid;
        ctrl.yyyymm = yyyymm;
        ctrl.gjktFlg = gjktFlg;
        ctrl.dataSkFlg = dataSkFlg;
        ctrl.onlineFlg = "1";
        ctrl.renketsuFlg = "0";
        ctrl.delflg = "0";
        return ctrl;
    }

    // ========================================================================
    // monthlyConfirm
    // ========================================================================

    @Nested
    @DisplayName("monthlyConfirm: 月次確認")
    class MonthlyConfirm {

        @Test
        @DisplayName("正常系: 権限あり → gjktFlg=1, dataSkFlg=0 に更新される")
        void happyPath() {
            // Arrange
            CzSecurityContext.set(createPrincipal(true, false, false));
            Mcz04Ctrl ctrl = createCtrl("00", "202603", "0", "0");
            when(ctrlDao.selectById("00", "202603"))
                    .thenReturn(Optional.of(ctrl));
            when(ctrlDao.update(any(Mcz04Ctrl.class))).thenReturn(1);

            // Act
            Mcz04Ctrl result = service.monthlyConfirm("202603", "100210");

            // Assert
            assertNotNull(result);
            assertEquals("1", result.gjktFlg);
            assertEquals("0", result.dataSkFlg);
            verify(ctrlDao).update(ctrl);
        }

        @Test
        @DisplayName("異常系: 権限なし → CZ-106 で拒否")
        void noPermission() {
            // Arrange
            CzSecurityContext.set(createPrincipal(false, false, false));

            // Act & Assert
            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.monthlyConfirm("202603", "100210"));
            assertEquals("CZ-106", ex.getCode());
            verify(ctrlDao, never()).update(any());
        }

        @Test
        @DisplayName("異常系: レコード未存在 → CZ-300 システムエラー")
        void notFound() {
            // Arrange
            CzSecurityContext.set(createPrincipal(true, false, false));
            when(ctrlDao.selectById("00", "202603"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.monthlyConfirm("202603", "100210"));
            assertEquals("CZ-300", ex.getCode());
            verify(ctrlDao, never()).update(any());
        }

        @Test
        @DisplayName("正常系: jinjiMode=true → sysid=01 で検索される")
        void jinjiMode() {
            // Arrange
            CzSecurityContext.set(createPrincipal(true, false, true));
            Mcz04Ctrl ctrl = createCtrl("01", "202603", "0", "0");
            when(ctrlDao.selectById("01", "202603"))
                    .thenReturn(Optional.of(ctrl));
            when(ctrlDao.update(any(Mcz04Ctrl.class))).thenReturn(1);

            // Act
            Mcz04Ctrl result = service.monthlyConfirm("202603", "100210");

            // Assert
            assertEquals("01", result.sysid);
            assertEquals("1", result.gjktFlg);
        }
    }

    // ========================================================================
    // monthlyAggregate
    // ========================================================================

    @Nested
    @DisplayName("monthlyAggregate: 月次集約")
    class MonthlyAggregate {

        @Test
        @DisplayName("正常系: 権限あり＋確認済 → dataSkFlg=1 に更新される")
        void happyPath() {
            // Arrange
            CzSecurityContext.set(createPrincipal(false, true, false));
            Mcz04Ctrl ctrl = createCtrl("00", "202603", "1", "0");
            when(ctrlDao.selectById("00", "202603"))
                    .thenReturn(Optional.of(ctrl));
            when(ctrlDao.update(any(Mcz04Ctrl.class))).thenReturn(1);

            // Act
            Mcz04Ctrl result = service.monthlyAggregate("202603", "100210");

            // Assert
            assertNotNull(result);
            assertEquals("1", result.dataSkFlg);
            verify(ctrlDao).update(ctrl);
        }

        @Test
        @DisplayName("異常系: 権限なし → CZ-106 で拒否")
        void noPermission() {
            // Arrange
            CzSecurityContext.set(createPrincipal(false, false, false));

            // Act & Assert
            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.monthlyAggregate("202603", "100210"));
            assertEquals("CZ-106", ex.getCode());
            verify(ctrlDao, never()).update(any());
        }

        @Test
        @DisplayName("異常系: 未確認 (gjktFlg=0) → CZ-108 で拒否")
        void notConfirmed() {
            // Arrange
            CzSecurityContext.set(createPrincipal(false, true, false));
            Mcz04Ctrl ctrl = createCtrl("00", "202603", "0", "0");
            when(ctrlDao.selectById("00", "202603"))
                    .thenReturn(Optional.of(ctrl));

            // Act & Assert
            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.monthlyAggregate("202603", "100210"));
            assertEquals("CZ-108", ex.getCode());
            verify(ctrlDao, never()).update(any());
        }
    }

    // ========================================================================
    // monthlyUnconfirm
    // ========================================================================

    @Nested
    @DisplayName("monthlyUnconfirm: 月次確認取消")
    class MonthlyUnconfirm {

        @Test
        @DisplayName("正常系: 権限あり → gjktFlg=0, dataSkFlg=0 にリセットされる")
        void happyPath() {
            // Arrange
            CzSecurityContext.set(createPrincipal(true, false, false));
            Mcz04Ctrl ctrl = createCtrl("00", "202603", "1", "1");
            when(ctrlDao.selectById("00", "202603"))
                    .thenReturn(Optional.of(ctrl));
            when(ctrlDao.update(any(Mcz04Ctrl.class))).thenReturn(1);

            // Act
            Mcz04Ctrl result = service.monthlyUnconfirm("202603", "100210");

            // Assert
            assertNotNull(result);
            assertEquals("0", result.gjktFlg);
            assertEquals("0", result.dataSkFlg);
            verify(ctrlDao).update(ctrl);
        }

        @Test
        @DisplayName("異常系: 権限なし → CZ-106 で拒否")
        void noPermission() {
            // Arrange
            CzSecurityContext.set(createPrincipal(false, false, false));

            // Act & Assert
            CzBusinessException ex = assertThrows(CzBusinessException.class,
                    () -> service.monthlyUnconfirm("202603", "100210"));
            assertEquals("CZ-106", ex.getCode());
            verify(ctrlDao, never()).update(any());
        }
    }

    // ========================================================================
    // getControl
    // ========================================================================

    @Nested
    @DisplayName("getControl: コントロール情報取得")
    class GetControl {

        @Test
        @DisplayName("正常系: sysid + yyyymm でエンティティが返却される")
        void happyPath() {
            // Arrange
            CzSecurityContext.set(createPrincipal(false, false, false));
            Mcz04Ctrl ctrl = createCtrl("00", "202603", "1", "0");
            when(ctrlDao.selectById("00", "202603"))
                    .thenReturn(Optional.of(ctrl));

            // Act
            Mcz04Ctrl result = service.getControl("202603");

            // Assert
            assertNotNull(result);
            assertEquals("00", result.sysid);
            assertEquals("202603", result.yyyymm);
            assertEquals("1", result.gjktFlg);
        }
    }
}
