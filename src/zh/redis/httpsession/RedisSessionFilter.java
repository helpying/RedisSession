package zh.redis.httpsession;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RedisSessionFilter implements Filter {
	public static final String[] IGNORE_SUFFIX = { ".png", ".jpg", ".jpeg",".gif", ".css", ".js", ".html", ".htm", "swf"};
	private RedisSessionManager sessionManager;

	public void init(FilterConfig filterConfig) throws ServletException {
		int port = Integer.parseInt(filterConfig.getInitParameter("port"));
		String host = filterConfig.getInitParameter("host");
		this.sessionManager = new RedisSessionManager(host, port);
	}

	public void setSessionManager(RedisSessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;

		if (!(shouldFilter(request))) {
			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		RequestEventSubject eventSubject = new RequestEventSubject();
		SessionHttpServletRequestWrapper requestWrapper = new SessionHttpServletRequestWrapper(
				request, response, this.sessionManager, eventSubject);
		try {
			filterChain.doFilter(requestWrapper, servletResponse);
		} finally {
			eventSubject.completed(request, response);
		}
	}

    private boolean shouldFilter(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        for (String suffix : IGNORE_SUFFIX) {
            if (uri.endsWith(suffix)) return false;
        }
        return true;
    }

	public void destroy() {
	}
}