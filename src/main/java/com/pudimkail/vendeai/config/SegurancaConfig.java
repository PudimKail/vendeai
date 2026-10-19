package com.pudimkail.vendeai.config;

import com.pudimkail.vendeai.usuario.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * API protegida por sessão (cookie httpOnly) + proteção CSRF por cookie, que o React manda de volta
 * no cabeçalho X-XSRF-TOKEN. Vitrine, busca e produto são públicos; o resto da API exige login.
 */
@Configuration
public class SegurancaConfig {

    @Bean
    SecurityFilterChain filtro(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(acesso -> acesso
                        .requestMatchers("/api/auth/**", "/api/categorias").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()) // páginas do React e arquivos estáticos
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository()))
                // Sem login, a API responde 401 (em vez de redirecionar para uma página)
                .exceptionHandling(erro -> erro.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService usuarios, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(usuarios);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    /** O login usa o e-mail; a senha fica salva só como hash BCrypt. */
    @Bean
    UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return email -> usuarios.findByEmail(email.toLowerCase().trim())
                .map(u -> User.withUsername(u.getEmail()).password(u.getSenhaHash()).roles("USUARIO").build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
