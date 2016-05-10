package org.wstorm.rcache.annotation;


import java.lang.annotation.*;

/**
 * 缓存配置注解
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年5月06日
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheConfig {

    /**
     * @return 设置指定缓存KEY的前缀, 为达到最优效果, 最多10个字节
     */
    String keyPrefix();

    /**
     * @return 缓存过期时间, 单位秒. 默认120秒
     */
    int expiredTime() default 120;

    /**
     * @return 缓存所属区域
     */
    String region();
}
