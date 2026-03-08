package com.example.czConsv.security;

import com.example.czConsv.security.model.CzPrincipal;

/**
 * ThreadLocal による CZ 認証コンテキスト。
 * AlbOidcAuthFilter がリクエストごとにセットし、
 * リクエスト完了時にクリアする。
 */
public final class CzSecurityContext {

    private static final ThreadLocal<CzPrincipal> HOLDER = new ThreadLocal<>();

    private CzSecurityContext() {
    }

    public static void set(CzPrincipal principal) {
        HOLDER.set(principal);
    }

    public static CzPrincipal get() {
        return HOLDER.get();
    }

    public static CzPrincipal require() {
        CzPrincipal principal = HOLDER.get();
        if (principal == null) {
            throw new IllegalStateException("CzPrincipal not set — authentication required");
        }
        return principal;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
