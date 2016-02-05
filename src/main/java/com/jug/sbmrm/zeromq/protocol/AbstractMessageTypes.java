/**
 *
 */
package com.jug.sbmrm.zeromq.protocol;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMessageTypes implements MessageTypes {

	private final Map< Integer, Class< ? > > idToClass;

	private final Map< Class< ? >, Integer > classToId;

	public AbstractMessageTypes() {
		idToClass = new HashMap< Integer, Class< ? > >();
		classToId = new HashMap< Class< ? >, Integer >();
	}

	protected void put( final int id, final Class< ? > klass )
	{
		idToClass.put( id, klass );
		classToId.put( klass, id );
	}

	@Override
	public Class< ? > classForId( final int id ) {
		return idToClass.get( id );
	}

	@Override
	public int idForClass( final Class< ? > klass ) {
		return classToId.get( klass );
	}
}