package com.miproject.finalwork.common.interceptor;

import com.miproject.finalwork.common.annotation.RateLimiter;
import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.exception.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流拦截器
 */
@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);
            
            if (rateLimiter != null) {
                String key = rateLimiter.key();
                if (key.isEmpty()) {
                    key = request.getRequestURI();
                }
                
                String clientId = request.getRemoteAddr();
                String redisKey = "rate_limit:" + key + ":" + clientId;
                
                String countStr = stringRedisTemplate.opsForValue().get(redisKey);
                int count = countStr == null ? 0 : Integer.parseInt(countStr);
                
                if (count >= rateLimiter.maxRequests()) {
                    // 超过限流阈值
                    throw new ClientException(BaseErrorCode.FLOW_LIMIT_ERROR);
                }
                
                // 增加计数
                stringRedisTemplate.opsForValue().increment(redisKey, 1);
                stringRedisTemplate.expire(redisKey, rateLimiter.timeWindow(), rateLimiter.timeUnit());
            }
        }
        return true;
    }
}