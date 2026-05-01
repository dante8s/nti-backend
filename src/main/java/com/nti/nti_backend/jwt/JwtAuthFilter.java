package com.nti.nti_backend.jwt;

import com.nti.nti_backend.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // 🔓 1. Пропускаємо ТІЛЬКИ публічні маршрути
        // Важливо: /api/auth/admin/** та /api/auth/me мають проходити через JWT-фільтр
        if (path.startsWith("/api/public")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/verify")
                || path.equals("/api/auth/forgot-password")
                || path.equals("/api/auth/reset-password")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Беремо Authorization header
        String header = request.getHeader("Authorization");

        // Якщо токена немає — пропускаємо далі
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        String email;
        try {
            // 3. Пробуємо витягнути email
            email = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            // ❗ Прострочений або некоректний токен — просто пропускаємо
            chain.doFilter(request, response);
            return;
        }

        // 4. Якщо email є і користувач ще не авторизований
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userService.loadUserByUsername(email);

            if (jwtUtil.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                auth.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}