package com.visasetu.config;

import com.visasetu.security.UserPrincipal;
import com.visasetu.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.visasetu.repository.UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (jwtTokenService.validateToken(token)) {
                try {
                    UUID userId = jwtTokenService.extractUserId(token);

                    // Validate user active state and password changed revocation if repository is available
                    if (userRepository != null) {
                        var userOpt = userRepository.findById(userId);
                        if (userOpt.isPresent()) {
                            com.visasetu.domain.entity.User user = userOpt.get();
                            if (Boolean.FALSE.equals(user.getIsActive())) {
                                log.warn("Rejected authentication: Inactive user account {}", userId);
                                filterChain.doFilter(request, response);
                                return;
                            }
                            if (user.getPasswordChangedAt() != null) {
                                java.util.Date issuedAt = jwtTokenService.extractIssuedAt(token);
                                if (issuedAt != null && issuedAt.toInstant().isBefore(user.getPasswordChangedAt())) {
                                    log.warn("Rejected authentication: Token revoked by password change for user {}", userId);
                                    filterChain.doFilter(request, response);
                                    return;
                                }
                            }
                        }
                    }

                    String email = jwtTokenService.extractEmail(token);
                    String role = jwtTokenService.extractRole(token);

                    UserPrincipal principal = new UserPrincipal(
                            userId,
                            email,
                            List.of(new SimpleGrantedAuthority(role != null ? role : "ROLE_TRAVELER"))
                    );

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    log.debug("Failed to set authentication from token: {}", e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
