package com.omjadon.contractanalyzer.account;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(AppUserRepository users) {
        return email -> users.findByEmailIgnoreCase(email)
                .map(user -> User.withUsername(user.email())
                        .password(user.passwordHash())
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Account not found"
                ));
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) throws Exception {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        http.authenticationProvider(provider);

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/favicon.svg",
                        "/icons.svg",
                        "/api/auth/csrf"
                ).permitAll()
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/register"
                ).permitAll()
                .anyRequest().authenticated()
        );

        http.csrf(csrf -> csrf.spa());
        http.httpBasic(AbstractHttpConfigurer::disable);

        http.formLogin(form -> form
                .loginPage("/")
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"status\":\"AUTHENTICATED\"}"
                    );
                })
                .failureHandler((request, response, error) ->
                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED
                        )
                )
                .permitAll()
        );

        http.logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(
                                HttpServletResponse.SC_NO_CONTENT
                        )
                )
        );

        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, error) ->
                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED
                        )
                )
        );

        return http.build();
    }
}