/**
 * 
 */
package io.nats.client;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static io.nats.client.Constants.*;

/**
 * @author larry
 *
 */
public class EncodedConnectionTest {
	@Rule
	public TestCasePrinterRule pr = new TestCasePrinterRule(System.out);

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	EncodedConnectionImpl newEConn() {
		Connection nc = null;
		EncodedConnectionImpl ec = null;
		try {
			nc = new ConnectionFactory().createConnection();
			ec = new EncodedConnectionImpl(nc, DEFAULT_ENCODER);		
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}
		return ec;
	}
	
	@Test
	public void testConstructorErrors() {
		Connection c = null;
		
		boolean exThrown = false;
		try {
			new EncodedConnectionImpl(c, DEFAULT_ENCODER);
		} catch (NullPointerException e) {
			exThrown = true;
		}
		assertTrue("Expected exception for null connection", exThrown);

		c = mock(Connection.class);

		exThrown = false;
		try {
			new EncodedConnectionImpl(c, "foo22");
		} catch (IllegalArgumentException e) {
			exThrown = true;
		}
		assertTrue("Expected exception for bad encoder", exThrown);

		when(c.isClosed()).thenReturn(true);
		try {
			new EncodedConnectionImpl(c, "default");
		} catch (IllegalStateException e) {
			assertEquals(ERR_CONNECTION_CLOSED, e.getMessage());
			exThrown = true;
		}
		assertTrue("Expected exception for closed connection", exThrown);
	}
	
	@Test
	public void testMarshalString() {
		
	}

}
