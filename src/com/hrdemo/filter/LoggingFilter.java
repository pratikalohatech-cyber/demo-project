package com.hrdemo.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Demonstrates a simple cross-cutting Filter.
 *
 * Filters run BEFORE the request reaches any Servlet, and are chained in the
 * order they're declared in web.xml. Each filter must call chain.doFilter()
 * to pass control to the next filter (or the target servlet) - forgetting
 * this call silently breaks every request that hits this filter.
 */
public class LoggingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // Called once when the container loads this filter - good place for one-time setup.
        System.out.println("[LoggingFilter] initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        long start = System.currentTimeMillis();

        System.out.printf("[LoggingFilter] --> %s %s%n",
                httpRequest.getMethod(), httpRequest.getRequestURI());

        // Pass control to the next filter in the chain, or the target servlet
        // if this is the last filter. Everything after this line runs on the
        // way BACK out, after the servlet has produced a response.
        chain.doFilter(request, response);

        long duration = System.currentTimeMillis() - start;
        System.out.printf("[LoggingFilter] <-- %s %s (%dms)%n",
                httpRequest.getMethod(), httpRequest.getRequestURI(), duration);
    }

    @Override
    public void destroy() {
        System.out.println("[LoggingFilter] destroyed");
    }
}
