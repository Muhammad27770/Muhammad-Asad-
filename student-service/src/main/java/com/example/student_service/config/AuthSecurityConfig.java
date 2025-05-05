// package com.example.student_service.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.CorsConfigurationSource;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
// import org.springframework.web.bind.annotation.RequestMethod;
// import org.springframework.http.HttpMethod;

// import java.util.Arrays;

// @Configuration
// @EnableWebSecurity
// public class AuthSecurityConfig {

//     @Bean
//     public PasswordEncoder passwordHasher() {
//         return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
//     }

//     @Bean
//     public SecurityFilterChain securityChain(HttpSecurity http) throws Exception {
//         http
//             .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//             .csrf(csrf -> csrf.disable())
//             .authorizeHttpRequests(auth -> auth
//             .requestMatchers(HttpMethod.POST, "/student/register", "/student/login").permitAll()
//             .requestMatchers("/student/courses/**").permitAll()
//             .requestMatchers(HttpMethod.POST, "/courses/enroll").permitAll()

//                 .anyRequest().authenticated()
//             )
//             .formLogin(form -> form.disable())
//             .httpBasic(httpBasic -> httpBasic.disable())
//             .logout(logout -> logout.disable());
            
//         return http.build();
//     }

//     @Bean
//     public CorsConfigurationSource corsConfigurationSource() {
//         CorsConfiguration configuration = new CorsConfiguration();
//         configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*"));

//         configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//         configuration.setAllowedHeaders(Arrays.asList("*"));
//         configuration.setAllowCredentials(true); // Add this line
//         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//         source.registerCorsConfiguration("/**", configuration);
//         return source;
//     }
// }


package com.example.student_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class AuthSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity (enable in production)
            .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/student/courses/enroll").permitAll()
            .requestMatchers(
                "/",
                "/**",
                "/index.html",
                "/static/**",
                "/script.js",
                "/styles.css",
                "/favicon.ico",
                "/student/**",
                "/api/**",
                "/courses/**",
                "/enroll/**",
                "/auth/**"
            ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable()) // Disable Spring Security's default login page
            .logout(logout -> logout.disable()); // Disable default logout behavior
        return http.build();
    }
}