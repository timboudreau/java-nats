/*******************************************************************************
 * Copyright (c) 2015-2016 Apcera Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the MIT License (MIT) which accompanies this
 * distribution, and is available at http://opensource.org/licenses/MIT
 *******************************************************************************/

package io.nats.client;

import static io.nats.client.ConnectionImpl.PONG_PROTO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UnitTestUtilities {
    // final Object mu = new Object();
    static NatsServer defaultServer = null;
    Process authServerProcess = null;

    static final String defaultInfo =
            "INFO {\"server_id\":\"a1c9cf0c66c3ea102c600200d441ad8e\",\"version\":\"0.7.2\",\"go\":"
                    + "\"go1.4.2\",\"host\":\"0.0.0.0\",\"port\":4222,\"auth_required\":false,"
                    + "\"ssl_required\":false,\"tls_required\":false,\"tls_verify\":false,"
                    + "\"max_payload\":1048576}";

    static final String defaultAsyncInfo =
            "INFO {\"server_id\":\"a1c9cf0c66c3ea102c600200d441ad8e\",\"version\":\"0.7.2\",\"go\":"
                    + "\"go1.4.2\",\"host\":\"0.0.0.0\",\"port\":4222,\"auth_required\":false,"
                    + "\"ssl_required\":false,\"tls_required\":false,\"tls_verify\":false,"
                    + "\"connect_urls\":[\"localhost:5222\"]," + "\"max_payload\":1048576}";

    static synchronized NatsServer runDefaultServer() {
        return runDefaultServer(false);
    }

    static synchronized NatsServer runDefaultServer(boolean debug) {
        NatsServer ns = new NatsServer(debug);
        sleep(100, TimeUnit.MILLISECONDS);
        return ns;
    }

    static synchronized Connection newDefaultConnection() throws IOException, TimeoutException {
        return new ConnectionFactory().createConnection();
    }

    static synchronized Connection newDefaultConnection(TcpConnectionFactory tcf)
            throws IOException, TimeoutException {
        return new ConnectionFactory().createConnection(tcf);
    }

    static synchronized Connection newDefaultConnection(TcpConnectionFactory tcf, Options opts)
            throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        ConnectionImpl conn = new ConnectionImpl(opts != null ? opts : cf.options(), tcf);
        conn.connect();
        return conn;
    }

    static synchronized Connection newMockedConnection() throws IOException, TimeoutException {
        return newMockedConnection(null, null);
    }

    static synchronized Connection newMockedConnection(TcpConnectionFactoryMock tcf)
            throws IOException, TimeoutException {
        return newMockedConnection(tcf, null);
    }

    static synchronized Connection newMockedConnection(Options opts)
            throws IOException, TimeoutException {
        return newMockedConnection(null, opts);
    }

    static synchronized Connection newMockedConnection(TcpConnectionFactoryMock tcf, Options opts)
            throws IOException, TimeoutException {
        TcpConnectionFactory tcpConnFactory = null;
        if (tcf != null) {
            tcpConnFactory = null;
        } else {
            tcpConnFactory = new TcpConnectionFactoryMock();
        }
        return newDefaultConnection(tcpConnFactory,
                opts != null ? opts : new ConnectionFactory().options());
    }

    protected static Connection newNewMockedConnection() throws IOException, TimeoutException {
        return newNewMockedConnection(null);
    }

    protected static io.nats.client.Connection newNewMockedConnection(Options opts)
            throws IOException, TimeoutException {
        if (opts == null) {
            opts = new ConnectionFactory().options();
        }
        final ConnectionImpl nc = spy(new ConnectionImpl(opts));

        // Set up mock TcpConnection
        final TcpConnectionFactory tcfMock = newMockedTcpConnectionFactory();
        final TcpConnection tcpConnMock = newMockedTcpConnection();
        when(tcfMock.createConnection()).thenReturn(tcpConnMock);
        nc.setTcpConnectionFactory(tcfMock);

        when(nc.newInbox())
                .thenReturn(String.format("_INBOX.%s", io.nats.client.NUID.nextGlobal()));

        nc.setInputStream(tcpConnMock.getInputStream(65536));
        nc.setOutputStream(tcpConnMock.getOutputStream());

        nc.connect();

        return nc;
    }

    static synchronized TcpConnection newMockedTcpConnection()
            throws IOException, TimeoutException {
        final TcpConnection tcpConnMock = mock(TcpConnection.class);
        final BufferedReader bufferedReaderMock = mock(BufferedReader.class);
        final BufferedInputStream brMock = mock(BufferedInputStream.class);
        final BufferedOutputStream bwMock = mock(BufferedOutputStream.class);

        // isConnected
        doReturn(true).when(tcpConnMock).isConnected();

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final byte[] pingProtoBytes = ConnectionImpl.PING_PROTO.getBytes();
                final byte[] pongProtoBytes = ConnectionImpl.PONG_PROTO.getBytes();

                // when(br.readLine()).thenReturn("PONG");
                // Object[] args = invocation.getArguments();

                // Setup bufferedReaderMock
                when(tcpConnMock.getBufferedReader()).thenReturn(bufferedReaderMock);

                // Setup br
                when(tcpConnMock.getInputStream(anyInt())).thenReturn(brMock);

                // Setup br
                when(tcpConnMock.getOutputStream(anyInt())).thenReturn(bwMock);

                // First lines are always INFO and then PONG
                when(bufferedReaderMock.readLine()).thenReturn(TcpConnectionMock.defaultInfo,
                        PONG_PROTO.trim());

                // Additional pings
                doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) throws Throwable {
                        when(brMock.read(any(byte[].class))).thenReturn(pongProtoBytes.length);
                        return null;
                    }
                }).when(bwMock).write(pingProtoBytes, 0, pingProtoBytes.length);

                doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) throws Throwable {
                        when(tcpConnMock.isConnected()).thenReturn(false);
                        return null;
                    }
                }).when(tcpConnMock).close();
                return null;
            }
        }).when(tcpConnMock).open(any(String.class), anyInt(), anyInt());
        return tcpConnMock;
    }

    static synchronized TcpConnectionFactory newMockedTcpConnectionFactory() {
        TcpConnectionFactory tcf = mock(TcpConnectionFactory.class);
        try {
            doReturn(newMockedTcpConnection()).when(tcf).createConnection();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
        return tcf;
    }

    static synchronized void startDefaultServer() {
        startDefaultServer(false);
    }

    static synchronized void startDefaultServer(boolean debug) {
        if (defaultServer == null) {
            defaultServer = runDefaultServer(debug);
        }
    }

    static synchronized void stopDefaultServer() {
        if (defaultServer != null) {
            defaultServer.shutdown();
            defaultServer = null;
        }
    }

    static synchronized void bounceDefaultServer(int delayMillis) {
        stopDefaultServer();
        sleep(delayMillis);
        startDefaultServer();
    }

    public void startAuthServer() throws IOException {
        authServerProcess = Runtime.getRuntime().exec("gnatsd -config auth.conf");
    }

    static NatsServer runServerOnPort(int port) {
        return runServerOnPort(port, false);
    }

    static NatsServer runServerOnPort(int port, boolean debug) {
        NatsServer srv = new NatsServer(port, debug);
        sleep(500);
        return srv;
    }

    static NatsServer runServerWithConfig(String configFile) {
        return runServerWithConfig(configFile, false);
    }

    static NatsServer runServerWithConfig(String configFile, boolean debug) {
        NatsServer srv = new NatsServer(configFile, debug);
        sleep(500);
        return srv;
    }

    static String getCommandOutput(String command) {
        String output = null; // the string to return

        Process process = null;
        BufferedReader reader = null;
        InputStreamReader streamReader = null;
        InputStream stream = null;

        try {
            process = Runtime.getRuntime().exec(command);

            // Get stream of the console running the command
            stream = process.getInputStream();
            streamReader = new InputStreamReader(stream);
            reader = new BufferedReader(streamReader);

            // store current line of output from the cmd
            String currentLine = null;
            // build up the output from cmd
            StringBuilder commandOutput = new StringBuilder();
            while ((currentLine = reader.readLine()) != null) {
                commandOutput.append(currentLine + "\n");
            }

            int returnCode = process.waitFor();
            if (returnCode == 0) {
                output = commandOutput.toString();
            }

        } catch (IOException e) {
            System.err.println("Cannot retrieve output of command");
            System.err.println(e);
            output = null;
        } catch (InterruptedException e) {
            System.err.println("Cannot retrieve output of command");
            System.err.println(e);
        } finally {
            // Close all inputs / readers

            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    System.err.println("Cannot close stream input! " + e);
                }
            }
            if (streamReader != null) {
                try {
                    streamReader.close();
                } catch (IOException e) {
                    System.err.println("Cannot close stream input reader! " + e);
                }
            }
            if (reader != null) {
                try {
                    streamReader.close();
                } catch (IOException e) {
                    System.err.println("Cannot close stream input reader! " + e);
                }
            }
        }
        // Return the output from the command - may be null if an error occured
        return output;
    }

    void getConnz() {
        URL url = null;
        try {
            url = new URL("http://localhost:8222/connz");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            if (url != null) {
                try (InputStream urlInputStream = url.openStream()) {
                    try (BufferedReader reader =
                            new BufferedReader(new InputStreamReader(urlInputStream, "UTF-8"))) {
                        for (String line; (line = reader.readLine()) != null;) {
                            System.out.println(line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void sleep(int timeout) {
        sleep(timeout, TimeUnit.MILLISECONDS);
    }

    static void sleep(int duration, TimeUnit unit) {
        try {
            unit.sleep(duration);
        } catch (InterruptedException e) {
            /* NOOP */
        }
    }

    static boolean await(CountDownLatch latch) {
        return await(latch, 5, TimeUnit.SECONDS);
    }

    static boolean await(CountDownLatch latch, long timeout, TimeUnit unit) {
        boolean val = false;
        try {
            val = latch.await(timeout, unit);
        } catch (InterruptedException e) {
            /* NOOP */
        }
        return val;
    }

    static synchronized void setLogLevel(ch.qos.logback.classic.Level level) {
        ch.qos.logback.classic.Logger lbLog =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("io.nats.client");
        lbLog.setLevel(level);
    }

    static void processServerConfigFile(String configFile) {

    }
}
