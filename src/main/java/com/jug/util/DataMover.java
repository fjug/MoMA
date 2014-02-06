/**
 *
 */
package com.jug.util;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

/**
 * @author jug
 *
 */
public class DataMover {

	/**
	 * FROM: imglib Example 2c :)
	 * Copy from a source that is just
	 * RandomAccessible to an IterableInterval. Latter one defines size and
	 * location of the copy operation. It will query the same pixel locations of
	 * the IterableInterval in the RandomAccessible. It is up to the developer
	 * to ensure that these coordinates match.
	 * 
	 * Note that both, input and output could be Views, Img or anything that
	 * implements those interfaces.
	 * 
	 * @param source
	 *            - a RandomAccess as source that can be infinite
	 * @param target
	 *            - an IterableInterval as target
	 */
	public static < T extends Type< T >> void copy( final RandomAccessible< T > source, final IterableInterval< T > target ) {
		// create a cursor that automatically localizes itself on every move
		final Cursor< T > targetCursor = target.localizingCursor();
		final RandomAccess< T > sourceRandomAccess = source.randomAccess();

		// iterate over the input cursor
		while ( targetCursor.hasNext() ) {
			// move input cursor forward
			targetCursor.fwd();

			// set the output cursor to the position of the input cursor
			sourceRandomAccess.setPosition( targetCursor );

			// set the value of this pixel of the output image, every Type
			// supports T.set( T type )
			targetCursor.get().set( sourceRandomAccess.get() );
		}
	}

	public static < T extends Type< T >> void copy( final RandomAccessible< T > source, final RandomAccessibleInterval< T > target ) {
		copy( source, Views.iterable( target ) );
	}

	public static < T extends NativeType< T >> Img< T > createEmptyArrayImgLike( final RandomAccessibleInterval< ? > blueprint, final T type ) {
		final long[] dims = new long[ blueprint.numDimensions() ];
		for ( int i = 0; i < blueprint.numDimensions(); i++ ) {
			dims[ i ] = blueprint.dimension( i );
		}
		final Img< T > ret = new ArrayImgFactory< T >().create( dims, type );
		return ret;
	}

	/**
	 * Another hacky util function that copies one image into another.
	 * I cases where the native pixel types match, <code>convertAndCopy</code>
	 * will simply call <code>copy</code>.
	 * 
	 * <b>General assumptions and rules:</b> <li>User takes care that
	 * <code>source</code> is defines on all interval locations of
	 * <code>target</code>. (The source <i>exists</i> on all places the target
	 * has to be filled.)
	 * 
	 * <b>Supported conversions are:</b> <li>From <code>DoubleType</code>... <li>
	 * ...to <code>ARGBType</code>; source range is expected to be subset or
	 * equal to <code>[0,1]</code>. If values <0 or >1 are found the source
	 * image will be scanned for maximal and minimal values and the converted
	 * image will be normalized, filling the entire target range
	 * <code>[0,255]</code> in all 3 color channels. <li>From
	 * <code>ARGBType</code>... <li>...to <code>DoubleType</code>; conversion
	 * ignores A, and computed double value like this:
	 * <code>v = ( 0.2989R + 0.5870G + 0.1140B ) / 255</code>, which leads to a
	 * target <code>Img</code> that lies within <code>[0,1]</code>.
	 * 
	 * @param source
	 * @param target
	 * @throws Exception
	 */
//    public static <ST extends NativeType<ST>, TT extends NativeType<TT>> void convertAndCopy(final Img<ST> source, final Img<TT> target) throws Exception {
//	convertAndCopy( source, Views.iterable(target) );
//    }
//
//    public static <ST extends NativeType<ST>, TT extends NativeType<TT>> void convertAndCopy(final RandomAccessible<ST> source, final RandomAccessibleInterval<TT> target) throws Exception {
//	convertAndCopy( source, Views.iterable(target) );
//    }

	@SuppressWarnings( "unchecked" )
	public static < ST extends NativeType< ST >, TT extends NativeType< TT >> void convertAndCopy( final RandomAccessible< ST > source, final IterableInterval< TT > target ) throws Exception {
		final ST sourceType = source.randomAccess().get();
		final TT targetType = target.firstElement();

		// if source and target are of same type -> use copy since convert is not needed...
		if ( sourceType.getClass().isInstance( targetType ) ) {
			DataMover.copy( source, ( IterableInterval< ST > ) target );
		}

		// implemented conversion cases follow here...

		boolean throwException = false;
		if ( sourceType instanceof DoubleType ) {

			// DoubleType --> ARGBType
			if ( targetType instanceof ARGBType ) {
				final Cursor< TT > targetCursor = target.localizingCursor();
				final RandomAccess< ST > sourceRandomAccess = source.randomAccess();
				int v;
				while ( targetCursor.hasNext() ) {
					targetCursor.fwd();
					sourceRandomAccess.setPosition( targetCursor );
					try {
						v = ( int ) Math.round( ( ( DoubleType ) sourceRandomAccess.get() ).get() * 255 );
					}
					catch ( final ArrayIndexOutOfBoundsException e ) {
						v = 255; // If image-sizes do not match we pad with white pixels...
					}
					if ( v > 255 ) { throw new Exception( "TODO: in this case (source in not within [0,1]) I did not finish the code!!! Now would likely be a good time... ;)" ); }
					( ( ARGBType ) targetCursor.get() ).set( ARGBType.rgba( v, v, v, 255 ) );
				}
			} else {
				throwException = true;
			}
		} else if ( sourceType instanceof ARGBType ) {

			// ARGBType --> DoubleType
			if ( targetType instanceof ARGBType ) {
				final Cursor< TT > targetCursor = target.localizingCursor();
				final RandomAccess< ST > sourceRandomAccess = source.randomAccess();
				double v;
				int intRGB;
				while ( targetCursor.hasNext() ) {
					targetCursor.fwd();
					sourceRandomAccess.setPosition( targetCursor );
					intRGB = ( ( ARGBType ) sourceRandomAccess.get() ).get();
					v = 0.2989 * ARGBType.red( intRGB ) + 0.5870 * ARGBType.green( intRGB ) + 0.1140 * ARGBType.blue( intRGB );
					v /= 255;
					( ( ARGBType ) targetCursor.get() ).set( ARGBType.rgba( v, v, v, 255 ) );
				}
			} else {
				throwException = true;
			}
		} else {
			throwException = true;
		}

		if ( throwException )
			throw new Exception( "Convertion between the given NativeTypes not implemented!" );
	}
}
