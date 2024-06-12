package com.hmdp.interceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;

import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RefreshTokenInterceptor implements HandlerInterceptor {
    /*
     * 该拦截器不拦截请求，只刷新token有效期
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String token = request.getHeader("authorization");
        if (token == null) {
            return true;
        }
        log.info("jwt校验:{}", token);
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries("login:token:" + token);
        if (userMap.isEmpty()) {
            return true;
        }
        UserDTO userDto = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        UserHolder.saveUser(userDto);
        stringRedisTemplate.expire("login:token:" + token, 30, TimeUnit.MINUTES);
        return true;

    }
}
