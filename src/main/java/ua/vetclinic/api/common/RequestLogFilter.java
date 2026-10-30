package ua.vetclinic.api.common;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class RequestLogFilter extends OncePerRequestFilter {

	private static final Logger HTTP = LoggerFactory.getLogger("HTTP");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long startedAt = System.nanoTime();
		try {
			chain.doFilter(request, response);
		}
		finally {
			long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
			HTTP.info("method={} path={} status={} durationMs={} requestId={}", request.getMethod(),
					request.getRequestURI(), response.getStatus(), durationMs, MDC.get("requestId"));
		}
	}
}
