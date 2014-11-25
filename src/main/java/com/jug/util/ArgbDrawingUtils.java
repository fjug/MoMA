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
import net.imglib2.type.numeric.real.FloatType;

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
	public static void taintComponentTreeNode( final Component< FloatType, ? > ctn, final RandomAccess< ARGBType > raArgbImg, final long offsetX, final long offsetY ) {
		assert ( ctn.iterator().hasNext() );

		switch ( ctn.iterator().next().numDimensions() ) {
		case 1:
			taint1dComponentTreeNodeFaintGreen( ctn, raArgbImg, offsetX, offsetY );
			break;
		default:
			new Exception( "Given dimensionality is not supported by this function!" ).printStackTrace();
		}
	}

	/**
	 * @param ctn
	 * @param raAnnotationImg
	 * @param offsetX
	 * @param offsetY
	 */
	public static void taintForcedComponentTreeNode( final Component< FloatType, ? > ctn, final RandomAccess< ARGBType > raArgbImg, final long offsetX, final long offsetY ) {
		assert ( ctn.iterator().hasNext() );

		switch ( ctn.iterator().next().numDimensions() ) {
		case 1:
			taint1dComponentTreeNodeYellow( ctn, raArgbImg, offsetX, offsetY );
			break;
		default:
			new Exception( "Given dimensionality is not supported by this function!" ).printStackTrace();
		}
	}

	/**
	 * @param ctn
	 * @param raAnnotationImg
	 * @param offsetX
	 * @param offsetY
	 */
	public static void taintInactiveComponentTreeNode( final Component< FloatType, ? > ctn, final RandomAccess< ARGBType > raArgbImg, final long offsetX, final long offsetY ) {
		assert ( ctn.iterator().hasNext() );

		switch ( ctn.iterator().next().numDimensions() ) {
		case 1:
			taint1dComponentTreeNodeRed( ctn, raArgbImg, offsetX, offsetY );
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
	private static void taint1dComponentTreeNodeFaintGreen( final Component< FloatType, ? > ctn, final RandomAccess< ARGBType > raArgbImg, final long offsetX, final long offsetY ) {

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
				final int redToUse = ( int ) ( Math.min( 10, ( 255 - ARGBType.red( curCol ) ) ) / 1.25 );
				final int greenToUse = Math.min( 35, ( 255 - ARGBType.green( curCol ) ) ) / 1;
				final int blueToUse = ( int ) ( Math.min( 10, ( 255 - ARGBType.blue( curCol ) ) ) / 1.25 );
				raArgbImg.get().set( new ARGBType( ARGBType.rgba( ARGBType.red( curCol ) + ( redToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.green( curCol ) + ( greenToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.blue( curCol ) + ( blueToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.alpha( curCol ) ) ) );
			}
		}

	}

	/**
	 * @param ctn
	 * @param raArgbImg
	 * @param offsetX
	 * @param offsetY
	 */
	private static void taint1dComponentTreeNodeGreen( final Component< FloatType, ? > ctn, final RandomAccess< ARGBType > raArgbImg, final long offsetX, final long offsetY ) {

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
				final int redToUse = Math.min( 10, ( 255 - ARGBType.red( curCol ) ) ) / 4;
				final int greenToUse = Math.min( 100, ( 255 - ARGBType.green( curCol ) ) ) / 1;
				final int blueToUse = Math.min( 10, ( 255 - ARGBType.blue( curCol ) ) ) / 4;
				raArgbImg.get().set( new ARGBType( ARGBType.rgba( ARGBType.red( curCol ) + ( redToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.green( curCol ) + ( greenToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.blue( curCol ) + ( blueToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.alpha( curCol ) ) ) );
			}
		}

	}

	/**
	 * @param ctn
	 * @param raArgbImg
	 * @param offsetX
	 * @param offsetY
	 */
	private static void taint1dComponentTreeNodeRed( final Component< FloatType, ? > ctn, final RandomAccess< ARGBType > raArgbImg, final long offsetX, final long offsetY ) {

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
				final int redToUse = Math.min( 100, ( 255 - ARGBType.red( curCol ) ) ) / 1;;
				final int greenToUse = Math.min( 10, ( 255 - ARGBType.green( curCol ) ) ) / 4;
				final int blueToUse = Math.min( 10, ( 255 - ARGBType.blue( curCol ) ) ) / 4;
				raArgbImg.get().set( new ARGBType( ARGBType.rgba( ARGBType.red( curCol ) + ( redToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.green( curCol ) + ( greenToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.blue( curCol ) + ( blueToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.alpha( curCol ) ) ) );
			}
		}

	}

	/**
	 * @param ctn
	 * @param raArgbImg
	 * @param offsetX
	 * @param offsetY
	 */
	private static void taint1dComponentTreeNodeYellow( final Component< FloatType, ? > ctn, final RandomAccess< ARGBType > raArgbImg, final long offsetX, final long offsetY ) {

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
				final int redToUse = Math.min( 100, ( 255 - ARGBType.red( curCol ) ) ) / 1;
				final int greenToUse = ( int ) ( Math.min( 75, ( 255 - ARGBType.green( curCol ) ) ) / 1.25 );
				final int blueToUse = Math.min( 10, ( 255 - ARGBType.blue( curCol ) ) ) / 4;
				raArgbImg.get().set( new ARGBType( ARGBType.rgba( ARGBType.red( curCol ) + ( redToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.green( curCol ) + ( greenToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.blue( curCol ) + ( blueToUse * ( ( float ) ( delta - Math.abs( i ) ) / delta ) ), ARGBType.alpha( curCol ) ) ) );
			}
		}

	}

}
