package org.wstorm.rcache;

import org.wstorm.rcache.annotation.CacheConfig;

/**
 * 测试对象缓存
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月14日
 */
@CacheConfig(region = "org.wstorm.rcache", keyPrefix = "testObj", expiredTime = 10)
public class TestObj implements RObject<String> {

    private String id;

    private int sum;

    public TestObj(String id, int sum) {
        this.id = id;
        this.sum = sum;
    }

    public TestObj() {
        // null object constructor
        setId(null);
        setSum(-999);
    }

    public String getId() {
        return id();
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestObj)) return false;

        TestObj sObject = (TestObj) o;

        return sum == sObject.sum && (getId() != null ? id.equals(sObject.id) : sObject.id == null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + sum;
        return result;
    }
}
