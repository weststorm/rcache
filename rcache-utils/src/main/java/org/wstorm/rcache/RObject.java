package org.wstorm.rcache;

/**
 * 对象接口,表示这个对象是不是一个空的,没有任何数据初始化的对象
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月08日
 */
public interface RObject<ID> extends Cloneable {

    /**
     * 取id字段值
     *
     * @return id字段值
     */
    default ID id() {
        return null;
    }

    /**
     * 是否为空对象
     *
     * @return true为空
     */
    boolean isBlank();
}
