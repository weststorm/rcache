package org.wstorm.rcache.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.enums.CacheRegion;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * 工具类
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年5月06日
 */
public class CacheUtils {
    /**
     * 默认注解
     */
    private static CacheConfig defaultCacheAnno = new CacheConfig() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return CacheConfig.class;
        }

        @Override
        public CacheRegion region() {
            return CacheRegion.unknown;
        }

        @Override
        public String keyPrefix() {
            return null;
        }

        @Override
        public int expiredTime() {
            return 0;
        }
    };

    /**
     * 取缓存定义类
     *
     * @param cacheObjectClass 缓存对象类
     * @return 缓存配置注解
     */
    public static <T> CacheConfig getCacheAnnotation(final Class<T> cacheObjectClass) {
        if (cacheObjectClass != null) {
            return cacheObjectClass.getAnnotation(CacheConfig.class);
        }
        return defaultCacheAnno;
    }

    /**
     * 取缓存类定义的缓存Region
     *
     * @param cacheObjectClass 缓存类
     * @return 缓存区域
     */
    public static <T> String getCacheRegion(final Class<T> cacheObjectClass) {
        CacheConfig sndCache = getCacheAnnotation(cacheObjectClass);
        if (sndCache != null) {
            return sndCache.region().region;
        }
        return null;// 如果没有取外层
    }

    /**
     * 取缓存类定义的缓存失效时间
     *
     * @param cacheObjectClass 缓存类
     * @return 过期时间
     */
    public static <T> int getCacheExpiredTime(final Class<T> cacheObjectClass) {
        CacheConfig sndCache = getCacheAnnotation(cacheObjectClass);
        if (sndCache != null) {
            return sndCache.expiredTime();
        }
        return 0;// 默认为0,表示不失效
    }

    /**
     * 取缓存类定义的缓存Key前缀
     *
     * @param cacheObjectClass 缓存类
     * @return 缓存KEY前缀
     */
    public static <T> String getCacheKeyPrefix(final Class<T> cacheObjectClass) {
        CacheConfig sndCache = getCacheAnnotation(cacheObjectClass);
        if (sndCache != null) {
            return sndCache.keyPrefix();
        }
        return null;
    }

    /**
     * 将keys与缓存前缀拼接并返回一个全新的拼接成功的字符串
     *
     * @param cacheConfig 缓存配置
     * @param key         未修饰过的键
     * @return 缓存KEY
     */
    public static <ID> String genCacheKey(final CacheConfig cacheConfig, final ID key) {
        String keyStr = key.toString();
        if (cacheConfig == null) {
            return keyStr;
        }
        if (StringUtils.isNotBlank(cacheConfig.region().region)) {
            if (keyStr.startsWith(cacheConfig.region().region)) {
                return keyStr;
            } else {
                if (StringUtils.isNotBlank(cacheConfig.keyPrefix())) {
                    return concat(cacheConfig.region().region, cacheConfig.keyPrefix(), keyStr);
                } else {
                    return keyStr;
                }
            }
        } else {
            if (StringUtils.isNotBlank(cacheConfig.keyPrefix())) {
                if (keyStr.startsWith(cacheConfig.keyPrefix())) {
                    return keyStr;
                }
                return concat(cacheConfig.keyPrefix(), keyStr);
            } else {
                return keyStr;
            }
        }
    }

    /**
     * 通过缓存 配置项和keys 修饰成 缓存Keys
     *
     * @param cacheConfig 缓存配置项
     * @param keys        未修饰过的键
     * @return 生成缓存Key列表
     */
    public static <ID> List<String> genCacheKeys(final CacheConfig cacheConfig, final List<ID> keys) {
        List<String> fixKeys = Lists.newArrayListWithCapacity(keys.size());
        if (cacheConfig == null) {
            for (ID key : keys) {
                fixKeys.add(key.toString());
            }
            return fixKeys;
        }
        if (StringUtils.isBlank(cacheConfig.region().region)) {
            if (StringUtils.isNotBlank(cacheConfig.keyPrefix())) {
                for (ID key : keys) {
                    if (key.toString().startsWith(cacheConfig.keyPrefix())) {
                        fixKeys.add(key.toString());
                    } else {
                        fixKeys.add(concat(cacheConfig.keyPrefix(), key.toString()));
                    }
                }
            } else {
                for (ID key : keys) fixKeys.add(key.toString());
            }
        } else {
            if (StringUtils.isNotBlank(cacheConfig.keyPrefix())) {
                for (ID key : keys) {
                    if (key.toString().startsWith(cacheConfig.region().region)) {
                        fixKeys.add(key.toString());
                    } else {
                        fixKeys.add(concat(cacheConfig.region().region, cacheConfig.keyPrefix(), key.toString()));
                    }
                }
            } else {
                for (ID key : keys) {
                    if (key.toString().startsWith(cacheConfig.region().region)) {
                        fixKeys.add(key.toString());
                    } else {
                        fixKeys.add(concat(cacheConfig.region().region, key.toString()));
                    }
                }
            }
        }
        return fixKeys;
    }

    /**
     * 字符串拼接，中间用:隔开
     *
     * @param strs 字串数组
     * @return 生成缓存key规范的key, 如a:b:c:d
     */
    public static String concat(final String... strs) {
        Preconditions.checkNotNull(strs);
        return StringUtils.join(strs, ":");
    }

    /**
     * 通过CacheKey来取取真实的id, 如 cacheConfig中的region为user， 而keyprefix为id， cacheKey为
     * user:id:123， 将返回123
     *
     * @param cacheConfig 缓存KEY的前缀等相关配置，不能为空！
     * @param cacheKey    如 user:id:232848238432
     * @return 缓存id
     */
    public static String getIdByCacheKey(CacheConfig cacheConfig, String cacheKey) {
        if (cacheConfig == null) {
            return cacheKey;
        }
        String id = cacheKey;
        if (null != cacheConfig.region().region) {
            if (id.startsWith(cacheConfig.region().region)) {
                id = id.substring(cacheConfig.region().region.length() + 1);
            }
            if (id.startsWith(cacheConfig.keyPrefix())) {
                id = id.substring(cacheConfig.keyPrefix().length() + 1);
            }
        }
        return id;
    }

    /**
     * 按ids顺序返回bulk中包含该 ids的 值序列
     *
     * @param ids  id序列
     * @param bulk 数据map
     * @return 包含该 ids的值并按ids顺序排序的序列，如果其中有id是不存在的，则跳过
     */
    public static <ID, T> List<T> getBulkValueByIds(List<ID> ids, Map<String, T> bulk) {
        List<T> values = Lists.newArrayListWithCapacity(ids.size());
        for (ID id : ids) {
            T e = bulk.get(id.toString());
            if (e != null)
                values.add(e);
        }
        return values;
    }
}
