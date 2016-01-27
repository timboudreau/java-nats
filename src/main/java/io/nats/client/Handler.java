/**
 * 
 */
package io.nats.client;

/**
 * 
 *
 */
public interface Handler<T> {
//	void handle(T object);
//	void handle(String subject, T object);
//	void handle(String subject, String reply, T object);
	void handle(EncodedMessage<T> msg);
//	void handle(T object, String... strings);
}
