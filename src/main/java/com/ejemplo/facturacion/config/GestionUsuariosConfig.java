package com.ejemplo.facturacion.config;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ejemplo.facturacion.security.JwtFilter;

// Clase de configuración para la gestión de usuarios y seguridad en la aplicación
@Configuration
public class GestionUsuariosConfig {
    // Bean para codificar contraseñas utilizando BCrypt con un factor de trabajo de 4 y un generador de números aleatorios seguro
    @Bean
    public PasswordEncoder passwordEncoder() throws NoSuchAlgorithmException {
        SecureRandom s = SecureRandom.getInstanceStrong();
        return new BCryptPasswordEncoder(4, s);
    }

    // Bean para configurar la cadena de filtros de seguridad, deshabilitando CSRF, estableciendo la política de creación de sesiones a stateless, permitiendo el acceso sin autenticación a ciertos endpoints y agregando un filtro JWT personalizado
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter)
            throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/autenticar", "/actuator/health").permitAll()
                .anyRequest().authenticated());

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Bean para exponer el AuthenticationManager, que es necesario para la autenticación de usuarios en la aplicación
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
