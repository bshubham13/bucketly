package com.bucketly.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to explicitly resolve the deferred Spring Security CSRF token.
 * In Spring Security 6/7, CSRF tokens are deferred by default and only written to the
 * response cookie when the token attribute is explicitly resolved.
 * Touching csrfToken.getToken() forces the CookieCsrfTokenRepository to populate the
 * XSRF-TOKEN cookie on safe requests.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Invoking getToken() forces the DeferredCsrfToken to generate and commit the cookie
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}

