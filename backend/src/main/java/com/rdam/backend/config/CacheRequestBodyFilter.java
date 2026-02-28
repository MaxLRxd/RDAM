package com.rdam.backend.config;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * Envuelve cada request en un ContentCachingRequestWrapper
 * para que el body pueda leerse más de una vez.
 *
 * Sin esto, el WebhookController no puede leer el raw body
 * después de que Jackson ya lo consumió para el @RequestBody.
 */
@Component
public class CacheRequestBodyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest) {
            chain.doFilter(
                new ContentCachingRequestWrapper(httpRequest),
                response
            );
        } else {
            chain.doFilter(request, response);
        }
    }
}