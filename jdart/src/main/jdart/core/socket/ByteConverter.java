package jdart.core.socket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class ByteConverter {

	public static byte[] convertToBytes(Object object) throws IOException {
		byte[] bytes;
		ByteArrayOutputStream bos = null;
		ObjectOutput out = null;
		try{
			bos = new ByteArrayOutputStream();
			out = new ObjectOutputStream(bos);
			out.writeObject(object);
			bytes = bos.toByteArray();
			System.out.println("send byte[] : "+bytes.length);
		}finally{
			bos.close();
			out.close();
		}
	    return bytes;
	}
	

	public static Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
		System.out.println("receive byte[] : "+bytes.length);
		Object object;
		ByteArrayInputStream bis = null;
        ObjectInput in = null;
		try{
			bis = new ByteArrayInputStream(bytes);
	        in = new ObjectInputStream(bis);
	        object = in.readObject();
		}finally{
			bis.close();
			in.close();
		}
        return object;
	}

}
