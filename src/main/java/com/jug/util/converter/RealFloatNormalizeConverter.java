/**
 *
 */
package com.jug.util.converter;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Converts any {@link RealType} to a {@link FloatType} and divides by the
 * given number.
 * 
 * If the input type is complex, it loses the imaginary part without complaining
 * further.
 * 
 * @author Jug
 */
public class RealFloatNormalizeConverter< R extends RealType< R > > implements Converter< R, FloatType > {

	float num;

	public RealFloatNormalizeConverter( final float num ) {
		this.num = num;
	}

	@Override
	public void convert( final R input, final FloatType output ) {
		output.set( input.getRealFloat() / num );
	}
}
