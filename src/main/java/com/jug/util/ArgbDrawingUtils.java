/**
 *
 */
package com.jug.util;

import java.util.Iterator;

import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class ArgbDrawingUtils {

	/**
	 * @param ctn
	 * @param raAnnotationImg
	 * @param offsetX
	 * @param offsetY
	 */
	public static void taintComponentTreeNode( final Component< DoubleType, ? > ctn, final RandomAccess< ARGBType > raArgbImg, final long offsetX, final long offsetY ) {
		assert ( ctn.iterator().hasNext() );

		switch ( ctn.iterator().next().numDimensions() ) {
		case 1:
			taint1dComponentTreeNode( ctn, raArgbImg, offsetX, offsetY );
			break;
		default:
			new Exception( "Given dimensionality is not supported by this function!" ).printStackTrace();
		}
	}

	/**
	 * @param ctn
	 * @param raArgbImg
	 * @param offsetX
	 * @param offsetY
	 */
	private static void taint1dComponentTreeNode( final Component< DoubleType, ? > ctn, final RandomAccess< ARGBType > raArgbImg, final long offsetX, final long offsetY ) {

		final Iterator< Localizable > componentIterator = ctn.iterator();
		while ( componentIterator.hasNext() ) {
			final int ypos = componentIterator.next().getIntPosition( 0 );
			final Point p = new Point( offsetX, offsetY + ypos );
			final int delta = 15;
			for ( int i = -delta; i <= delta; i++ ) {
				final long[] imgPos = Util.pointLocation( p );
				imgPos[ 0 ] += i;
				raArgbImg.setPosition( imgPos );
				final int curCol = raArgbImg.get().get();
				final int redToLoose = 0;
				final int greenToUse = Math.min( 75, ( 255 - ARGBType.green( curCol ) ) ) / 1 ;
				final int blueToUse  = Math.min( 75, ( 255 - ARGBType.blue(  curCol ) ) ) / 2;
				raArgbImg.get().set( new ARGBType( ARGBType.rgba( ARGBType.red( curCol ) - ( redToLoose * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ),
																  ARGBType.green( curCol ) + ( greenToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ),
																  ARGBType.blue( curCol ) + ( blueToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ),
																  ARGBType.alpha( curCol ) ) ) );
			}
		}

	}

}
