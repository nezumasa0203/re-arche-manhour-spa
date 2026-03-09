package com.example.czConsv.util;

import com.example.czConsv.security.CzSecurityContext;
import com.example.czConsv.security.model.CzPrincipal;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * コントローラー共通ユーティリティ。
 *
 * <p>複数コントローラーで重複していた Excel ヘッダー構築・skbtcd 解決を集約する。
 */
public final class ControllerSupport {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private ControllerSupport() {}

    /**
     * Excel ダウンロード用レスポンスヘッダーを構築する。
     */
    public static HttpHeaders excelHeaders(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(EXCEL_MEDIA_TYPE);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        return headers;
    }

    /**
     * CzSecurityContext から skbtcd を解決する（jinjiMode → "01", 通常 → "00"）。
     */
    public static String resolveSkbtcd() {
        CzPrincipal principal = CzSecurityContext.require();
        return principal.permissions().jinjiMode() ? "01" : "00";
    }
}
