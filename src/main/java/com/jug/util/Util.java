/**
 *
 */
package com.jug.util;

import net.imglib2.Point;

/**
 * @author jug
 *
 */
public class Util {
    /**
     * <p>Create a long[] with the location of of an {@link Point}.</p>
     *
     * <p>Keep in mind that creating arrays wildly is not good practice and
     * consider using the point directly.</p>
     *
     * @param point
     *
     * @return location of the point as a new long[]
     */
    final static public long[] pointLocation( final Point point )
    {
	final long[] dimensions = new long[ point.numDimensions() ];
	for ( int i=0; i < point.numDimensions(); i++ )
	    dimensions[i] = point.getLongPosition( i );
	return dimensions;
    }
}
