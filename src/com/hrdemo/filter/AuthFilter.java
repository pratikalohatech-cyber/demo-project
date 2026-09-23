package com.hrdemo.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Demonstrates auth enforcement at the Filter layer, and short-circuiting
 * the chain when a request shouldn't reach the servlet at all.
 *
 * In a real system, this would validate a session or an SSO/Keycloak token
 * rather than a hardcoded demo string - the pattern (check first, reject
 * early, never let unauthenticated requests reach business logic) is the
 * same either way.
 */
public class AuthFilter implements Filter {

    private String expectedToken;

    @Override
    public void init(FilterConfig filterConfig) {
        // init-param values from web.xml are available here at startup
        this.expectedToken = filterConfig.getInitParameter("demoToken");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Flow explanation:
        // 1) Read the custom auth header sent by the client.
        // 2) Compare it to the expected demo token configured in web.xml.
        // 3) If it does not match, stop here and return 401.
        // 4) Otherwise, allow the request to continue to the servlet.
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String token = httpRequest.getHeader("X-Demo-Auth-Token");
        boolean isAuthorized = token != null && token.equals(expectedToken);

        if (!isAuthorized) {
            // Short-circuit: do NOT call chain.doFilter() - the servlet never sees this request.
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Missing or invalid X-Demo-Auth-Token header\"}");
            System.out.println("[AuthFilter] BLOCKED unauthenticated request to " + httpRequest.getRequestURI());
            return;
        }

        // Authenticated - let the request continue to the next filter/servlet.
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
