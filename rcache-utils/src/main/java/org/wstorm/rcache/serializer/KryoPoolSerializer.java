package org.wstorm.rcache.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

import java.io.Closeable;
import java.io.IOException;

/**
 * Kryo序列化器
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年5月07日
 */
public class KryoPoolSerializer implements Serializer {

    /**
     * Serialize object
     *
     * @param obj object need to serialize to byte[]
     * @return return serialize data
     * @throws Exception exception
     */
    @Override
    public byte[] serialize(Object obj) throws Exception {

        if (obj == null) throw new Exception("obj can not be null");

        try (KryoWrapper kryoWrapper = (KryoWrapper) Holder.kryoPool.borrow()) {

            kryoWrapper.output.clear();
            kryoWrapper.writeClassAndObject(kryoWrapper.output, obj);
            return kryoWrapper.output.toBytes();
        } catch (Exception e) {
            throw new Exception("Serialize obj exception", e);
        }
    }

    /**
     * Deserialize data
     *
     * @param bytes need to deserialize to object
     * @return object object return
     * @throws Exception exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes) throws Exception {

        if (bytes == null) throw new Exception("bytes can not be null");

        try (KryoWrapper kryoWrapper = (KryoWrapper) Holder.kryoPool.borrow()) {

            kryoWrapper.input.setBuffer(bytes, 0, bytes.length);
            return (T) kryoWrapper.readClassAndObject(kryoWrapper.input);
        } catch (Exception e) {
            throw new Exception("Deserialize bytes exception", e);
        }
    }

    /**
     * 保证单例和延迟加载
     */
    private static class Holder {
        private static KryoPool kryoPool = new KryoPool.Builder(new WSKryoFactory()).softReferences().build();
    }

    private static class WSKryoFactory implements KryoFactory {

        @Override
        public KryoWrapper create() {
            return new KryoWrapper(Holder.kryoPool);
        }
    }

    private static class KryoWrapper extends Kryo implements Closeable {
        private static final int BUFFER_SIZE = 1024;
        private KryoPool pool;
        private Output output = new Output(BUFFER_SIZE, -1);
        private Input input = new Input();

        KryoWrapper(KryoPool pool) {
            this.pool = pool;
        }

        @Override
        public void close() throws IOException {
            this.pool.release(this);
        }
    }
}
