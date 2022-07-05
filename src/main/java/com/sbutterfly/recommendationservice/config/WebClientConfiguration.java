package com.sbutterfly.recommendationservice.config;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.util.MimeType;

@Configuration
public class WebClientConfiguration {

    /**
     * Itunes returns response in json format but with content-type=text/javascript;charset=UTF-8
     * and default jackson decoder can't parse it. Thus, we register custom decoder.
     */
    @Bean
    public CodecCustomizer jacksonLegacyJsonCustomizer(ObjectMapper mapper) {
        return (configurer) -> {
            MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
            CodecConfigurer.CustomCodecs customCodecs = configurer.customCodecs();
            customCodecs.register(new Jackson2JsonDecoder(mapper, textJavascript));
        };
    }
}
