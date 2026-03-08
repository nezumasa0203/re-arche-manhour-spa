package com.example.czConsv.service;

import com.example.czConsv.dao.Tcz19MySysDao;
import com.example.czConsv.entity.Tcz19MySys;
import com.example.czConsv.exception.CzBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MySystemService の単体テスト。
 *
 * <p>MYシステム（お気に入り）の登録・参照・削除ロジックを検証する。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MySystemService: MYシステム管理")
class MySystemServiceTest {

    @Mock
    private Tcz19MySysDao mySysDao;

    @InjectMocks
    private MySystemService service;

    // ========================================================================
    // getMySystemList テスト
    // ========================================================================

    @Nested
    @DisplayName("getMySystemList: ユーザーのMYシステム一覧取得")
    class GetMySystemListTests {

        @Test
        @DisplayName("該当ユーザーの未削除レコードのみ返す")
        void returnsFilteredList() {
            Tcz19MySys mine1 = createMySys("user01", "SYS001", "0");
            Tcz19MySys mine2 = createMySys("user01", "SYS002", "0");
            Tcz19MySys deleted = createMySys("user01", "SYS003", "1");
            Tcz19MySys otherUser = createMySys("user02", "SYS001", "0");

            when(mySysDao.selectAll())
                    .thenReturn(List.of(mine1, mine2, deleted, otherUser));

            List<Tcz19MySys> result = service.getMySystemList("user01");

            assertEquals(2, result.size());
            assertEquals("SYS001", result.get(0).sknno);
            assertEquals("SYS002", result.get(1).sknno);
        }

        @Test
        @DisplayName("該当なしの場合は空リストを返す")
        void returnsEmptyList() {
            when(mySysDao.selectAll()).thenReturn(Collections.emptyList());

            List<Tcz19MySys> result = service.getMySystemList("user99");

            assertTrue(result.isEmpty());
        }
    }

    // ========================================================================
    // registerMySystem テスト
    // ========================================================================

    @Nested
    @DisplayName("registerMySystem: MYシステム登録")
    class RegisterMySystemTests {

        @Test
        @DisplayName("新規登録が成功する")
        void registerSuccess() {
            when(mySysDao.selectById("user01", "SYS001"))
                    .thenReturn(Optional.empty());
            when(mySysDao.insert(any(Tcz19MySys.class))).thenReturn(1);

            Tcz19MySys result = service.registerMySystem("user01", "SYS001");

            assertNotNull(result);
            assertEquals("user01", result.tntEsqid);
            assertEquals("SYS001", result.sknno);
            assertEquals("0", result.delflg);
            assertEquals("user01", result.initnt);
            assertEquals("user01", result.updtnt);
            assertEquals("MySystem", result.updpgid);
            assertNotNull(result.inidate);
            assertNotNull(result.upddate);

            verify(mySysDao).insert(any(Tcz19MySys.class));
        }

        @Test
        @DisplayName("既に登録済みの場合はCZ-132をスローする")
        void registerDuplicate() {
            Tcz19MySys existing = createMySys("user01", "SYS001", "0");
            when(mySysDao.selectById("user01", "SYS001"))
                    .thenReturn(Optional.of(existing));

            CzBusinessException ex = assertThrows(
                    CzBusinessException.class,
                    () -> service.registerMySystem("user01", "SYS001"));

            assertEquals("CZ-132", ex.getCode());
            verify(mySysDao, never()).insert(any());
        }
    }

    // ========================================================================
    // removeMySystem テスト
    // ========================================================================

    @Nested
    @DisplayName("removeMySystem: MYシステム削除（論理削除）")
    class RemoveMySystemTests {

        @Test
        @DisplayName("論理削除が成功する")
        void removeSuccess() {
            Tcz19MySys existing = createMySys("user01", "SYS001", "0");
            when(mySysDao.selectById("user01", "SYS001"))
                    .thenReturn(Optional.of(existing));
            when(mySysDao.logicalDelete(any(Tcz19MySys.class))).thenReturn(1);

            assertDoesNotThrow(
                    () -> service.removeMySystem("user01", "SYS001"));

            verify(mySysDao).logicalDelete(any(Tcz19MySys.class));
        }

        @Test
        @DisplayName("対象が存在しない場合はCZ-300をスローする")
        void removeNotFound() {
            when(mySysDao.selectById("user01", "SYS999"))
                    .thenReturn(Optional.empty());

            CzBusinessException ex = assertThrows(
                    CzBusinessException.class,
                    () -> service.removeMySystem("user01", "SYS999"));

            assertEquals("CZ-300", ex.getCode());
            verify(mySysDao, never()).logicalDelete(any());
        }
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    private Tcz19MySys createMySys(String userId, String sknno,
                                   String delflg) {
        Tcz19MySys entity = new Tcz19MySys();
        entity.tntEsqid = userId;
        entity.sknno = sknno;
        entity.delflg = delflg;
        return entity;
    }
}
