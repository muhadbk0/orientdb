package com.orientechnologies.orient.server.distributed;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.test.configs.SimpleDServerConfig;
import com.orientechnologies.orient.test.util.TestConfig;
import com.orientechnologies.orient.test.util.TestSetup;
import com.orientechnologies.orient.test.util.TestSetupUtil;
import io.kubernetes.client.openapi.ApiException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class BasicSyncIT {

  private TestSetup setup;
  private TestConfig config;
  private String server0, server1, server2;

  @Before
  public void before() throws Exception {
    config = new SimpleDServerConfig();
    setup = TestSetupUtil.create(config);
    server0 = config.getServerIds().get(0);
    server1 = config.getServerIds().get(1);
    server2 = config.getServerIds().get(2);
    setup.start();

    OrientDB remote =
        new OrientDB(
            "remote:" + setup.getAddress(server0, TestSetup.PortType.BINARY),
            "root",
            "test",
            OrientDBConfig.defaultConfig());
    remote.create("test", ODatabaseType.PLOCAL);
    remote.close();
    System.out.println("created database 'test'");
  }

  @Test
  public void sync() {
    String remoteAddress = "remote:" + setup.getAddress(server0, TestSetup.PortType.BINARY);
    try (OrientDB remote = new OrientDB(remoteAddress, OrientDBConfig.defaultConfig())) {
      try (ODatabaseSession session = remote.open("test", "admin", "admin")) {
        session.createClass("One");
        session.save(session.newElement("One"));
        session.save(session.newElement("One"));
      }
      setup.shutdownServer(server2);
      try (ODatabaseSession session = remote.open("test", "admin", "admin")) {
        session.save(session.newElement("One"));
      }
    }
    setup.shutdownServer(server0);
    setup.shutdownServer(server1);
    // Starting the servers in reverse shutdown order to trigger miss sync
    setup.startServer(server0);
    setup.startServer(server1);
    setup.startServer(server2);
    // Test server 0
    try (OrientDB remote = new OrientDB(remoteAddress, OrientDBConfig.defaultConfig())) {
      try (ODatabaseSession session = remote.open("test", "admin", "admin")) {
        assertEquals(session.countClass("One"), 3);
      }
    }
    // Test server 1
    String server1Address = "remote:" + setup.getAddress(server1, TestSetup.PortType.BINARY);
    try (OrientDB remote = new OrientDB(server1Address, OrientDBConfig.defaultConfig())) {
      try (ODatabaseSession session = remote.open("test", "admin", "admin")) {
        assertEquals(session.countClass("One"), 3);
      }
    }
    // Test server 2
    String server2Address = "remote:" + setup.getAddress(server2, TestSetup.PortType.BINARY);
    try (OrientDB remote = new OrientDB(server2Address, OrientDBConfig.defaultConfig())) {
      try (ODatabaseSession session = remote.open("test", "admin", "admin")) {
        assertEquals(session.countClass("One"), 3);
      }
    }
  }

  @Test
  @Ignore
  public void reverseStartSync() {
    try (OrientDB remote = new OrientDB("remote:localhost", OrientDBConfig.defaultConfig())) {
      try (ODatabaseSession session = remote.open("test", "admin", "admin")) {
        session.createClass("One");
        session.save(session.newElement("One"));
        session.save(session.newElement("One"));
      }
      setup.shutdownServer(server2);
      try (ODatabaseSession session = remote.open("test", "admin", "admin")) {
        session.save(session.newElement("One"));
      }
    }
    setup.shutdownServer(server1);
    setup.shutdownServer(server0);
    // Starting the servers in reverse shutdown order to trigger miss sync
    setup.startServer(server2);
    setup.startServer(server1);
    setup.startServer(server0);
    // Test server 0
    try (OrientDB remote = new OrientDB("remote:localhost", OrientDBConfig.defaultConfig())) {
      try (ODatabaseSession session = remote.open("test", "admin", "admin")) {
        assertEquals(session.countClass("One"), 3);
      }
    }
    // Test server 1
    try (OrientDB remote = new OrientDB("remote:localhost:2425", OrientDBConfig.defaultConfig())) {
      try (ODatabaseSession session = remote.open("test", "admin", "admin")) {
        assertEquals(session.countClass("One"), 3);
      }
    }
    // Test server 2
    try (OrientDB remote = new OrientDB("remote:localhost:2426", OrientDBConfig.defaultConfig())) {
      try (ODatabaseSession session = remote.open("test", "admin", "admin")) {
        assertEquals(session.countClass("One"), 3);
      }
    }
  }

  @After
  public void after() {
    setup.teardown();
  }
}
