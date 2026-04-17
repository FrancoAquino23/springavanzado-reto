package com.ejemplo.facturacion.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";
    private static final String HEADER_AUTORIZACION = "Authorization";

    @Autowired private JwtService jwtService;
    @Autowired private JPADetalleUsuariosService detalleUsuariosService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String encabezado = request.getHeader(HEADER_AUTORIZACION);

        if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER)) {
            String token = encabezado.substring(PREFIJO_BEARER.length());
            try {
                String nombreUsuario = jwtService.extraerNombreUsuario(token);

                if (nombreUsuario != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails detallesUsuario =
                            detalleUsuariosService.loadUserByUsername(nombreUsuario);

                    if (jwtService.validarToken(token, detallesUsuario)) {
                        UsernamePasswordAuthenticationToken autenticacion =
                                new UsernamePasswordAuthenticationToken(
                                        detallesUsuario,
                                        null,
                                        detallesUsuario.getAuthorities());
                        autenticacion.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Token inválido, expirado o malformado → continuar sin autenticar (→ 401/403)
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}

