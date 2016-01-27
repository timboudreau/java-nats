package io.nats.client.encoders;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import io.nats.client.TestCasePrinterRule;

public class SerializableEncoderTest {
	@Rule
	public TestCasePrinterRule pr = new TestCasePrinterRule(System.out);
	
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

//	@Test
//	public void testDecode() {
//		fail("Not yet implemented"); // TODO
//	}

	@Test
	public void testEncodeString() {
		SerializableEncoder enc = new SerializableEncoder();

		String subject = "foo";
		String str = "Hello, World!";

		byte[] data = null;
		try {
			data = enc.encode(subject, str);
			System.err.println("data.length="+data.length);
		} catch (IOException e) {
			fail(e.getMessage());		
		} 
		
		String decodedString = null;
		try {
			decodedString = (String) enc.decode(subject, data);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertEquals(str, decodedString);
	}
	
	@Test
	public void testEncodeInt() {
		SerializableEncoder enc = new SerializableEncoder();

		String subject = "foo";
		int theInt = 42;

		byte[] data = null;
		try {
			data = enc.encode(subject, theInt);
			System.err.println("data.length="+data.length);
		} catch (IOException e) {
			fail(e.getMessage());		
		} 
		
		int decodedInt = Integer.MIN_VALUE;
		try {
			decodedInt = (int) enc.decode(subject, data);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertEquals(theInt, decodedInt);
	}
	
	@Test
	public void testEncodeByteArray() {
		SerializableEncoder enc = new SerializableEncoder();

		String subject = "foo";
		byte[] theArray = "Hello".getBytes();
		System.err.println("Original array length = " + theArray.length);

		byte[] data = null;
		try {
			data = enc.encode(subject, theArray);
			System.err.println("data.length="+data.length);
		} catch (IOException e) {
			fail(e.getMessage());		
		} 

		assertEquals(theArray, data);
		
		byte[] decodedArray = null;
		try {
			decodedArray = (byte[]) enc.decode(subject, data);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertEquals(theArray, decodedArray);
	}

}
