package org.wstorm.rcache.exception;

/**
 * 缓存的异常
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月06日
 */
public class CacheException extends RuntimeException {

    public CacheException(String s) {
        super(s);
    }

    public CacheException(String s, Throwable e) {
        super(s, e);
    }

    public CacheException(Throwable e) {
        super(e);
    }

}