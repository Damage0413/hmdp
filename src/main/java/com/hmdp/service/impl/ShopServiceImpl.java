package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Shop queryById(Long id) {
        String cachedshop = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
        if (!(cachedshop == null) && !cachedshop.isEmpty()) {
            Shop shop = JSONUtil.toBean(cachedshop, Shop.class);
            return shop;
        }
        if (!(cachedshop == null) && cachedshop.isEmpty()) {
            return null;
        }
        // 互斥锁解决缓存击穿
        // 获取互斥锁
        boolean tryLock = tryLock("lock:shop:" + id);
        Shop shop = null;
        if (tryLock) {
            shop = getById(id);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (shop != null) {
                stringRedisTemplate.opsForValue().set("cache:shop:" + id, JSONUtil.toJsonStr(shop), 30,
                        TimeUnit.MINUTES);
            } else {
                stringRedisTemplate.opsForValue().set("cache:shop:" + id, "", 2, TimeUnit.MINUTES);
            }
            unLock("lock:shop:" + id);

        } else {
            try {
                Thread.sleep(50);
                return queryById(id);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return shop;
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("店铺id不能为空");
        }
        // 先更新
        updateById(shop);
        // 再删缓存
        stringRedisTemplate.delete("cache:shop:" + shop.getId());
        return Result.ok();
    }

    private boolean tryLock(String key) {
        Boolean setIfAbsent = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(setIfAbsent);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
