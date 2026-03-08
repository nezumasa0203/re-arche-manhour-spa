package com.example.czConsv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Clock Bean 定義。
 *
 * <p>本番環境ではシステムデフォルトタイムゾーンの {@link Clock} を提供する。
 * テスト時は {@link Clock#fixed} を使用して日付を固定し、決定論的なテストを実現する。
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
