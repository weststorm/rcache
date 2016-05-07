package org.wstorm.rcache.serializer;


/**
 * 对象序列化接口
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年5月07日
 */
public interface Serializer {

    byte[] serialize(Object obj) throws Exception;

    Object deserialize(byte[] bytes) throws Exception;

}
