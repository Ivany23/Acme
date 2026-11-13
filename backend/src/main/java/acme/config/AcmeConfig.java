package acme.config;

import acme.servico.CalculadoraCustos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class AcmeConfig implements WebMvcConfigurer {

    private final String[] origens;

    public AcmeConfig(@Value("${acme.cors.origens}") String origens) {
        this.origens = Arrays.stream(origens.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Bean
    public CalculadoraCustos calculadoraCustos(@Value("${acme.percentagem-organizacao}") double percentagem) {
        return new CalculadoraCustos(percentagem);
    }

    /** Permite que o frontend React (Vercel / localhost) chame a API. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origens)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
