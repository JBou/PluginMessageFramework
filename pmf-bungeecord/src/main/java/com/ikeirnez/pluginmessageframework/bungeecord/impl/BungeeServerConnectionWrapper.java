package com.ikeirnez.pluginmessageframework.bungeecord.impl;

import com.ikeirnez.pluginmessageframework.connection.QueueableConnectionWrapper;
import com.ikeirnez.pluginmessageframework.impl.BaseConnectionWrapper;
import net.md_5.bungee.api.config.ServerInfo;

/**
 * Created by Keir on 03/04/2015.
 */
public class BungeeServerConnectionWrapper extends BaseConnectionWrapper<ServerInfo> implements QueueableConnectionWrapper<ServerInfo> {

    public BungeeServerConnectionWrapper(ServerInfo gateway) {
        super(gateway);
    }

    @Override
    public void sendCustomPayload(String channel, byte[] data) {
        sendCustomPayload(channel, data, true);
    }

    @Override
    public boolean sendCustomPayload(String channel, byte[] data, boolean queue) {
        return getConnection().sendData(channel, data, queue);
    }
}
