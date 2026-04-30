package org.example.commonsecurity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter doc thong tin user tu header ma API Gateway inject vao.
 * KHONG verify JWT - Gateway da lam dieu do.
 * Chi map header -> SecurityContext de @PreAuthorize hoat dong.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

//    @Override
//    protected void doFilterInternal(HttpServletRequest req,
//                                    HttpServletResponse res,
//                                    FilterChain chain) throws ServletException, IOException {
//        String userId = req.getHeader("userId");
//        String username = req.getHeader("username");
//        String roles = req.getHeader("userRoles");
//
//        if (userId != null && username != null && roles != null) {
//            List<SimpleGrantedAuthority> authorities =
//                    List.of(new SimpleGrantedAuthority(roles));
//
//            UsernamePasswordAuthenticationToken auth =
//                    new UsernamePasswordAuthenticationToken(username, null, authorities);
//
//            // userId la UUID String - giu nguyen, khong parse Long
//            auth.setDetails(userId);
//
//            SecurityContextHolder.getContext().setAuthentication(auth);
//        }
//
//        chain.doFilter(req, res);
//    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String userId = req.getHeader("userId");
        String username = req.getHeader("username");
        String roles = req.getHeader("userRoles");

        System.out.println("====== [HeaderAuthFilter] CALLED ======");
        System.out.println("  path=" + req.getRequestURI());
        System.out.println("  method=" + req.getMethod());
        System.out.println("  userId header=" + userId);
        System.out.println("  username header=" + username);
        System.out.println("  userRoles header=" + roles);
        System.out.println("  Authorization header=" + req.getHeader("Authorization"));

        if (userId != null && username != null && roles != null) {
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(roles));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(auth);
            System.out.println("  >>> Authentication SET, principal=" + username + ", authorities=" + authorities);
        } else {
            System.out.println("  >>> Authentication NOT SET (missing one or more headers)");
        }
        System.out.println("=========================================");

        chain.doFilter(req, res);
    }
}