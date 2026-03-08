package com.example.czConsv.security;

import com.example.czConsv.security.model.CzPermissions;
import com.example.czConsv.security.model.CzPrincipal;
import com.example.czConsv.security.model.DataAuthority;
import com.example.czConsv.security.model.EmploymentType;
import com.example.czConsv.security.model.TabPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CzSecurityContext の単体テスト。
 * ThreadLocal ベースの認証コンテキストの基本動作およびスレッド分離を検証する。
 */
@DisplayName("CzSecurityContext: ThreadLocal 認証コンテキスト")
class CzSecurityContextTest {

    @AfterEach
    void tearDown() {
        CzSecurityContext.clear();
    }

    // ========================================================================
    // ヘルパー
    // ========================================================================

    /**
     * テスト用の CzPrincipal を生成する。
     */
    private CzPrincipal createPrincipal(String userId, String userName) {
        CzPermissions permissions = new CzPermissions(
                false,
                TabPermission.EMPTY,
                TabPermission.EMPTY,
                TabPermission.EMPTY,
                new DataAuthority(null, null, null),
                EmploymentType.OFFICIAL,
                null,
                false
        );
        return new CzPrincipal(userId, userName, userId + "@example.com",
                "ORG001", "テスト組織", permissions);
    }

    // ========================================================================
    // テストケース
    // ========================================================================

    @Test
    @DisplayName("set() と get() でプリンシパルの設定・取得ができる")
    void setAndGetReturnsSamePrincipal() {
        CzPrincipal principal = createPrincipal("U001", "テスト太郎");

        CzSecurityContext.set(principal);
        CzPrincipal actual = CzSecurityContext.get();

        assertSame(principal, actual, "set() で設定したインスタンスと同一オブジェクトが返ること");
        assertEquals("U001", actual.userId());
        assertEquals("テスト太郎", actual.userName());
    }

    @Test
    @DisplayName("require() — セット済みの場合、プリンシパルが返る")
    void requireReturnsPrincipalWhenSet() {
        CzPrincipal principal = createPrincipal("U002", "山田花子");

        CzSecurityContext.set(principal);
        CzPrincipal actual = CzSecurityContext.require();

        assertSame(principal, actual, "require() は set() したインスタンスと同一オブジェクトを返すこと");
        assertEquals("U002", actual.userId());
    }

    @Test
    @DisplayName("require() — 未セットの場合、IllegalStateException がスローされる")
    void requireThrowsWhenNotSet() {
        // CzSecurityContext は tearDown で clear 済みなので未セット状態

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                CzSecurityContext::require,
                "未セット状態で require() を呼ぶと IllegalStateException がスローされること"
        );

        assertTrue(ex.getMessage().contains("not set"),
                "例外メッセージに 'not set' が含まれること: " + ex.getMessage());
    }

    @Test
    @DisplayName("clear() でプリンシパルが除去される")
    void clearRemovesPrincipal() {
        CzPrincipal principal = createPrincipal("U003", "鈴木一郎");
        CzSecurityContext.set(principal);

        // clear 前は取得できる
        assertNotNull(CzSecurityContext.get(), "clear 前は get() が非 null を返すこと");

        CzSecurityContext.clear();

        assertNull(CzSecurityContext.get(), "clear 後は get() が null を返すこと");
        assertThrows(IllegalStateException.class, CzSecurityContext::require,
                "clear 後は require() が IllegalStateException をスローすること");
    }

    @Test
    @DisplayName("スレッド分離 — 異なるスレッドは独立したプリンシパルを保持する")
    void threadIsolation() throws InterruptedException {
        CzPrincipal mainPrincipal = createPrincipal("MAIN", "メインスレッド");
        CzSecurityContext.set(mainPrincipal);

        AtomicReference<CzPrincipal> childSeen = new AtomicReference<>();
        AtomicReference<CzPrincipal> childOwn = new AtomicReference<>();
        CountDownLatch childReady = new CountDownLatch(1);
        CountDownLatch mainChecked = new CountDownLatch(1);

        Thread child = new Thread(() -> {
            try {
                // 子スレッドからはメインスレッドのプリンシパルは見えない
                childSeen.set(CzSecurityContext.get());

                // 子スレッド独自のプリンシパルを設定
                CzPrincipal childPrincipal = createPrincipal("CHILD", "子スレッド");
                CzSecurityContext.set(childPrincipal);
                childOwn.set(CzSecurityContext.get());

                childReady.countDown();
                mainChecked.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                CzSecurityContext.clear();
            }
        });

        child.start();
        childReady.await();

        // 子スレッドはメインスレッドのプリンシパルを見ていないこと
        assertNull(childSeen.get(),
                "子スレッドから親スレッドのプリンシパルが見えないこと（ThreadLocal 分離）");

        // 子スレッドが独自にセットしたプリンシパルは子スレッドのもの
        assertNotNull(childOwn.get(), "子スレッドは独自のプリンシパルを保持できること");
        assertEquals("CHILD", childOwn.get().userId());

        // メインスレッドのプリンシパルは影響を受けていない
        CzPrincipal mainActual = CzSecurityContext.get();
        assertSame(mainPrincipal, mainActual,
                "子スレッドの操作がメインスレッドのプリンシパルに影響しないこと");
        assertEquals("MAIN", mainActual.userId());

        mainChecked.countDown();
        child.join(5000);
    }

    @Test
    @DisplayName("get() — 未セット状態では null が返る")
    void getReturnsNullWhenNotSet() {
        assertNull(CzSecurityContext.get(),
                "初期状態で get() は null を返すこと");
    }

    @Test
    @DisplayName("set() の上書き — 後から設定したプリンシパルが有効になる")
    void setOverwritesPreviousPrincipal() {
        CzPrincipal first = createPrincipal("FIRST", "最初のユーザー");
        CzPrincipal second = createPrincipal("SECOND", "二番目のユーザー");

        CzSecurityContext.set(first);
        assertEquals("FIRST", CzSecurityContext.get().userId());

        CzSecurityContext.set(second);
        assertEquals("SECOND", CzSecurityContext.get().userId(),
                "後から set() したプリンシパルで上書きされること");
    }
}
