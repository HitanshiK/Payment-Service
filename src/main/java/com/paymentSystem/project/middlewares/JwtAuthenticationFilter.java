package com.paymentSystem.project.middlewares;

import com.paymentSystem.project.entity.User;
import com.paymentSystem.project.enums.Status;
import com.paymentSystem.project.repos.UserRepository;
import com.paymentSystem.project.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                String jti = jwtUtil.extractJti(token);
                if (redisTemplate.hasKey("blacklist:" + jti)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been logged out");
                    return;
                }

                Long userId = jwtUtil.extractUserId(token);
                User user = userRepository.findById(userId).orElse(null);


                if (user != null) {
                    if (!user.getStatus().equals(Status.INACTIVE)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "User account is inactive");
                        return;
                    }

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    user.getId(),
                                    null,
                                    List.of()
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid token");
            }
        }
        filterChain.doFilter(request, response);
    }
}



