package com.ikeirnez.pluginmessageframework.impl;

import com.ikeirnez.pluginmessageframework.PrimaryArgumentProvider;
import com.ikeirnez.pluginmessageframework.Utilities;
import com.ikeirnez.pluginmessageframework.gateway.Gateway;
import com.ikeirnez.pluginmessageframework.gateway.payload.PayloadHandler;
import com.ikeirnez.pluginmessageframework.gateway.payload.StandardPayloadHandler;
import com.ikeirnez.pluginmessageframework.packet.Packet;
import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.ikeirnez.pluginmessageframework.packet.PrimaryValuePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Support class for {@link Gateway} implementations.
 * Provides connections and forwards received packets to the framework.
 *
 * @param <C> the client connection type
 */
public abstract class GatewaySupport<C> implements Gateway<C> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final String channel;
    private PayloadHandler payloadHandler = null;

    private final Map<Class<? extends Packet>, List<Object>> listeners = new HashMap<>();

    public GatewaySupport(String channel) {
        if (channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("Channel cannot be null or an empty string.");
        }

        this.channel = channel;
    }

    @Override
    public final String getChannel() {
        return channel;
    }

    @Override
    public final PayloadHandler getPayloadHandler() {
        if (payloadHandler == null) {
            payloadHandler = new StandardPayloadHandler();
        }

        return payloadHandler;
    }

    @Override
    public void setPayloadHandler(PayloadHandler payloadHandler) {
        this.payloadHandler = payloadHandler;
    }

    /**
     * Helper method, checks if packet is applicable (if not an exception is thrown) and then returns the packet in a byte[] form.
     *
     * @param packet the packet to write bytes for
     * @return the byte[] representation of the packet
     * @throws IOException thrown if there is an exception whilst writing the packet
     */
    @SuppressWarnings("unchecked")
    public byte[] writePacket(Packet packet) throws IOException {
        if (!getPayloadHandler().isPacketApplicable(packet)) {
            throw new IllegalArgumentException("Assigned PayloadHandler cannot handle this type of Packet.");
        }

        return getPayloadHandler().writeOutgoingPacket(packet);
    }

    @Override
    public void sendPacket(C connection, Packet packet) throws IOException {
        sendCustomPayload(connection, getChannel(), writePacket(packet));
    }

    public abstract void sendCustomPayload(C connection, String channel, byte[] bytes);

    protected Object handleListenerParameter(Class<?> clazz, Packet packet, C connection) {
        // todo do this better? gets overridden
        if (packet instanceof PrimaryArgumentProvider) {
            Object object = ((PrimaryArgumentProvider) packet).getValue();

            if (clazz.isAssignableFrom(object.getClass())) {
                return object;
            }
        }

        if (Packet.class.isAssignableFrom(clazz)) {
            return packet;
        }

        Class<?> connectionClass = connection.getClass();
        if (clazz.isAssignableFrom(connectionClass)) {
            return connectionClass.cast(connection);
        }

        return null;
    }

    public void incomingPayload(C connection, byte[] data) throws IOException {
        receivePacket(connection, getPayloadHandler().readIncomingPacket(data));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerListener(Object listener) {
        for (Method method : listener.getClass().getMethods()) {
            if (method.isAnnotationPresent(PacketHandler.class)) { // todo check parameters too
                Class<?>[] parameterTypes = method.getParameterTypes();
                Class<? extends Packet> packetClazz = PrimaryValuePacket.class;

                for (Class<?> parameterType : parameterTypes) { // find packet class
                    if (Packet.class.isAssignableFrom(parameterType)) {
                        packetClazz = (Class<? extends Packet>) parameterType;
                        break;
                    }
                }

                List<Object> list = listeners.get(packetClazz);
                if (list == null) {
                    list = new ArrayList<>();
                    listeners.put(packetClazz, list);
                }

                list.add(listener);
            }
        }
    }

    @Override
    public void unregisterListener(Object listener) {
        for (List<Object> list : listeners.values()) {
            list.remove(listener);
        }
    }

    @Override
    public void receivePacket(C connection, Packet packet) {
        Class<? extends Packet> packetClass = packet.getClass();

        if (listeners.containsKey(packetClass)) {
            for (Object listener : listeners.get(packetClass)) {
                methodLoop: for (Method method : listener.getClass().getMethods()) {
                    if (method.isAnnotationPresent(PacketHandler.class)) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        Object[] parameters = new Object[parameterTypes.length];

                        for (int i = 0; i < parameters.length; i++) {
                            Class<?> parameterType = parameterTypes[i];
                            Object parameter = handleListenerParameter(parameterType, packet, connection);

                            if (parameter != null) {
                                parameters[i] = parameter;
                            } else {
                                continue methodLoop;
                            }
                        }

                        try {
                            method.invoke(listener, parameters);
                        } catch (IllegalAccessException e) {
                            logger.error("Error occurred whilst dispatching packet to listeners.", e);
                        } catch (InvocationTargetException e) {
                            Throwable throwable = e.getCause();
                            if (throwable == null) {
                                throwable = e;
                            }

                            Utilities.sneakyThrow(throwable);
                        }
                    }
                }
            }
        }
    }

}
