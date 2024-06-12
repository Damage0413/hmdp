package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        if (phoneInvalid) {
            return Result.fail("手机号输入错误");
        }
        // 生成验证码
        String randomCode = RandomUtil.randomNumbers(6);
        // 保存验证码
        stringRedisTemplate.opsForValue().set("login:code:" + phone, randomCode, 2, TimeUnit.MINUTES);
        // 发送验证码
        log.debug(randomCode);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String password = loginForm.getPassword();
        String code = loginForm.getCode();
        if (password != null) {
            // 密码登录
        } else {
            // 验证码登录
            boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
            if (phoneInvalid) {
                return Result.fail("手机号输入错误");
            }
            String cachedCode = stringRedisTemplate.opsForValue().get("login:code:" + phone);
            if (cachedCode == null || !cachedCode.equals(code)) {
                return Result.fail("验证码错误");
            }
        }
        // 查询用户
        User user = query().eq("phone", phone).one();
        if (user == null) {
            // 不存在用户
            user = createUser(phone);
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);

        String token = UUID.randomUUID().toString(true);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", userDTO.getId().toString());
        userMap.put("nickName", userDTO.getNickName());
        userMap.put("icon", userDTO.getIcon());

        stringRedisTemplate.opsForHash().putAll("login:token:" + token, userMap);
        // 设置有效期
        stringRedisTemplate.expire("login:token:" + token, 30, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User createUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("User_" + RandomUtil.randomString(5));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        save(user);
        return user;
    }
}
