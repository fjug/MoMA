/**
 *
 */
package com.jug.util;


/**
 * @author jug
 */
public class MyUnnormalizedGaussian {

	private final double height;
	private final double mean;
	private final double v2;

	public MyUnnormalizedGaussian( final double height, final double mean, final double sd ) {
		this.height = height;
		this.mean = mean;
		this.v2 = sd * sd;
	}

	public double value( final double x ) {
		final double d = x - mean;
		return height * Math.exp( -( d * d ) / ( 2.0 * v2 ) );
	}
}
