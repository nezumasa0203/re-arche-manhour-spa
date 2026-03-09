package com.example.czConsv.service;

import com.example.czConsv.dao.Tcz01HosyuKousuuDao;
import com.example.czConsv.dao.Tcz13SubsysSumDao;
import com.example.czConsv.dto.response.MonthlyBreakdownResponse;
import com.example.czConsv.entity.Tcz01HosyuKousuu;
import com.example.czConsv.entity.Tcz13SubsysSum;
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

@ExtendWith(MockitoExtension.class)
class MonthlyBreakdownServiceTest {

    @Mock Tcz13SubsysSumDao subsysSumDao;
    @Mock Tcz01HosyuKousuuDao workHoursDao;
    @InjectMocks MonthlyBreakdownService monthlyBreakdownService;

    private Tcz13SubsysSum sum(String yyyymm, String sknno, String subsknno,
                                String kategoriId, BigDecimal hours) {
        Tcz13SubsysSum r = new Tcz13SubsysSum();
        r.yyyymm = yyyymm; r.sknno = sknno; r.subsknno = subsknno;
        r.hsKategoriId = kategoriId; r.hsKousuu = hours; r.delflg = "0";
        return r;
    }

    private Tcz13SubsysSum sumDeleted(String yyyymm, String sknno, String subsknno,
                                       String kategoriId, BigDecimal hours) {
        Tcz13SubsysSum r = sum(yyyymm, sknno, subsknno, kategoriId, hours);
        r.delflg = "1"; return r;
    }

    private Tcz01HosyuKousuu wh(String yearHalf, String sknno, String subsknno,
                                  String kategori, BigDecimal hours) {
        Tcz01HosyuKousuu r = new Tcz01HosyuKousuu();
        r.yearHalf = yearHalf; r.taisyoSknno = sknno; r.taisyoSubsysno = subsknno;
        r.hsKategori = kategori; r.kousuu = hours; r.delflg = "0";
        return r;
    }

    @Nested
    class GetCategories {
        @Test
        void aggregatesByCategoryForGivenMonth() {
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    sum("202502", "SYS01", "SUB01", "CAT-A", new BigDecimal("10")),
                    sum("202502", "SYS02", "SUB02", "CAT-A", new BigDecimal("20")),
                    sum("202502", "SYS01", "SUB01", "CAT-B", new BigDecimal("5")),
                    sum("202501", "SYS01", "SUB01", "CAT-A", new BigDecimal("99"))
            ));
            MonthlyBreakdownResponse resp =
                    monthlyBreakdownService.getCategories("2025-02", "hours", "all");
            assertThat(resp.rows()).hasSize(2);
            MonthlyBreakdownResponse.BreakdownRow catA = resp.rows().stream()
                    .filter(r -> "CAT-A".equals(r.key())).findFirst().orElseThrow();
            assertThat(catA.total()).isEqualByComparingTo("30");
            assertThat(catA.months()).hasSize(1);
            assertThat(catA.months().get(0).yearMonth()).isEqualTo("2025-02");
            assertThat(catA.months().get(0).value()).isEqualByComparingTo("30");
        }

        @Test
        void excludesDeletedRecords() {
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    sum("202502",        "SYS01", "SUB01", "CAT-A", new BigDecimal("10")),
                    sumDeleted("202502", "SYS01", "SUB01", "CAT-A", new BigDecimal("99"))
            ));
            MonthlyBreakdownResponse resp =
                    monthlyBreakdownService.getCategories("2025-02", "hours", "all");
            assertThat(resp.rows()).hasSize(1);
            assertThat(resp.rows().get(0).total()).isEqualByComparingTo("10");
        }

        @Test
        void contextContainsYearMonth() {
            when(subsysSumDao.selectAll()).thenReturn(List.of());
            MonthlyBreakdownResponse resp =
                    monthlyBreakdownService.getCategories("2025-02", "hours", "all");
            assertThat(resp.context().yearMonth()).isEqualTo("2025-02");
            assertThat(resp.context().systemNo()).isNull();
            assertThat(resp.context().categoryCode()).isNull();
        }

        @Test
        void returnsEmptyWhenNoData() {
            when(subsysSumDao.selectAll()).thenReturn(List.of());
            MonthlyBreakdownResponse resp =
                    monthlyBreakdownService.getCategories("2025-02", "hours", "all");
            assertThat(resp.rows()).isEmpty();
        }
    }

    @Nested
    class GetSystems {
        @Test
        void filtersByCategoryAndGroupsBySystem() {
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    sum("202502", "SYS01", "SUB01", "CAT-A", new BigDecimal("10")),
                    sum("202502", "SYS02", "SUB02", "CAT-A", new BigDecimal("20")),
                    sum("202502", "SYS01", "SUB01", "CAT-B", new BigDecimal("99"))
            ));
            MonthlyBreakdownResponse resp =
                    monthlyBreakdownService.getSystems("2025-02", "CAT-A", "hours", "all");
            assertThat(resp.rows()).hasSize(2);
            assertThat(resp.rows()).extracting(MonthlyBreakdownResponse.BreakdownRow::key)
                    .containsExactlyInAnyOrder("SYS01", "SYS02");
            assertThat(resp.context().categoryCode()).isEqualTo("CAT-A");
        }

        @Test
        void aggregatesMultipleSubsystemsPerSystem() {
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    sum("202502", "SYS01", "SUB01", "CAT-A", new BigDecimal("10")),
                    sum("202502", "SYS01", "SUB02", "CAT-A", new BigDecimal("15"))
            ));
            MonthlyBreakdownResponse resp =
                    monthlyBreakdownService.getSystems("2025-02", "CAT-A", "hours", "all");
            assertThat(resp.rows()).hasSize(1);
            assertThat(resp.rows().get(0).total()).isEqualByComparingTo("25");
        }
    }

    @Nested
    class GetSubsystems {
        @Test
        void filtersByCategoryAndSystemAndGroupsBySubsystem() {
            when(subsysSumDao.selectAll()).thenReturn(List.of(
                    sum("202502", "SYS01", "SUB01", "CAT-A", new BigDecimal("10")),
                    sum("202502", "SYS01", "SUB02", "CAT-A", new BigDecimal("30")),
                    sum("202502", "SYS02", "SUB03", "CAT-A", new BigDecimal("99")),
                    sum("202502", "SYS01", "SUB01", "CAT-B", new BigDecimal("99"))
            ));
            MonthlyBreakdownResponse resp =
                    monthlyBreakdownService.getSubsystems("2025-02", "SYS01", "CAT-A", "hours", "all");
            assertThat(resp.rows()).hasSize(2);
            assertThat(resp.rows()).extracting(MonthlyBreakdownResponse.BreakdownRow::key)
                    .containsExactlyInAnyOrder("SUB01", "SUB02");
            assertThat(resp.context().systemNo()).isEqualTo("SYS01");
            assertThat(resp.context().categoryCode()).isEqualTo("CAT-A");
        }
    }

    @Nested
    class GetDetail {
        @Test
        void returnsWorkHoursAggregatedForContext() {
            when(workHoursDao.selectAll()).thenReturn(List.of(
                    wh("202502", "SYS01", "SUB01", "CAT-A", new BigDecimal("8")),
                    wh("202502", "SYS01", "SUB01", "CAT-A", new BigDecimal("4")),
                    wh("202502", "SYS02", "SUB01", "CAT-A", new BigDecimal("99")),
                    wh("202502", "SYS01", "SUB02", "CAT-A", new BigDecimal("99")),
                    wh("202502", "SYS01", "SUB01", "CAT-B", new BigDecimal("99")),
                    wh("202501", "SYS01", "SUB01", "CAT-A", new BigDecimal("99"))
            ));
            MonthlyBreakdownResponse resp =
                    monthlyBreakdownService.getDetail("2025-02", "SYS01", "SUB01", "CAT-A", "hours", "all");
            assertThat(resp.rows()).hasSize(1);
            assertThat(resp.rows().get(0).total()).isEqualByComparingTo("12");
            assertThat(resp.context().yearMonth()).isEqualTo("2025-02");
            assertThat(resp.context().systemNo()).isEqualTo("SYS01");
            assertThat(resp.context().subsystemNo()).isEqualTo("SUB01");
            assertThat(resp.context().categoryCode()).isEqualTo("CAT-A");
        }

        @Test
        void excludesDeletedWorkHours() {
            Tcz01HosyuKousuu deleted = wh("202502", "SYS01", "SUB01", "CAT-A", new BigDecimal("99"));
            deleted.delflg = "1";
            when(workHoursDao.selectAll()).thenReturn(List.of(
                    wh("202502", "SYS01", "SUB01", "CAT-A", new BigDecimal("5")),
                    deleted
            ));
            MonthlyBreakdownResponse resp =
                    monthlyBreakdownService.getDetail("2025-02", "SYS01", "SUB01", "CAT-A", "hours", "all");
            assertThat(resp.rows()).hasSize(1);
            assertThat(resp.rows().get(0).total()).isEqualByComparingTo("5");
        }
    }
}
