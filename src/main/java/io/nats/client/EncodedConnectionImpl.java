package io.nats.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.nats.client.Constants.ConnState;
import io.nats.client.encoders.ProtobufEncoder;
import io.nats.client.encoders.SerializableEncoder;
import io.nats.client.encoders.SimpleEncoder;

import static io.nats.client.Constants.*;

public class EncodedConnectionImpl implements EncodedConnection {
	
	Connection nc = null;
	Encoder encoder = null;
	
	private static Map<String,Encoder<?>> encMap;
	static {
		encMap = new HashMap<String, Encoder<?>>();
		encMap.put(DEFAULT_ENCODER, new SimpleEncoder());
		encMap.put(NATIVE_ENCODER, new SerializableEncoder());
		encMap.put(PROTOBUF_ENCODER, new ProtobufEncoder());
	}
	
	final static Lock encLock = new ReentrantLock();
	
	public EncodedConnectionImpl(Connection c, String encoderName) {
		if (c == null) {
			throw new NullPointerException("Connection cannot be null");
		}
		if (c.isClosed()) {
			throw new IllegalStateException(ERR_CONNECTION_CLOSED);
		}
		if (encoderName == null) {
			throw new NullPointerException("Encoder name cannot be null");
		}
		this.encoder = encoderForType(encoderName);
		if (encoder==null) {
			throw new IllegalArgumentException(
					"Unrecognized encoder: " + encoderName);
		}
		
		this.nc = c;
	}
	
	public static Encoder<?> encoderForType(String encType) {
		if (encType == null) {
			throw new NullPointerException("encoder type cannot be null");
		}
		if (encType.trim().isEmpty()) {
			throw new IllegalArgumentException("encoder type cannot be empty");
		}

		encLock.lock();
		try {
			return encMap.get(encType);
		} finally {
			encLock.unlock();
		}
	}
	
	public static void registerEncoder(String encType, Encoder<?> enc) {
		if (encType == null) {
			throw new NullPointerException("encoder type cannot be null");
		}
		if (encType.trim().isEmpty()) {
			throw new IllegalArgumentException("encoder type cannot be empty");
		}
		if (enc == null) {
			throw new NullPointerException("encoder cannot be null");			
		}
		
		encLock.lock();
		try {
			encMap.put(encType, enc);
		} finally {
			encLock.unlock();
		}
	}
	
	@Override
	public void close() {
		try {
			nc.close();
		} catch (Exception e) {
			// IGNORE
		}
	}

	@Override
	public AsyncSubscription subscribe(String subject, MessageHandler cb) {
		return nc.subscribe(subject, cb);
	}

	@Override
	public AsyncSubscription subscribeAsync(String subject, MessageHandler cb) {
		return nc.subscribeAsync(subject, cb);
	}

	@Override
	public SyncSubscription subscribeSync(String subject, String queue) {
		return nc.subscribeSync(subject, queue);
	}

	@Override
	public AsyncSubscription subscribeAsync(String subject, String queue) {
		return nc.subscribeAsync(subject, queue);
	}

	@Override
	public Subscription subscribe(String subject, String queue, MessageHandler cb) {
		return nc.subscribe(subject, queue, cb);
	}

	@Override
	public AsyncSubscription subscribeAsync(String subject, String queue, MessageHandler cb) {
		return nc.subscribeAsync(subject, queue, cb);
	}

	@Override
	public AsyncSubscription subscribeAsync(String subject) {
		return nc.subscribeAsync(subject);
	}

	@Override
	public SyncSubscription subscribeSync(String subject) {
		return nc.subscribeSync(subject);
	}

	@Override
	public String newInbox() {
		return nc.newInbox();
	}

	@Override
	public boolean isClosed() {
		return nc.isClosed();
	}

	@Override
	public boolean isReconnecting() {
		return nc.isReconnecting();
	}

	@Override
	public Statistics getStats() {
		return nc.getStats();
	}

	@Override
	public void resetStats() {
		nc.resetStats();
	}

	@Override
	public long getMaxPayload() {
		return nc.getMaxPayload();
	}

	@Override
	public void flush(int timeout) throws IOException, TimeoutException, Exception {
		nc.flush(timeout);
	}

	@Override
	public void flush() throws IOException, Exception {
		nc.flush();
	}

	@Override
	public ExceptionHandler getExceptionHandler() {
		return nc.getExceptionHandler();
	}

	@Override
	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		nc.setExceptionHandler(exceptionHandler);
	}

	@Override
	public ClosedCallback getClosedCallback() {
		return nc.getClosedCallback();
	}

	@Override
	public void setClosedCallback(ClosedCallback cb) {
		nc.setClosedCallback(cb);
	}

	@Override
	public DisconnectedCallback getDisconnectedCallback() {
		return nc.getDisconnectedCallback();
	}

	@Override
	public void setDisconnectedCallback(DisconnectedCallback cb) {
		nc.setDisconnectedCallback(cb);
	}

	@Override
	public ReconnectedCallback getReconnectedCallback() {
		return nc.getReconnectedCallback();
	}

	@Override
	public void setReconnectedCallback(ReconnectedCallback cb) {
		nc.setReconnectedCallback(cb);
	}

	@Override
	public String getConnectedUrl() {
		return nc.getConnectedUrl();
	}

	@Override
	public String getConnectedServerId() {
		return nc.getConnectedServerId();
	}

	@Override
	public ConnState getState() {
		return nc.getState();
	}

	@Override
	public ServerInfo getConnectedServerInfo() {
		return nc.getConnectedServerInfo();
	}

	@Override
	public Exception getLastException() {
		return nc.getLastException();
	}

	@Override
	public
	<T> void subscribe(final String subject, final String queue, final Handler<T> handler) {
		nc.subscribe(subject, new MessageHandler() {
			public void onMessage(Message msg) {
				EncodedMessage<T> encMsg = new EncodedMessage<T>(msg, encoder);
				handler.handle(encMsg);
			}
		});
	}

	@Override
	public void subscribe(String subject, Handler<?> handler) {
		subscribe(subject, null, handler);
	}

	@Override
	public <T> void publish(String subject, T obj) {
		try {
			byte[] data = encoder.encode(subject, obj);
			if (data == null)
				throw new NullPointerException("Couldn't encode null object");
			nc.publish(subject, data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
