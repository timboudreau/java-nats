package io.nats.client.encoders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import io.nats.client.Encoder;

public class SerializableEncoder implements Encoder<Serializable> {

	@Override
	public byte[] encode(String subject, Serializable object) throws IOException {
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		byte[] res = null;

		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);

			oos.writeObject(object);
			oos.flush();

			res = baos.toByteArray();

		} catch (Exception ex) {
			throw ex;
		} finally {
			try {
				if(oos != null)
					oos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return res;
	}

	@Override
	public Serializable decode(String subject, byte[] rowObject) throws Exception{
		ObjectInputStream ois = null;
		Serializable res = null;

		try {
			ois = new ObjectInputStream(new ByteArrayInputStream(rowObject));
			res = (Serializable) ois.readObject();
		} catch (Exception ex) {
			throw ex;
		} finally {
			try {
				if(ois != null)
					ois.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return res;
	}

	public static byte[][] serializeObject(Externalizable object) throws Exception {
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		byte[][] res = new byte[2][];

		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);

			object.writeExternal(oos);
			oos.flush();

			res[0] = object.getClass().getName().getBytes();
			res[1] = baos.toByteArray();

		} catch (Exception ex) {
			throw ex;
		} finally {
			try {
				if(oos != null)
					oos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return res;
	}

	public static Externalizable deserializeObject(byte[][] rowObject) throws Exception {
		ObjectInputStream ois = null;
		String objectClassName = null;
		Externalizable res = null;

		try {

			objectClassName = new String(rowObject[0]);
			byte[] objectBytes = rowObject[1];

			ois = new ObjectInputStream(new ByteArrayInputStream(objectBytes));

			Class objectClass = Class.forName(objectClassName);
			res = (Externalizable) objectClass.newInstance();
			res.readExternal(ois);

		} catch (Exception ex) {
			throw ex;
		} finally {
			try {
				if(ois != null)
					ois.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return res;

	}


}
