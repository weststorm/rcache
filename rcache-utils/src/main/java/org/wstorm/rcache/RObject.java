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
     * 取对象主键的字段值
     *
     * @return 主键值
     */
    ID id();

    /**
     * 是否为空对象
     *
     * @return true为空
     */
    default boolean isBlank() {
        return id() == null;
    }
}
