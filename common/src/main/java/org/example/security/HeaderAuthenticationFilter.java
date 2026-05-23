package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads user info from headers injected by the API Gateway.
 * Does NOT verify JWT — the Gateway handles that.
 * Maps headers -> SecurityContext so @PreAuthorize works in downstream services.
 */
public class
HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HeaderAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String userId   = firstHeader(req, "X-User-Id", "userId");
        String username = firstHeader(req, "X-Username", "username");
        String roles    = firstHeader(req, "X-Roles", "userRoles");

        if (userId != null && username != null && roles != null) {
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roles));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authentication set for user '{}' with roles '{}'", username, roles);
        } else {
            log.debug("No user headers found on request to {}", req.getRequestURI());
        }

        chain.doFilter(req, res);
    }

    private String firstHeader(HttpServletRequest req, String preferredName, String fallbackName) {
        String value = req.getHeader(preferredName);
        return value != null ? value : req.getHeader(fallbackName);
    }
}
