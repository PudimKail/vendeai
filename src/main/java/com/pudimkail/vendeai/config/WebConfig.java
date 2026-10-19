package com.pudimkail.vendeai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String pastaUploads;

    public WebConfig(@Value("${vendeai.uploads}") String pastaUploads) {
        this.pastaUploads = Path.of(pastaUploads).toAbsolutePath().toUri().toString();
    }

    /** Fotos dos anúncios: /uploads/arquivo.jpg */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(pastaUploads);
    }

    /**
     * O React cuida das rotas (/produto/1, /carrinho...). Se alguém abrir esses endereços direto,
     * o servidor devolve o index.html e o React mostra a página certa.
     */
    @Controller
    static class RotasDoReact {
        private static final String ROTA = "{rota:^(?!api|uploads|assets)[^\\.]*}";

        @GetMapping({"/" + ROTA, "/" + ROTA + "/{a:[^\\.]*}", "/" + ROTA + "/{a:[^\\.]*}/{b:[^\\.]*}"})
        String index() {
            return "forward:/index.html";
        }
    }
}
