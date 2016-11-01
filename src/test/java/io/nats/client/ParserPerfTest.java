package io.nats.client;

import static io.nats.client.UnitTestUtilities.newMockedTcpConnection;
import static io.nats.client.UnitTestUtilities.setLogLevel;
import static org.junit.Assert.fail;

import ch.qos.logback.classic.Level;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Category(PerfTest.class)
public class ParserPerfTest {
    static final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    static final Logger logger = LoggerFactory.getLogger(ParserPerfTest.class);

    static final LogVerifier verifier = new LogVerifier();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TestCasePrinterRule pr = new TestCasePrinterRule(System.out);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    /**
     * @throws java.lang.Exception if a problem occurs
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        verifier.setup();
    }

    /**
     * @throws java.lang.Exception if a problem occurs
     */
    @After
    public void tearDown() throws Exception {
        verifier.teardown();
        setLogLevel(Level.WARN);
    }

    @Test
    public void testParserPerformance() throws IOException, TimeoutException {
        try (TcpConnection mock = newMockedTcpConnection()) {
            ConnectionFactory cf = new ConnectionFactory();
            try (ConnectionImpl conn = cf.createConnection(new TcpConnectionFactoryMock())) {
                final int bufSize = 65536;
                int count = 40000;

                Parser parser = new Parser(conn);

                byte[] buf = new byte[bufSize];

                String msg = "MSG foo 1 4\r\ntest\r\n";
                byte[] msgBytes = msg.getBytes();
                int length = msgBytes.length;

                int bufLen = 0;
                int numMsgs = 0;
                for (int i = 0; (i + length) <= bufSize; i += length, numMsgs++) {
                    System.arraycopy(msgBytes, 0, buf, i, length);
                    bufLen += length;
                }

                System.err.printf("Parsing %d buffers of %d messages each (total=%d)\n", count,
                        numMsgs, count * numMsgs);

                long t0 = System.nanoTime();
                for (int i = 0; i < count; i++) {
                    try {
                        parser.parse(buf, bufLen);
                    } catch (ParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        System.err.println("Error offset=" + e.getErrorOffset());
                        break;
                    }
                }
                long elapsed = System.nanoTime() - t0;
                long avgNsec = elapsed / (count * numMsgs);
                long elapsedSec = TimeUnit.NANOSECONDS.toSeconds(elapsed);
                // long elapsedMsec = TimeUnit.NANOSECONDS.toMicros(elapsedNanos);

                long totalMsgs = numMsgs * count;
                System.err.printf("Parsed %d messages in %ds (%d msg/sec)\n", totalMsgs, elapsedSec,
                        (totalMsgs / elapsedSec));

                double totalBytes = (double) count * bufLen;
                double mbPerSec = (double) totalBytes / elapsedSec / 1000000;
                System.err.printf("Parsed %.0fMB in %ds (%.0fMB/sec)\n", totalBytes / 1000000,
                        elapsedSec, mbPerSec);

                System.err.printf("Average parse time per msg = %dns\n", avgNsec);
            } catch (IOException | TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }
}
