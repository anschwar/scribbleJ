package de.hska.scribble;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.scribble.context.DefaultModuleContext;
import org.scribble.context.ModuleContext;
import org.scribble.context.ModuleLoader;
import org.scribble.logging.ConsoleIssueLogger;
import org.scribble.logging.IssueLogger;
import org.scribble.model.Module;
import org.scribble.model.local.LProtocolDefinition;
import org.scribble.monitor.export.MonitorExporter;
import org.scribble.monitor.model.SessionType;
import org.scribble.monitor.runtime.DefaultMonitor;
import org.scribble.monitor.runtime.Monitor;
import org.scribble.monitor.runtime.MonitorMessage;
import org.scribble.monitor.runtime.SessionInstance;
import org.scribble.parser.ProtocolModuleLoader;
import org.scribble.parser.ProtocolParser;
import org.scribble.resources.DirectoryResourceLocator;
import org.scribble.resources.InputStreamResource;
import org.scribble.resources.Resource;


public class RecursiveProtocolTest {

   private static final String PROTOCOL_NAME = "PrintServer";

   private static final String   SEND           = "send";
   private static final String   DONE           = "done";
   private static final String   MSG            = "java.lang.String";

   private static final String   ROLE_CLIENT    = "Client";
   private static final String   ROLE_SERVER    = "Server";

   private final ProtocolParser  _parser        = new ProtocolParser();
   private final IssueLogger     _logger        = new ConsoleIssueLogger();
   private final MonitorExporter _exportMonitor = new MonitorExporter();
   private final Monitor         _monitor       = new DefaultMonitor();

   public SessionType loadSessionType( String protocalName ) {
      SessionType type = null;
      try {
         URL srcUrl = ClassLoader.getSystemResource(protocalName + ".scr");
         InputStream is = new FileInputStream(new File(srcUrl.getFile()));
         DirectoryResourceLocator locator = new DirectoryResourceLocator(protocalName);
         ModuleLoader loader = new ProtocolModuleLoader(_parser, locator, _logger);
         Resource res = new InputStreamResource(protocalName, is);
         Module module = _parser.parse(res, loader, _logger);
         ModuleContext context = new DefaultModuleContext(res, module, loader);
         type = _exportMonitor.export(context, (LProtocolDefinition)module.getProtocol(PROTOCOL_NAME));
      }
      catch ( Exception e ) {
         e.printStackTrace();
      }
      return type;
   }

   /**
    * A successful communication from the point of view of the client
    */
   @Test
   public void clientTestSuccessful() {
      // load the local protocol for the client
      SessionType sessionType = loadSessionType("printServer_Client");
      SessionInstance instance = new SessionInstance();
      _monitor.initializeInstance(sessionType, instance);

      MonitorMessage outgoingMessage;
      // send 4 messages
      for ( int i = 0; i < 4; i++ ) {
         outgoingMessage = new MonitorMessage();
         outgoingMessage.setOperator(SEND);
         outgoingMessage.getTypes().add(MSG);
         // the sent operation consist of the local protocol represented by the session type,
         // the session instance, the message an the role of the receiver
         Assert.assertTrue(_monitor.sent(sessionType, instance, outgoingMessage, ROLE_SERVER));
      }
      // and then send an EOF
      outgoingMessage = new MonitorMessage();
      outgoingMessage.setOperator(DONE);
      Assert.assertTrue(_monitor.sent(sessionType, instance, outgoingMessage, ROLE_SERVER));
      // assert that the communication was successful from the point of view of the client
      Assert.assertTrue(instance.hasCompleted());
   }

   /**
    * The client doesn't send the EOF so the protocol wasn't executed successful
    */
   @Test
   public void clientTestFailure() {
      // load the local protocol for the client
      SessionType sessionType = loadSessionType("printServer_Client");
      SessionInstance instance = new SessionInstance();
      _monitor.initializeInstance(sessionType, instance);

      MonitorMessage outgoingMessage;
      // send 4 messages
      for ( int i = 0; i < 4; i++ ) {
         outgoingMessage = new MonitorMessage();
         outgoingMessage.setOperator(SEND);
         outgoingMessage.getTypes().add(MSG);
         // the sent operation consist of the local protocol represented by the session type,
         // the session instance, the message an the role of the receiver
         Assert.assertTrue(_monitor.sent(sessionType, instance, outgoingMessage, ROLE_SERVER));
      }
      // assert that the communication was successful from the point of view of the client
      Assert.assertFalse(instance.hasCompleted());
   }

   /**
    * A successful communication from the point of view of the Server
    */
   @Test
   public void serverTestSuccessful() {
      // load the local protocol for the client
      SessionType sessionType = loadSessionType("printServer_Server");
      SessionInstance instance = new SessionInstance();
      _monitor.initializeInstance(sessionType, instance);

      MonitorMessage incomingMessage;
      // send 4 messages
      for ( int i = 0; i < 4; i++ ) {
         incomingMessage = new MonitorMessage();
         incomingMessage.setOperator(SEND);
         incomingMessage.getTypes().add(MSG);
         // the sent operation consist of the local protocol represented by the session type,
         // the session instance, the message an the role of the receiver
         Assert.assertTrue(_monitor.received(sessionType, instance, incomingMessage, ROLE_CLIENT));
      }
      // and then send an EOF
      incomingMessage = new MonitorMessage();
      incomingMessage.setOperator(DONE);
      Assert.assertTrue(_monitor.received(sessionType, instance, incomingMessage, ROLE_CLIENT));
      // assert that the communication was successful from the point of view of the client
      Assert.assertTrue(instance.hasCompleted());
   }

   /**
    * The server doesn't receive the EOF so the protocol wasn't executed successful
    */
   @Test
   public void serverTestFailure() {
      // load the local protocol for the client
      SessionType sessionType = loadSessionType("printServer_Server");
      SessionInstance instance = new SessionInstance();
      _monitor.initializeInstance(sessionType, instance);

      MonitorMessage incomingMessage;
      // send 4 messages
      for ( int i = 0; i < 4; i++ ) {
         incomingMessage = new MonitorMessage();
         incomingMessage.setOperator(SEND);
         incomingMessage.getTypes().add(MSG);
         // the sent operation consist of the local protocol represented by the session type,
         // the session instance, the message an the role of the receiver
         Assert.assertTrue(_monitor.received(sessionType, instance, incomingMessage, ROLE_CLIENT));
      }
      // assert that the communication was successful from the point of view of the client
      Assert.assertFalse(instance.hasCompleted());
   }

   /**
    * Client sends messages to the server which the server receives. Amount of messages sent and received are equal.
    */
   @Test
   public void communicationTestSuccess() {
      // load the local protocol for the client and server and init it
      SessionType sessionTypeClient = loadSessionType("printServer_Client");
      SessionType sessionTypeServer = loadSessionType("printServer_Server");
      SessionInstance clientInstance = new SessionInstance();
      SessionInstance serverInstance = new SessionInstance();
      Monitor clientMonitor = new DefaultMonitor();
      Monitor serverMonitor = new DefaultMonitor();
      clientMonitor.initializeInstance(sessionTypeClient, clientInstance);
      serverMonitor.initializeInstance(sessionTypeServer, serverInstance);

      MonitorMessage outgoingMessage;
      MonitorMessage incomingMessage;
      // send 4 messages from client and receive it with the server
      for ( int i = 0; i < 4; i++ ) {
         // client
         outgoingMessage = new MonitorMessage();
         outgoingMessage.setOperator(SEND);
         outgoingMessage.getTypes().add(MSG);
         // server
         incomingMessage = new MonitorMessage();
         incomingMessage.setOperator(SEND);
         incomingMessage.getTypes().add(MSG);
         Assert.assertTrue(clientMonitor.sent(sessionTypeClient, clientInstance, outgoingMessage, ROLE_SERVER));
         Assert.assertTrue(serverMonitor.received(sessionTypeServer, serverInstance, incomingMessage, ROLE_CLIENT));
      }
      // notify end
      outgoingMessage = new MonitorMessage();
      outgoingMessage.setOperator(DONE);
      incomingMessage = new MonitorMessage();
      incomingMessage.setOperator(DONE);
      // send it
      Assert.assertTrue(clientMonitor.sent(sessionTypeClient, clientInstance, outgoingMessage, ROLE_SERVER));
      Assert.assertTrue(serverMonitor.received(sessionTypeServer, serverInstance, incomingMessage, ROLE_CLIENT));

      // assert that the communication was successful from the point of view of the client
      Assert.assertTrue(clientInstance.hasCompleted());
      Assert.assertTrue(serverInstance.hasCompleted());
   }

   /**
    * Client sends messages to the server which the server receives. Amount of messages sent and received are not equal
    * because the done message was lost.
    */
   @Test
   public void communicationTestFailure() {
      // load the local protocol for the client and server and init it
      SessionType sessionTypeClient = loadSessionType("printServer_Client");
      SessionType sessionTypeServer = loadSessionType("printServer_Server");
      SessionInstance clientInstance = new SessionInstance();
      SessionInstance serverInstance = new SessionInstance();
      Monitor clientMonitor = new DefaultMonitor();
      Monitor serverMonitor = new DefaultMonitor();
      clientMonitor.initializeInstance(sessionTypeClient, clientInstance);
      serverMonitor.initializeInstance(sessionTypeServer, serverInstance);

      MonitorMessage outgoingMessage;
      MonitorMessage incomingMessage;
      // send 4 messages from client and receive it with the server
      for ( int i = 0; i < 4; i++ ) {
         // client
         outgoingMessage = new MonitorMessage();
         outgoingMessage.setOperator(SEND);
         outgoingMessage.getTypes().add(MSG);
         // server
         incomingMessage = new MonitorMessage();
         incomingMessage.setOperator(SEND);
         incomingMessage.getTypes().add(MSG);
         Assert.assertTrue(clientMonitor.sent(sessionTypeClient, clientInstance, outgoingMessage, ROLE_SERVER));
         Assert.assertTrue(serverMonitor.received(sessionTypeServer, serverInstance, incomingMessage, ROLE_CLIENT));
      }
      // notify end
      outgoingMessage = new MonitorMessage();
      outgoingMessage.setOperator(DONE);
      // send it
      Assert.assertTrue(clientMonitor.sent(sessionTypeClient, clientInstance, outgoingMessage, ROLE_SERVER));

      // assert that the communication was successful from the point of view of the client
      Assert.assertTrue(clientInstance.hasCompleted());
      Assert.assertFalse(serverInstance.hasCompleted());
   }
}
