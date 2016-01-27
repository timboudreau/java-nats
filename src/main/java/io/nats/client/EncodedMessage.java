package io.nats.client;

public class EncodedMessage<T> extends Message {

	private T obj;
	private Encoder<T> enc;
	
	EncodedMessage(Message msg, Encoder<T> enc) {
		this.subject = msg.subject;
		this.replyTo = msg.replyTo;
		this.sub = msg.sub;
		this.data = msg.data;
		this.enc = enc;
	}

	public void setObject(final T obj) {
		this.obj = obj;
	}

	public T getObject() {
		if (this.obj == null) {
			try {
				this.obj = enc.decode(this.subject, this.data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return this.obj;
	}
}
