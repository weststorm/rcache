package org.wstorm.rcache.jedis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月08日
 */
public class JedisWrapperTest extends JedisTestBase {

    protected JedisWrapper wrapper;

    protected String key = "key_jedisWrapperTest";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        wrapper = new JedisWrapper(pool);

    }

    @After
    public void tearDown() throws Exception {
        wrapper.execute(jedis -> jedis.del(key));
        super.tearDown();
    }

    @Test
    public void execute() throws Exception {
        assertThat((Long) wrapper.execute(jedis -> jedis.rpush(key, "test"))).isEqualTo(1);
        assertThat((String) wrapper.execute(jedis -> jedis.lpop(key))).isEqualTo("test");
    }

    @Test
    public void serialKey() throws Exception {
        byte[] bytes = wrapper.serialKey(key);
        assertThat(wrapper.deserialKey(bytes)).isEqualTo(key);
    }

}