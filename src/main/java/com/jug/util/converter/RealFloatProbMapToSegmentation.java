/**
 *
 */
package com.jug.util.converter;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;

/**
 * Converts any {@link RealType} to a {@link ShortType} by thresholding and
 * putting 0 or 1.
 * 
 * If the input type is complex, it loses the imaginary part without complaining
 * any further.
 * 
 * @author Jug
 */
public class RealFloatProbMapToSegmentation< R extends RealType< R > > implements Converter< R, ShortType > {

	float threshold;

	public RealFloatProbMapToSegmentation( final float threshold ) {
		this.threshold = threshold;
	}

	@Override
	public void convert( final R input, final ShortType output ) {
		output.set( ( short ) ( ( 1f - input.getRealFloat() > threshold ) ? 1 : 0 ) );
	}
}
