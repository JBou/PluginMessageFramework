package com.ikeirnez.pluginmessageframework.packet;

import java.io.*;

/**
 * Base class for creating packets.
 * Extending classes must make sure that all fields are serializable and any fields which <b>should not</b> be serialized
 * are marked as <b>transient</b>
 */
public abstract class Packet implements Serializable {

    private static final long serialVersionUID = 8235895603790260813L;

}
