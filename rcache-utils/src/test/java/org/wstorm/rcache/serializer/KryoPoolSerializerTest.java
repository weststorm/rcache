package org.wstorm.rcache.serializer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月11日
 */
public class KryoPoolSerializerTest {

    private byte[] bytes = {(byte) 1, (byte) 0, (byte) 111, (byte) 114, (byte) 103, (byte) 46,
            (byte) 119, (byte) 115, (byte) 116, (byte) 111, (byte) 114, (byte) 109,
            (byte) 46, (byte) 114, (byte) 99, (byte) 97, (byte) 99, (byte) 104,
            (byte) 101, (byte) 46, (byte) 115, (byte) 101, (byte) 114, (byte) 105,
            (byte) 97, (byte) 108, (byte) 105, (byte) 122, (byte) 101, (byte) 114,
            (byte) 46, (byte) 83, (byte) 79, (byte) 98, (byte) 106, (byte) 101,
            (byte) 99, (byte) -12, (byte) 1, (byte) 1, (byte) 49, (byte) 48,
            (byte) -79, (byte) -50, (byte) 15};

    private Serializer serializer = new KryoPoolSerializer();

    @Test
    public void serialize() throws Exception {
        SObject o = new SObject();
        o.id = "101";
        o.sum = 999;
        byte[] bytes = serializer.serialize(o);

        assertThat(bytes).isEqualTo(this.bytes);

        SObject desObj = serializer.deserialize(bytes);

        assertThat(o).isEqualTo(desObj);
    }

    @Test
    public void deserialize() throws Exception {
        SObject desObj = serializer.deserialize(bytes);
        assertThat(desObj.id).isEqualTo("101");
        assertThat(desObj.sum).isEqualTo(999);
    }

}