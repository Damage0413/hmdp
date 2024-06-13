package com.hmdp.service.impl;

import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;

import cn.hutool.json.JSONUtil;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;
import java.util.stream.Collectors;

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
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryTypeList() {
        List<String> cachedshopType = stringRedisTemplate.opsForList().range("cache:shopType", 0, -1);
        if (cachedshopType != null && !cachedshopType.isEmpty()) {
            List<ShopType> shoptypes = cachedshopType.stream().map(x -> {
                return JSONUtil.toBean(x, ShopType.class);
            }).collect(Collectors.toList());
            return shoptypes;
        }
        List<ShopType> shopType = query().orderByAsc("sort").list();
        if (shopType != null && !shopType.isEmpty()) {
            shopType.stream().forEach(x -> {
                String jsonStr = JSONUtil.toJsonStr(x);
                stringRedisTemplate.opsForList().rightPush("cache:shopType", jsonStr);
            });
        }
        return shopType;
    }

}
