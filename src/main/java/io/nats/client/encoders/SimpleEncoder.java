package io.nats.client.encoders;

import io.nats.client.Constants;
import io.nats.client.Encoder;

public class SimpleEncoder<T> implements Encoder<T> {

	final static String name = Constants.DEFAULT_ENCODER; 
	T typeObj = null;
	
	@Override
	public byte[] encode(String subject, T obj) throws Exception {
		byte[] res = null;
		
		this.typeObj = obj;
		
		if (obj instanceof byte[]) {
			res = (byte[]) obj;
		} else if (obj instanceof String) {
			res = ((String) obj).getBytes();
		} else if (obj instanceof Number) {
			res = String.valueOf(obj).getBytes();
		} else if (obj instanceof Boolean) {
			res = String.valueOf(obj).getBytes();
		}
		else {
			throw new IllegalArgumentException("Don't know how to encode " +
					obj.getClass().getName());
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T decode(String subject, byte[] data) throws Exception {
		T res = null;
		
		if (typeObj instanceof byte[]) {
			res = (T)data;
		} else if (typeObj instanceof String) {
			res = (T) new String(data);
		} else if (typeObj instanceof Number) {
			if (typeObj instanceof Float)
				res = (T)Float.valueOf(new String(data));
			if (typeObj instanceof Double)
				res = (T)Double.valueOf(new String(data));
			if (typeObj instanceof Integer)
				res = (T)Integer.valueOf(new String(data));
			if (typeObj instanceof Long)
				res = (T)Long.valueOf(new String(data));
			if (typeObj instanceof Short)
				res = (T)Short.valueOf(new String(data));
		} else if (typeObj instanceof Boolean) {
			res = (T)Boolean.valueOf(new String(data));
		} else {
			System.err.println("Don't know how to decode " + typeObj.getClass().getName());
		}
		
		return res;	
	}

}
