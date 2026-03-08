package com.example.czConsv.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.format.DateTimeFormatter;

/**
 * Web MVC configuration.
 *
 * <p>Configures Jackson serialization for date/time types.
 * CORS is handled by SecurityConfig, not here.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Jackson customizer for LocalDateTime and LocalDate serialization format.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.simpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            builder.serializers(new LocalDateTimeSerializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
            builder.serializers(new LocalDateSerializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        };
    }
}
