/**
 *
 */
package com.jug.sbmrm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author jug
 */
public class TypedJsonBytes {

	private final MessageTypes messageTypes;

	public TypedJsonBytes( final MessageTypes messageTypes ) {
		this.messageTypes = messageTypes;
	}

	public byte[] toJson( final Object obj ) {
		return toJson( obj, messageTypes );
	}

	public TypedObject fromJson( final byte[] array ) {
		return fromJson( array, messageTypes );
	}

	public static class TypedObject {

		private final int typeId;

		private final Object object;

		public TypedObject( final int typeId, final Object obj ) {
			this.typeId = typeId;
			this.object = obj;
		}

		public int type() {
			return typeId;
		}

		public Object object() {
			return object;
		}
	}

	public static byte[] toJson( final Object obj, final MessageTypes messageTypes ) {
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final OutputStreamWriter writer = new OutputStreamWriter( bos );
		bos.write( messageTypes.idForClass( obj.getClass() ) );
		gson.toJson( obj, writer );
		try {
			writer.close();
		} catch ( final IOException e ) {}
		return bos.toByteArray();
	}

	public static TypedObject fromJson( final byte[] array, final MessageTypes messageTypes ) {
		final ByteArrayInputStream bis = new ByteArrayInputStream( array );
		final int typeId = bis.read();
		final Class< ? > klass = messageTypes.classForId( typeId );
		final Gson gson = new GsonBuilder().create();
		final Object obj = gson.fromJson( new InputStreamReader( bis ), klass );
		return new TypedObject( typeId, obj );
	}
}
