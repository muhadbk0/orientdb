package com.orientechnologies.orient.client.remote.message;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.orientechnologies.orient.client.binary.OChannelBinaryAsynchClient;
import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelBinary;

public class OErrorResponse implements OBinaryResponse<Void> {
  private Map<String, String> messages;
  private byte[]              result;

  public OErrorResponse() {
  }

  public OErrorResponse(Map<String, String> messages, byte[] result) {
    this.messages = messages;
    this.result = result;
  }

  @Override
  public Void read(OChannelBinaryAsynchClient network, OStorageRemoteSession session) throws IOException {
    messages = new HashMap<>();
    while (network.readByte() == 1) {
      String key = network.readString();
      String value = network.readString();
      messages.put(key, value);
    }
    result = network.readBytes();
    return null;
  }

  @Override
  public void write(OChannelBinary channel, int protocolVersion, String recordSerializer) throws IOException {
    for (Entry<String, String> entry : messages.entrySet()) {
      // MORE DETAILS ARE COMING AS EXCEPTION
      channel.writeByte((byte) 1);

      channel.writeString(entry.getKey());
      channel.writeString(entry.getValue());
    }
    channel.writeByte((byte) 0);

    channel.writeBytes(result);
  }

  public Map<String, String> getMessages() {
    return messages;
  }

  public byte[] getResult() {
    return result;
  }

}