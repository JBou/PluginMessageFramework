package com.ikeirnez.pluginmessageframework.gateway;

import com.ikeirnez.pluginmessageframework.connection.ProxySide;
import com.ikeirnez.pluginmessageframework.packet.Packet;

import java.io.IOException;

/**
 * Represents a 2-way connection on a Proxy-like server (e.g. BungeeCord).
 *
 * @param <T> class representing the client side (e.g. BungeeCord ProxiedPlayer)
 * @param <U> class representing the server side (e.g. BungeeCord ServerInfo)
 */
public interface ProxyGateway<T, U> extends Gateway<T> {

    /**
     * Gets the side this proxy gateway is running on.
     *
     * @return the side this proxy gateway is running on
     */
    ProxySide getProxySide();

    /**
     * Sends a {@link Packet} to a server with queueing enabled.
     *
     * @param serverConnection the server connection
     * @param packet the packet to send to the server
     * @return true if the packet was sent immediately, false if the packet was unable to be sent immediately (and was queued if queueing is enabled)
     * @throws IOException thrown if there was an issue sending the packet (will likely be in the serialization stage)
     */
    boolean sendPacketServer(U serverConnection, Packet packet) throws IOException;

    /**
     * Sends a {@link Packet} to a specified server.
     *
     * @param serverConnection the server connection
     * @param packet the packet to send to the server
     * @param queue if enabled, if there is no connection to the server, the packet will be queued until a connection is available
     * @return true if the packet was sent immediately, false if the packet was unable to be sent immediately (and was queued if queueing is enabled)
     * @throws IOException thrown if there was an issue sending the packet (will likely be in the serialization stage)
     */
    boolean sendPacketServer(U serverConnection, Packet packet, boolean queue) throws IOException;

}
