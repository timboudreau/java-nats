/**
 * 
 */
package io.nats.client.encoders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;

import io.nats.client.Encoder;

/**
 * 
 *
 */
public class ProtobufEncoder<T extends Message> implements Encoder<T> {

	final static Logger logger = LoggerFactory.getLogger(ProtobufEncoder.class);
	
	@Override
	public byte[] encode(String subject, T obj) 
			throws Exception {
		if (!(obj instanceof Message)) {
			throw new InvalidProtocolBufferException("Invalid protobuf object");
		}
		return objectToByteBuffer(obj);
	}

	@Override
	public T decode(String subject, byte[] data) 
			throws Exception {
		T obj = objectFromByteBuffer(data); 
		return obj;
	}

	/*
	 * See http://planet.jboss.org/post/generic_marshalling_with_google_protocol_buffers
	 */
	final static Map<String, String> mapping = new HashMap<String, String>();
	final static Lock mapLock = new ReentrantLock();

	public static void registerMapping(String filename) throws IOException, 
	DescriptorValidationException 
	{
		InputStream is = new FileInputStream(filename);
		registerMapping(is);
	}

	public static void registerMapping(InputStream inputStream) throws IOException, 
	DescriptorValidationException 
	{
		mapLock.lock();
		try {
			FileDescriptorSet descriptorSet = FileDescriptorSet.parseFrom(inputStream);

			for (FileDescriptorProto fdp: descriptorSet.getFileList()) {
				FileDescriptor fd = FileDescriptor.buildFrom(fdp,
						new FileDescriptor[]{});

				for (Descriptor descriptor : fd.getMessageTypes()) {
					String className = fdp.getOptions().getJavaPackage() + "." 
							+ fdp.getOptions().getJavaOuterClassname() + "$" 
							+ descriptor.getName();
					logger.info("Registering descriptor " + descriptor.getFullName() + " for class " + className);
					mapping.put(descriptor.getFullName(), className);
				}
			}
		} finally {
			mapLock.unlock();
		}
	}
	public byte[] objectToByteBuffer(Object o) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Message message = (Message) o;
		byte[] name = message.getDescriptorForType().getFullName()
				.getBytes("UTF-8");
		baos.write(name.length); // TODO: Length as int and not byte
		// Write the full descriptor name, i.e. protobuf.Person
		baos.write(name);
		byte[] messageBytes = message.toByteArray();
		baos.write(messageBytes.length); // TODO: Length as int and not byte
		baos.write(messageBytes);
		return baos.toByteArray();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public T objectFromByteBuffer(byte[] buffer) throws Exception {
	    ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
	    byte[] name = new byte[bais.read()];
	    bais.read(name); // TODO: Read fully??
	    // Get the class name associated with the descriptor name
	    String className = mapping.get(new String(name, "UTF-8"));
	    Class clazz = Thread.currentThread().getContextClassLoader()
	       .loadClass(className);
	    Method parseFromMethod = clazz.getMethod("parseFrom", byte[].class);
	    byte[] message = new byte[bais.read()];
	    bais.read(message); // TODO: Read fully??
	    return (T) parseFromMethod.invoke(null, message);
	 }
}
