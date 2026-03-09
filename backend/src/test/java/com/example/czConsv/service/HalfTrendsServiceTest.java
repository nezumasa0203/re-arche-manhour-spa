package com.example.czConsv.service;

import com.example.czConsv.dao.Tcz13SubsysSumDao;
import com.example.czConsv.dto.response.HalfTrendsResponse;
import com.example.czConsv.entity.Tcz13SubsysSum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * HalfTrendsService 単体テスト。
 *
 * <p>STEP_0（分類別）、STEP_1（システム別）、STEP_2（サブシステム別）の
 * 3 階層ドリルダウン集計を検証する。
 */
@ExtendWith(MockitoExtension.class)
class HalfTrendsServiceTest {

    @Mock
    Tcz13SubsysSumDao subsysSumDao;

    @Mock
    FiscalYearResolver fiscalYearResolver;

    @InjectMocks
    HalfTrendsService halfTrendsService;

    @AfterEach
    void tearDown() {
        com.example.czConsv.security.CzSecurityContext.clear();
    }

    // =========================================================================
    // ヘルパー
    // =========================================================================

    private Tcz13SubsysSum rec(String yyyymm, String sknno, String subsknno,
                                String kategoriId, String skbtcd, BigDecimal hours) {
        Tcz13SubsysSum r = new Tcz13SubsysSum();
        r.yyyymm = yyyymm;
        r.sknno = sknno;
        r.subsknno = subsknno;
        r.hsKategoriId = kategoriId;
        r.skbtcd = skbtcd;
        r.hsKousuu = hours;
        r.delflg = "0";
        return r;
    }

    private Tcz13SubsysSum deleted(String yyyymm, String sknno, String subsknno,
                                    String kategoriId, String skbtcd, BigDecimal hours) {
        Tcz13SubsysSum r = rec(yyyymm, sknno, subsknno, kategoriId, skbtcd, hours);
        r.delflg = "1";
        return r;
    }

    // =========================================================================
    // STEP_0: getCategories
    // =========================================================================

    @Nested
    class GetCategories {

        @Test
        void aggregatesByCategory() {
            // FY2016 H1: 2016-01 〜 2016-06 (6ヶ月)
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    rec("201601", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("10")),
                    rec("201602", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("20")),
                    rec("201601", "SYS02", "SUB02", "CAT-B", "SKB1", new BigDecimal("5"))
            ));

            HalfTrendsResponse resp = halfTrendsService.getCategories("2016-1", "hours", "all");

            assertThat(resp.rows()).hasSize(2);

            HalfTrendsResponse.HalfTrendsRow catA = resp.rows().stream()
                    .filter(r -> "CAT-A".equals(r.key())).findFirst().orElseThrow();
            assertThat(catA.total()).isEqualByComparingTo("30");
            assertThat(catA.months()).hasSize(6);
            // 2016-01 = 10
            assertThat(catA.months().get(0).yearMonth()).isEqualTo("2016-01");
            assertThat(catA.months().get(0).value()).isEqualByComparingTo("10");
            // 2016-02 = 20
            assertThat(catA.months().get(1).value()).isEqualByComparingTo("20");
            // 残月 = 0
            assertThat(catA.months().get(2).value()).isEqualByComparingTo("0");

            HalfTrendsResponse.HalfTrendsRow catB = resp.rows().stream()
                    .filter(r -> "CAT-B".equals(r.key())).findFirst().orElseThrow();
            assertThat(catB.total()).isEqualByComparingTo("5");
        }

        @Test
        void excludesDeletedRecords() {
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    rec("201601",     "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("10")),
                    deleted("201601", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("99"))
            ));

            HalfTrendsResponse resp = halfTrendsService.getCategories("2016-1", "hours", "all");

            assertThat(resp.rows()).hasSize(1);
            assertThat(resp.rows().get(0).total()).isEqualByComparingTo("10");
        }

        @Test
        void excludesOutOfPeriodRecords() {
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    rec("201601", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("10")),
                    rec("201607", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("99")) // 対象外
            ));

            HalfTrendsResponse resp = halfTrendsService.getCategories("2016-1", "hours", "all");

            assertThat(resp.rows()).hasSize(1);
            assertThat(resp.rows().get(0).total()).isEqualByComparingTo("10");
        }

        @Test
        void drilldownContextContainsYearHalf() {
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of());

            HalfTrendsResponse resp = halfTrendsService.getCategories("2016-1", "hours", "all");

            assertThat(resp.drilldown().yearHalf()).isEqualTo("2016-1");
            assertThat(resp.drilldown().systemNo()).isNull();
            assertThat(resp.drilldown().categoryCode()).isNull();
        }

        @Test
        void returns3MonthsFor2015SecondHalf() {
            // FY2015 H2 = 過渡期: 10月〜12月（3ヶ月のみ）
            when(fiscalYearResolver.getMonthList(2015, FiscalYearResolver.HalfPeriod.SECOND))
                    .thenReturn(List.of("2015-10", "2015-11", "2015-12"));
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    rec("201510", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("8")),
                    rec("201511", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("4")),
                    rec("201512", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("2"))
            ));

            HalfTrendsResponse resp = halfTrendsService.getCategories("2015-2", "hours", "all");

            assertThat(resp.rows()).hasSize(1);
            HalfTrendsResponse.HalfTrendsRow row = resp.rows().get(0);
            assertThat(row.months()).hasSize(3);
            assertThat(row.total()).isEqualByComparingTo("14");
        }

        @Test
        void returnsEmptyRowsWhenNoData() {
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of());

            HalfTrendsResponse resp = halfTrendsService.getCategories("2016-1", "hours", "all");

            assertThat(resp.rows()).isEmpty();
        }
    }

    // =========================================================================
    // STEP_1: getSystems
    // =========================================================================

    @Nested
    class GetSystems {

        @Test
        void filtersByCategoryAndGroupsBySystem() {
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    rec("201601", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("10")),
                    rec("201602", "SYS02", "SUB02", "CAT-A", "SKB1", new BigDecimal("20")),
                    rec("201601", "SYS03", "SUB03", "CAT-B", "SKB1", new BigDecimal("99")) // 別カテゴリ
            ));

            HalfTrendsResponse resp = halfTrendsService.getSystems("2016-1", "CAT-A", "hours", "all");

            assertThat(resp.rows()).hasSize(2);
            assertThat(resp.rows()).extracting(HalfTrendsResponse.HalfTrendsRow::key)
                    .containsExactlyInAnyOrder("SYS01", "SYS02");

            HalfTrendsResponse.HalfTrendsRow sys1 = resp.rows().stream()
                    .filter(r -> "SYS01".equals(r.key())).findFirst().orElseThrow();
            assertThat(sys1.total()).isEqualByComparingTo("10");

            HalfTrendsResponse.HalfTrendsRow sys2 = resp.rows().stream()
                    .filter(r -> "SYS02".equals(r.key())).findFirst().orElseThrow();
            assertThat(sys2.total()).isEqualByComparingTo("20");
        }

        @Test
        void drilldownContextContainsCategoryCode() {
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of());

            HalfTrendsResponse resp = halfTrendsService.getSystems("2016-1", "CAT-A", "hours", "all");

            assertThat(resp.drilldown().yearHalf()).isEqualTo("2016-1");
            assertThat(resp.drilldown().categoryCode()).isEqualTo("CAT-A");
            assertThat(resp.drilldown().systemNo()).isNull();
        }

        @Test
        void aggregatesMultipleSubsystemsPerSystem() {
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    rec("201601", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("10")),
                    rec("201601", "SYS01", "SUB02", "CAT-A", "SKB1", new BigDecimal("15")) // 同システム別サブシステム
            ));

            HalfTrendsResponse resp = halfTrendsService.getSystems("2016-1", "CAT-A", "hours", "all");

            assertThat(resp.rows()).hasSize(1);
            assertThat(resp.rows().get(0).key()).isEqualTo("SYS01");
            assertThat(resp.rows().get(0).total()).isEqualByComparingTo("25");
        }
    }

    // =========================================================================
    // STEP_2: getSubsystems
    // =========================================================================

    @Nested
    class GetSubsystems {

        @Test
        void filtersByCategoryAndSystemAndGroupsBySubsystem() {
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    rec("201601", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("10")),
                    rec("201602", "SYS01", "SUB02", "CAT-A", "SKB1", new BigDecimal("30")),
                    rec("201601", "SYS02", "SUB01", "CAT-A", "SKB1", new BigDecimal("99")), // 別システム
                    rec("201601", "SYS01", "SUB01", "CAT-B", "SKB1", new BigDecimal("99"))  // 別カテゴリ
            ));

            HalfTrendsResponse resp = halfTrendsService.getSubsystems("2016-1", "SYS01", "CAT-A", "hours", "all");

            assertThat(resp.rows()).hasSize(2);
            assertThat(resp.rows()).extracting(HalfTrendsResponse.HalfTrendsRow::key)
                    .containsExactlyInAnyOrder("SUB01", "SUB02");

            HalfTrendsResponse.HalfTrendsRow sub1 = resp.rows().stream()
                    .filter(r -> "SUB01".equals(r.key())).findFirst().orElseThrow();
            assertThat(sub1.total()).isEqualByComparingTo("10");

            HalfTrendsResponse.HalfTrendsRow sub2 = resp.rows().stream()
                    .filter(r -> "SUB02".equals(r.key())).findFirst().orElseThrow();
            assertThat(sub2.total()).isEqualByComparingTo("30");
        }

        @Test
        void drilldownContextContainsSystemAndCategory() {
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of());

            HalfTrendsResponse resp = halfTrendsService.getSubsystems("2016-1", "SYS01", "CAT-A", "hours", "all");

            assertThat(resp.drilldown().yearHalf()).isEqualTo("2016-1");
            assertThat(resp.drilldown().systemNo()).isEqualTo("SYS01");
            assertThat(resp.drilldown().categoryCode()).isEqualTo("CAT-A");
        }

        @Test
        void monthlyValuesAreCorrectlyDistributed() {
            when(fiscalYearResolver.getMonthList(2016, FiscalYearResolver.HalfPeriod.FIRST))
                    .thenReturn(List.of("2016-01", "2016-02", "2016-03",
                                        "2016-04", "2016-05", "2016-06"));
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    rec("201601", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("3")),
                    rec("201603", "SYS01", "SUB01", "CAT-A", "SKB1", new BigDecimal("7"))
            ));

            HalfTrendsResponse resp = halfTrendsService.getSubsystems("2016-1", "SYS01", "CAT-A", "hours", "all");

            HalfTrendsResponse.HalfTrendsRow row = resp.rows().get(0);
            assertThat(row.months().get(0).value()).isEqualByComparingTo("3");  // 01月
            assertThat(row.months().get(1).value()).isEqualByComparingTo("0");  // 02月 (データなし)
            assertThat(row.months().get(2).value()).isEqualByComparingTo("7");  // 03月
            assertThat(row.total()).isEqualByComparingTo("10");
        }
    }
}
