package ch.neeman.guy.secretmessage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Long> lastRequest = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        Long last = lastRequest.get(ip);

        if (last != null && (now - last) < 1000) {
            response.setStatus(429);
            response.getWriter().write("Too many requests");
            return false;
        }

        lastRequest.put(ip, now);

        return true;
    }
}