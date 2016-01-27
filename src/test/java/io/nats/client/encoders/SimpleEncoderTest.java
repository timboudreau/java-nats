package io.nats.client.encoders;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleEncoderTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEncodeByteArray() {
		SimpleEncoder<Object> enc = new SimpleEncoder<Object>();
		
		String str = "Hello World";
		byte[] data = null;
		try {
			data = enc.encode("subjstr", str.getBytes());
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertNotNull(data);
		assertArrayEquals(str.getBytes(), data);
		
		byte[] result = null;
		try {
			result = (byte[])enc.decode("subjstr", data);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertNotNull(result);
		assertArrayEquals(str.getBytes(), result);
		assertEquals(str, new String(result));
	}
	
	@Test
	public void testEncodeString() {
		SimpleEncoder<Object> enc = new SimpleEncoder<Object>();
		
		String str = "Hello World";
		byte[] data = null;
		try {
			data = enc.encode("subjstr", str);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertNotNull(data);
		
		String result = null;
		try {
			result = (String)enc.decode("subjstr", data);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertNotNull(result);
		assertEquals(str, result);
	}
	
	@Test
	public void testEncodeBoolean() {
		SimpleEncoder<Object> enc = new SimpleEncoder<Object>();
		
		boolean bval = true;
		byte[] data = null;
		String subj = "subjstr";

		try {
			data = enc.encode(subj, bval);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertNotNull(data);
		
		boolean result = false;
		try {
			result = (boolean)enc.decode(subj, data);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertNotNull(result);
		assertEquals(bval, result);
	}
	
	@Test
	public void testEncodeNumber() {
		SimpleEncoder<Object> enc = new SimpleEncoder<Object>();
		
		int val = 42;
		int decodedVal = Integer.MIN_VALUE;
		String subj = "subjstr";
		try {
			decodedVal = (int)enc.decode(subj, enc.encode(subj, val));
			assertEquals(val, decodedVal);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		long longval = 84L;
		long decodedLongVal = Long.MIN_VALUE;
		try {
			decodedLongVal = (long)enc.decode(subj, enc.encode(subj, longval));
			assertEquals(longval, decodedLongVal);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		short shortval = 126;
		short decodedshortval = Short.MIN_VALUE;
		try {
			decodedshortval = (short)enc.decode(subj, enc.encode(subj, shortval));
			assertEquals(shortval, decodedshortval);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		double dval = 42.4;
		double decodedDoubleVal = Double.MIN_VALUE;
		try {
			decodedDoubleVal = (double) enc.decode(subj, enc.encode(subj, dval));
			assertTrue(dval == decodedDoubleVal);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		float fval = (float)42.4;
		float decodedfloatVal = Float.MIN_VALUE;
		try {
			decodedfloatVal = (float) enc.decode(subj, enc.encode(subj, fval));
			assertTrue(fval == decodedfloatVal);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
