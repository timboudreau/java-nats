package io.nats.client;

public interface EncodedConnection extends AbstractConnection {

	void subscribe(String subject, Handler<?> handler);

	<T> void subscribe(String subject, String queue, Handler<T> handler);

	<T> void publish(String string, T obj);

}
