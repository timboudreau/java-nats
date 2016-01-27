package io.nats.client.encoders;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.example.tutorial.AddressBookProtos;
import com.example.tutorial.AddressBookProtos.AddressBook;
import com.example.tutorial.AddressBookProtos.Person;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.InvalidProtocolBufferException;

import io.nats.client.*;
import static io.nats.client.Constants.*;

import static io.nats.client.UnitTestUtilities.*;

public class ProtobufEncoderTest {
	static final int TEST_PORT = 8086;

	@Rule
	public TestCasePrinterRule pr = new TestCasePrinterRule(System.out);

	UnitTestUtilities utils = new UnitTestUtilities();

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
	public void testEncodeDecode() {
		try (NATSServer ns = new NATSServer(TEST_PORT)) { 
			sleep(50);
			Message foo;
			try (Connection c = new ConnectionFactory("nats://localhost:"+TEST_PORT).createConnection()) {
				try (EncodedConnection ec = new EncodedConnectionImpl(c, PROTOBUF_ENCODER)) {
					InputStream input = 
							getClass().getClassLoader().getResourceAsStream("addressbook.desc");
					assertNotNull("Couldn't load protobuf example descriptor file", input);
					ProtobufEncoder.registerMapping(input);

					AddressBookProtos.Person.Builder personBuilder = 
							AddressBookProtos.Person.newBuilder();

					final AddressBookProtos.Person person1 = 
							personBuilder.setName("Derek")
							.setId(22)
							.setEmail("derek@foobar.com")
							.build();

					final AddressBookProtos.Person person2 = 
							personBuilder.setName("Larry")
							.setId(122)
							.setEmail("larry@foobar.com")
							.build();

					AddressBookProtos.AddressBook.Builder 
						addressBookBuilder = AddressBookProtos.AddressBook.newBuilder();
					final AddressBook addrBook = addressBookBuilder
							.addPerson(person1)
							.addPerson(person2)
							.build();

					final Channel<AddressBook> ch = new Channel<AddressBook>();
					ec.subscribe("foo", new Handler<AddressBook>() {
						@Override
						public void handle(EncodedMessage<AddressBook> msg) {
							ch.add(msg.getObject());
						}
					});

					ec.publish("foo", addrBook);

					AddressBook ab = ch.get(5, TimeUnit.SECONDS);
					assertTrue(ab.equals(addrBook));
					
					List<Person> entries = ab.getPersonList();
					System.err.println("Number of entries: " + entries.size());
					System.err.println("AddressBook:\n" + ab);

					// Send using regular connection and non-encoded message
					c.subscribe("foo2", new MessageHandler() {
						@Override
						public void onMessage(Message msg) {
							AddressBook newBook=null;
							try {
								newBook = AddressBook.parseFrom(msg.getData());
								assertTrue(addrBook.equals(newBook));
							} catch (InvalidProtocolBufferException e) {
								e.printStackTrace();
								fail(e.getMessage());
							}
							ch.add(newBook);
						}
					});
					
					c.publish("foo2", addrBook.toByteArray());

					AddressBook ab2 = ch.get(5, TimeUnit.SECONDS);
					List<Person> entries2 = ab2.getPersonList();
					System.err.println("Number of entries: " + entries2.size());
					System.err.println("AddressBook2:\n" + ab2);

					assertTrue(ab2.equals(addrBook));
					
				} catch (DescriptorValidationException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
	}
}
