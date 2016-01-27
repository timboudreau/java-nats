/**
 * 
 */
package io.nats.client;

import java.lang.Exception;

/**
 *
 */
public interface Encoder<T> {
	/**
	 * 
	 * @param subject the subject of interest
	 * @param obj the object to encode
	 * @return the encoded representation of {@code obj}
	 * @throws Exception if encoding fails
	 */
	byte[] encode(String subject, T obj) throws Exception;

	/**
	 * 
	 * @param subject the subject of interest
	 * @param data the encoded data
	 * @return the decoded Object
	 * @throws Exception 
	 */
	T decode(String subject, byte[] data) throws Exception;
}
