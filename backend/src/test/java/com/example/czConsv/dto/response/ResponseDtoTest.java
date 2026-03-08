package com.example.czConsv.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * レスポンス DTO 単体テスト。
 *
 * <p>全 9 レスポンス DTO のレコード生成と JSON シリアライズを検証する。
 */
@DisplayName("レスポンス DTO")
class ResponseDtoTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ================================================================
    // 1. ErrorResponse
    // ================================================================
    @Nested
    @DisplayName("ErrorResponse")
    class ErrorResponseTest {

        @Test
        @DisplayName("全フィールド指定で生成")
        void fullConstructor() {
            var err = new ErrorResponse(
                    "CZ-101", "楽観ロックエラー", "updatedAt",
                    List.of("param1"), 100L);
            assertEquals("CZ-101", err.code());
            assertEquals("楽観ロックエラー", err.message());
            assertEquals("updatedAt", err.field());
            assertEquals(List.of("param1"), err.params());
            assertEquals(100L, err.recordId());
        }

        @Test
        @DisplayName("簡易コンストラクタ（code + message）")
        void simpleConstructor() {
            var err = new ErrorResponse("CZ-300", "システムエラー");
            assertEquals("CZ-300", err.code());
            assertEquals("システムエラー", err.message());
            assertNull(err.field());
            assertNull(err.params());
            assertNull(err.recordId());
        }

        @Test
        @DisplayName("フィールド指定簡易コンストラクタ")
        void fieldConstructor() {
            var err = new ErrorResponse("CZ-200", "入力エラー", "yearMonth");
            assertEquals("yearMonth", err.field());
            assertNull(err.params());
            assertNull(err.recordId());
        }

        @Test
        @DisplayName("JSON シリアライズ")
        void serialize() throws JsonProcessingException {
            var err = new ErrorResponse("CZ-101", "エラー");
            String json = objectMapper.writeValueAsString(err);
            assertTrue(json.contains("\"code\""));
            assertTrue(json.contains("CZ-101"));
            assertTrue(json.contains("\"message\""));
        }
    }

    // ================================================================
    // 2. WorkHoursListResponse
    // ================================================================
    @Nested
    @DisplayName("WorkHoursListResponse")
    class WorkHoursListResponseTest {

        @Test
        @DisplayName("ネストレコード含む完全なレスポンス生成")
        void fullResponse() {
            var subsystem = new WorkHoursListResponse.SubsystemInfo(
                    "SUB001", "サブシステムA", "SYS001", "システムA");
            var category = new WorkHoursListResponse.CategoryInfo("CAT01", "開発");
            var now = LocalDateTime.of(2025, 4, 15, 10, 30, 0);
            var record = new WorkHoursListResponse.WorkHoursRecord(
                    1L, "2025-04", "2025-04-15", subsystem, subsystem,
                    category, "テスト作業", "08:30", "TMR-001",
                    "WR-001", "山田太郎", "0", now);
            var summary = new WorkHoursListResponse.WorkHoursSummary(
                    new BigDecimal("160.50"), new BigDecimal("8.50"));
            var perms = new WorkHoursListResponse.WorkHoursPermissions(
                    true, true, true, true, false, true, false);
            var monthCtrl = new WorkHoursListResponse.MonthControl(
                    "2025-04", "OPEN", false);

            var resp = new WorkHoursListResponse(
                    List.of(record), summary, perms, monthCtrl);

            assertEquals(1, resp.records().size());
            assertEquals(1L, resp.records().get(0).id());
            assertEquals("2025-04", resp.records().get(0).yearMonth());
            assertEquals(new BigDecimal("160.50"), resp.summary().monthlyTotal());
            assertTrue(resp.permissions().canCreate());
            assertFalse(resp.monthControl().isLocked());
        }

        @Test
        @DisplayName("SubsystemInfo のアクセサ")
        void subsystemInfo() {
            var info = new WorkHoursListResponse.SubsystemInfo(
                    "SUB001", "名前", "SYS001", "システム名");
            assertEquals("SUB001", info.subsystemNo());
            assertEquals("名前", info.subsystemName());
            assertEquals("SYS001", info.systemNo());
            assertEquals("システム名", info.systemName());
        }

        @Test
        @DisplayName("CategoryInfo のアクセサ")
        void categoryInfo() {
            var info = new WorkHoursListResponse.CategoryInfo("CAT01", "開発");
            assertEquals("CAT01", info.categoryCode());
            assertEquals("開発", info.categoryName());
        }

        @Test
        @DisplayName("空リストのレスポンス")
        void emptyRecords() {
            var summary = new WorkHoursListResponse.WorkHoursSummary(
                    BigDecimal.ZERO, BigDecimal.ZERO);
            var perms = new WorkHoursListResponse.WorkHoursPermissions(
                    false, false, false, false, false, false, false);
            var monthCtrl = new WorkHoursListResponse.MonthControl(
                    "2025-04", "LOCKED", true);

            var resp = new WorkHoursListResponse(
                    Collections.emptyList(), summary, perms, monthCtrl);
            assertTrue(resp.records().isEmpty());
            assertTrue(resp.monthControl().isLocked());
        }

        @Test
        @DisplayName("JSON シリアライズ")
        void serialize() throws JsonProcessingException {
            var summary = new WorkHoursListResponse.WorkHoursSummary(
                    BigDecimal.TEN, BigDecimal.ONE);
            var perms = new WorkHoursListResponse.WorkHoursPermissions(
                    true, false, false, false, false, false, false);
            var monthCtrl = new WorkHoursListResponse.MonthControl(
                    "2025-04", "OPEN", false);
            var resp = new WorkHoursListResponse(
                    Collections.emptyList(), summary, perms, monthCtrl);

            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"records\""));
            assertTrue(json.contains("\"summary\""));
            assertTrue(json.contains("\"permissions\""));
            assertTrue(json.contains("\"monthControl\""));
        }
    }

    // ================================================================
    // 3. WorkHoursUpdateResponse
    // ================================================================
    @Nested
    @DisplayName("WorkHoursUpdateResponse")
    class WorkHoursUpdateResponseTest {

        @Test
        @DisplayName("レスポンス生成")
        void create() {
            var summary = new WorkHoursListResponse.WorkHoursSummary(
                    new BigDecimal("160.00"), new BigDecimal("8.00"));
            var resp = new WorkHoursUpdateResponse(
                    1L, "subject", "旧件名", "新件名", summary);
            assertEquals(1L, resp.id());
            assertEquals("subject", resp.field());
            assertEquals("旧件名", resp.oldValue());
            assertEquals("新件名", resp.newValue());
            assertEquals(new BigDecimal("160.00"), resp.summary().monthlyTotal());
        }

        @Test
        @DisplayName("JSON シリアライズ")
        void serialize() throws JsonProcessingException {
            var summary = new WorkHoursListResponse.WorkHoursSummary(
                    BigDecimal.ZERO, BigDecimal.ZERO);
            var resp = new WorkHoursUpdateResponse(
                    1L, "hours", "08:00", "09:00", summary);
            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"id\""));
            assertTrue(json.contains("\"field\""));
            assertTrue(json.contains("\"oldValue\""));
            assertTrue(json.contains("\"newValue\""));
            assertTrue(json.contains("\"summary\""));
        }
    }

    // ================================================================
    // 4. WorkStatusListResponse
    // ================================================================
    @Nested
    @DisplayName("WorkStatusListResponse")
    class WorkStatusListResponseTest {

        @Test
        @DisplayName("完全なレスポンス生成")
        void fullResponse() {
            var record = new WorkStatusListResponse.WorkStatusRecord(
                    "STAFF001", "山田太郎", "ORG001", "開発部",
                    "2025-04", "1", new BigDecimal("160.00"), 20);
            var monthly = new WorkStatusListResponse.MonthlyControlInfo(
                    "2025-04", "ORG001", "CONFIRMED", true, false);
            var perms = new WorkStatusListResponse.WorkStatusPermissions(
                    true, true, true, false, false);

            var resp = new WorkStatusListResponse(
                    List.of(record), monthly, perms);
            assertEquals(1, resp.records().size());
            assertEquals("STAFF001", resp.records().get(0).staffId());
            assertEquals("山田太郎", resp.records().get(0).staffName());
            assertEquals(20, resp.records().get(0).recordCount());
            assertTrue(resp.monthlyControl().isConfirmed());
            assertTrue(resp.permissions().canApprove());
        }

        @Test
        @DisplayName("JSON シリアライズ")
        void serialize() throws JsonProcessingException {
            var record = new WorkStatusListResponse.WorkStatusRecord(
                    "S1", "名前", "O1", "部署",
                    "2025-04", "0", BigDecimal.ZERO, 0);
            var monthly = new WorkStatusListResponse.MonthlyControlInfo(
                    "2025-04", "O1", "OPEN", false, false);
            var perms = new WorkStatusListResponse.WorkStatusPermissions(
                    false, false, false, false, false);
            var resp = new WorkStatusListResponse(
                    List.of(record), monthly, perms);

            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"records\""));
            assertTrue(json.contains("\"monthlyControl\""));
            assertTrue(json.contains("\"permissions\""));
        }
    }

    // ================================================================
    // 5. HalfTrendsResponse
    // ================================================================
    @Nested
    @DisplayName("HalfTrendsResponse")
    class HalfTrendsResponseTest {

        @Test
        @DisplayName("完全なレスポンス生成")
        void fullResponse() {
            var mv1 = new HalfTrendsResponse.MonthValue("2025-01", new BigDecimal("10.5"));
            var mv2 = new HalfTrendsResponse.MonthValue("2025-02", new BigDecimal("20.0"));
            var row = new HalfTrendsResponse.HalfTrendsRow(
                    "SUB001", "サブシステムA", List.of(mv1, mv2),
                    new BigDecimal("30.5"));
            var drilldown = new HalfTrendsResponse.DrilldownContext(
                    "2025H1", "SYS001", null, null);

            var resp = new HalfTrendsResponse(List.of(row), drilldown);
            assertEquals(1, resp.rows().size());
            assertEquals("SUB001", resp.rows().get(0).key());
            assertEquals(2, resp.rows().get(0).months().size());
            assertEquals(new BigDecimal("30.5"), resp.rows().get(0).total());
            assertEquals("2025H1", resp.drilldown().yearHalf());
        }

        @Test
        @DisplayName("MonthValue のアクセサ")
        void monthValue() {
            var mv = new HalfTrendsResponse.MonthValue("2025-03", BigDecimal.TEN);
            assertEquals("2025-03", mv.yearMonth());
            assertEquals(BigDecimal.TEN, mv.value());
        }

        @Test
        @DisplayName("DrilldownContext で全フィールド null 可")
        void drilldownNullable() {
            var ctx = new HalfTrendsResponse.DrilldownContext(
                    "2025H1", null, null, null);
            assertNull(ctx.systemNo());
            assertNull(ctx.subsystemNo());
            assertNull(ctx.categoryCode());
        }

        @Test
        @DisplayName("JSON シリアライズ")
        void serialize() throws JsonProcessingException {
            var row = new HalfTrendsResponse.HalfTrendsRow(
                    "K1", "ラベル", Collections.emptyList(), BigDecimal.ZERO);
            var ctx = new HalfTrendsResponse.DrilldownContext(
                    "2025H1", null, null, null);
            var resp = new HalfTrendsResponse(List.of(row), ctx);

            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"rows\""));
            assertTrue(json.contains("\"drilldown\""));
        }
    }

    // ================================================================
    // 6. MonthlyBreakdownResponse
    // ================================================================
    @Nested
    @DisplayName("MonthlyBreakdownResponse")
    class MonthlyBreakdownResponseTest {

        @Test
        @DisplayName("完全なレスポンス生成")
        void fullResponse() {
            var mv = new MonthlyBreakdownResponse.MonthValue(
                    "2025-04", new BigDecimal("15.0"));
            var row = new MonthlyBreakdownResponse.BreakdownRow(
                    "CAT01", "開発", List.of(mv), new BigDecimal("15.0"));
            var ctx = new MonthlyBreakdownResponse.BreakdownContext(
                    "2025-04", "SYS001", "SUB001", "CAT01");

            var resp = new MonthlyBreakdownResponse(List.of(row), ctx);
            assertEquals(1, resp.rows().size());
            assertEquals("CAT01", resp.rows().get(0).key());
            assertEquals("2025-04", resp.context().yearMonth());
        }

        @Test
        @DisplayName("JSON シリアライズ")
        void serialize() throws JsonProcessingException {
            var row = new MonthlyBreakdownResponse.BreakdownRow(
                    "K1", "ラベル", Collections.emptyList(), BigDecimal.ZERO);
            var ctx = new MonthlyBreakdownResponse.BreakdownContext(
                    "2025-04", null, null, null);
            var resp = new MonthlyBreakdownResponse(List.of(row), ctx);

            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"rows\""));
            assertTrue(json.contains("\"context\""));
        }
    }

    // ================================================================
    // 7. MySystemListResponse
    // ================================================================
    @Nested
    @DisplayName("MySystemListResponse")
    class MySystemListResponseTest {

        @Test
        @DisplayName("完全なレスポンス生成")
        void fullResponse() {
            var item1 = new MySystemListResponse.MySystemItem("SYS001", "システムA", 3);
            var item2 = new MySystemListResponse.MySystemItem("SYS002", "システムB", 5);

            var resp = new MySystemListResponse(List.of(item1, item2));
            assertEquals(2, resp.systems().size());
            assertEquals("SYS001", resp.systems().get(0).systemNo());
            assertEquals("システムA", resp.systems().get(0).systemName());
            assertEquals(3, resp.systems().get(0).subsystemCount());
        }

        @Test
        @DisplayName("空リスト")
        void emptyList() {
            var resp = new MySystemListResponse(Collections.emptyList());
            assertTrue(resp.systems().isEmpty());
        }

        @Test
        @DisplayName("JSON シリアライズ")
        void serialize() throws JsonProcessingException {
            var item = new MySystemListResponse.MySystemItem("SYS001", "名前", 1);
            var resp = new MySystemListResponse(List.of(item));

            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"systems\""));
            assertTrue(json.contains("\"systemNo\""));
            assertTrue(json.contains("\"systemName\""));
            assertTrue(json.contains("\"subsystemCount\""));
        }
    }

    // ================================================================
    // 8. MasterListResponse
    // ================================================================
    @Nested
    @DisplayName("MasterListResponse")
    class MasterListResponseTest {

        @Test
        @DisplayName("レスポンス生成")
        void create() {
            var resp = new MasterListResponse(
                    List.of("item1", "item2"), 100, 1, 20);
            assertEquals(2, resp.items().size());
            assertEquals(100, resp.totalCount());
            assertEquals(1, resp.page());
            assertEquals(20, resp.pageSize());
        }

        @Test
        @DisplayName("空ページ")
        void emptyPage() {
            var resp = new MasterListResponse(
                    Collections.emptyList(), 0, 1, 20);
            assertTrue(resp.items().isEmpty());
            assertEquals(0, resp.totalCount());
        }

        @Test
        @DisplayName("JSON シリアライズ")
        void serialize() throws JsonProcessingException {
            var resp = new MasterListResponse(
                    List.of("a", "b"), 50, 2, 25);
            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"items\""));
            assertTrue(json.contains("\"totalCount\""));
            assertTrue(json.contains("\"page\""));
            assertTrue(json.contains("\"pageSize\""));
        }
    }

    // ================================================================
    // 9. DelegationResponse
    // ================================================================
    @Nested
    @DisplayName("DelegationResponse")
    class DelegationResponseTest {

        @Test
        @DisplayName("代行モード有効")
        void delegationActive() {
            var resp = new DelegationResponse("STAFF002", "佐藤花子", true);
            assertEquals("STAFF002", resp.delegationStaffId());
            assertEquals("佐藤花子", resp.delegationStaffName());
            assertTrue(resp.isDaiko());
        }

        @Test
        @DisplayName("代行解除ファクトリメソッド")
        void cancelled() {
            var resp = DelegationResponse.cancelled();
            assertNull(resp.delegationStaffId());
            assertNull(resp.delegationStaffName());
            assertFalse(resp.isDaiko());
        }

        @Test
        @DisplayName("JSON シリアライズ")
        void serialize() throws JsonProcessingException {
            var resp = new DelegationResponse("S1", "名前", true);
            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"delegationStaffId\""));
            assertTrue(json.contains("\"delegationStaffName\""));
            // Jackson serializes boolean isDaiko as "daiko" by default (is-prefix removed)
            // but record components keep their exact name
            assertTrue(json.contains("\"isDaiko\"") || json.contains("\"daiko\""));
        }

        @Test
        @DisplayName("代行解除の JSON シリアライズで null フィールド")
        void serializeCancelled() throws JsonProcessingException {
            var resp = DelegationResponse.cancelled();
            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"delegationStaffId\""));
            assertFalse(json.contains("\"isDaiko\":true"));
        }
    }

    // ================================================================
    // レコード等値性テスト
    // ================================================================
    @Nested
    @DisplayName("レコード等値性")
    class RecordEqualityTest {

        @Test
        @DisplayName("同値の ErrorResponse は等しい")
        void errorResponseEquality() {
            var e1 = new ErrorResponse("CZ-101", "msg");
            var e2 = new ErrorResponse("CZ-101", "msg");
            assertEquals(e1, e2);
            assertEquals(e1.hashCode(), e2.hashCode());
        }

        @Test
        @DisplayName("異なる DelegationResponse は等しくない")
        void delegationInequality() {
            var d1 = new DelegationResponse("S1", "名前A", true);
            var d2 = new DelegationResponse("S2", "名前B", true);
            assertNotEquals(d1, d2);
        }

        @Test
        @DisplayName("MasterListResponse の toString")
        void masterListToString() {
            var resp = new MasterListResponse(List.of("x"), 1, 1, 10);
            String str = resp.toString();
            assertTrue(str.contains("totalCount=1"));
        }
    }
}
