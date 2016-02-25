/**
 *
 */
package com.jug.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import com.jug.MoMA;
import com.jug.lp.Hypothesis;

/**
 * @author jug
 *
 */
public class Util {

	/**
	 * <p>
	 * Create a long[] with the location of of an {@link Point}.
	 * </p>
	 *
	 * <p>
	 * Keep in mind that creating arrays wildly is not good practice and
	 * consider using the point directly.
	 * </p>
	 *
	 * @param point
	 *
	 * @return location of the point as a new long[]
	 */
	final static public long[] pointLocation( final Point point ) {
		final long[] dimensions = new long[ point.numDimensions() ];
		for ( int i = 0; i < point.numDimensions(); i++ )
			dimensions[ i ] = point.getLongPosition( i );
		return dimensions;
	}

	/**
	 * Creates an image containing the given component (as is on screen).
	 *
	 * @param component
	 *            the component to be captured
	 * @return a <code>BufferedImage</code> containing a screenshot of the given
	 *         component.
	 */
	public static BufferedImage getImageOf( final Component component ) {
		return getImageOf( component, component.getWidth(), component.getHeight() );
	}

	/**
	 * Creates an image containing the given component (as is on screen).
	 *
	 * @param component
	 *            the component to be captured
	 * @param width
	 * @param height
	 * @return a <code>BufferedImage</code> containing a screenshot of the given
	 *         component.
	 */
	public static BufferedImage getImageOf( final Component component, final int width, final int height ) {
		final BufferedImage image = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
		final Graphics2D graphics = image.createGraphics();
		graphics.setPaint( Color.WHITE );
		graphics.fillRect( 0, 0, image.getWidth(), image.getHeight() );
		component.paint( image.getGraphics() );
//		component.printAll( image.getGraphics() );
		return image;
	}

	/**
	 * Saves a given image in the file specified by the given
	 * <code>filename</code>.
	 *
	 * @param image
	 *            <code>BufferedImage</code> to be saved.
	 * @param filename
	 *            path to the file the image should be saved to.
	 * @throws IOException
	 */
	public static void saveImage( final BufferedImage image, String filename ) throws IOException {
		if ( !filename.endsWith( ".png" ) && !filename.endsWith( ".PNG" ) ) {
			filename += ".png";
		}
		// write the image as a PNG
		ImageIO.write( image, "png", new File( filename ) );
	}

	/**
	 * @param farray
	 * @return
	 */
	public static double[] makeDoubleArray( final float[] farray ) {
		if ( farray == null ) { return null; }
		if ( farray.length == 0 ) { return new double[] {}; }

		final double[] ret = new double[ farray.length ];
		for ( int i = 0; i < farray.length; i++ ) {
			ret[ i ] = farray[ i ];
		}
		return ret;
	}

	/**
	 * @param farray
	 * @return
	 */
	public static double[][] makeDoubleArray2d( final float[][] farray ) {
		if ( farray == null ) { return null; }
		if ( farray.length == 0 ) { return new double[][] {}; }

		final double[][] ret = new double[ farray.length ][ farray[ 0 ].length ];
		for ( int i = 0; i < farray.length; i++ ) {
			for ( int j = 0; j < farray[ 0 ].length; j++ ) {
				ret[ i ][ j ] = farray[ i ][ j ];
			}
		}
		return ret;
	}

	/**
	 *
	 * @param input
	 * @param min
	 * @param max
	 */
	public static < T extends Comparable< T > & Type< T > > void computeMinMax( final Iterable< T > input, final T min, final T max ) {
		// create a cursor for the image (the order does not matter)
		final Iterator< T > iterator = input.iterator();

		// initialize min and max with the first image value
		T type = iterator.next();

		min.set( type );
		max.set( type );

		// loop over the rest of the data and determine min and max value
		while ( iterator.hasNext() ) {
			// we need this type more than once
			type = iterator.next();

			if ( type.compareTo( min ) < 0 ) min.set( type );

			if ( type.compareTo( max ) > 0 ) max.set( type );
		}
	}

	/**
	 * @param channelFrame
	 * @param hyp
	 * @param avgXpos
	 * @return
	 */
	public static IntervalView< FloatType > getColumnBoxInImg( final IntervalView< FloatType > channelFrame, final Hypothesis< net.imglib2.algorithm.componenttree.Component< FloatType, ? >> hyp, final long glMiddleInImg ) {
		final long[] lt = Util.getTopLeftInSourceImg( hyp, glMiddleInImg );
		lt[ 0 ] = glMiddleInImg - MoMA.GL_FLUORESCENCE_COLLECTION_WIDTH_IN_PIXELS / 2;
		final long[] rb = Util.getRightBottomInSourceImg( hyp, glMiddleInImg );
		rb[ 0 ] = glMiddleInImg + MoMA.GL_FLUORESCENCE_COLLECTION_WIDTH_IN_PIXELS / 2 + MoMA.GL_FLUORESCENCE_COLLECTION_WIDTH_IN_PIXELS % 2 - 1;
		return Views.interval( Views.zeroMin( channelFrame ), lt, rb );
	}

	/**
	 * @param channelFrame
	 * @param hyp
	 * @param avgXpos
	 * @return
	 */
	public static IntervalView< FloatType > getIntensityBoxInImg( final IntervalView< FloatType > channelFrame, final Hypothesis< net.imglib2.algorithm.componenttree.Component< FloatType, ? >> hyp, final long glMiddleInImg ) {
		final long[] lt = Util.getTopLeftInSourceImg( hyp, glMiddleInImg );
		lt[ 0 ] = glMiddleInImg - MoMA.GL_WIDTH_IN_PIXELS / 2;
		final long[] rb = Util.getRightBottomInSourceImg( hyp, glMiddleInImg );
		rb[ 0 ] = glMiddleInImg + MoMA.GL_WIDTH_IN_PIXELS / 2 + MoMA.GL_WIDTH_IN_PIXELS % 2 - 1;
		return Views.interval( channelFrame, lt, rb );
	}

	/**
	 * @param segmentedFrame
	 * @param hyp
	 * @param avgXpos
	 * @return
	 */
	public static IntervalView< ShortType > getClassificationBoxInImg( final IntervalView< ShortType > segmentedFrame, final Hypothesis< net.imglib2.algorithm.componenttree.Component< FloatType, ? >> hyp, final long glMiddleInImg ) {
		final long[] lt = Util.getTopLeftInSourceImg( hyp, glMiddleInImg );
		lt[ 0 ] = glMiddleInImg - MoMA.GL_WIDTH_IN_PIXELS; // to lazy for an additional param... twice GL_WIDTH should be ok...
		final long[] rb = Util.getRightBottomInSourceImg( hyp, glMiddleInImg );
		rb[ 0 ] = glMiddleInImg + MoMA.GL_WIDTH_IN_PIXELS; // to lazy for an additional param... twice GL_WIDTH should be ok...
//		if ( false ) {
//			new ImageJ();
//			ImageJFunctions.showUnsignedShort( Views.interval( segmentedFrame, lt, rb ) );
//		}
		return Views.interval( segmentedFrame, lt, rb );
	}

	/**
	 * @param channelFrame
	 * @param hyp
	 * @return
	 */
	public static IterableInterval< FloatType > getSegmentBoxInImg( final IntervalView< FloatType > channelFrame, final Hypothesis< net.imglib2.algorithm.componenttree.Component< FloatType, ? >> hyp, final long glMiddleInImg ) {
		final long[] lt = Util.getTopLeftInSourceImg( hyp, glMiddleInImg );
		final long[] rb = Util.getRightBottomInSourceImg( hyp, glMiddleInImg );
		return Views.iterable( Views.interval( channelFrame, lt, rb ) );
	}

	/**
	 * @param channelFrame
	 * @param hyp
	 * @return
	 */
	public static long getSegmentBoxPixelCount( final Hypothesis< net.imglib2.algorithm.componenttree.Component< FloatType, ? >> hyp, final long glMiddleInImg ) {
		final long[] lt = Util.getTopLeftInSourceImg( hyp, glMiddleInImg );
		final long[] rb = Util.getRightBottomInSourceImg( hyp, glMiddleInImg );
		return ( rb[ 0 ] - lt[ 0 ] + 1 ) * ( rb[ 1 ] - lt[ 1 ] + 1 );
	}

	/**
	 * @param hyp
	 * @return
	 */
	private static long[] getTopLeftInSourceImg( final Hypothesis< net.imglib2.algorithm.componenttree.Component< FloatType, ? >> hyp, final long middle ) {
		final ValuePair< Integer, Integer > limits = hyp.getLocation();
		final long left = middle - MoMA.GL_WIDTH_IN_PIXELS / 2;
		final long top = limits.getA() + MoMA.GL_OFFSET_TOP;;
		return new long[] { left, top };
	}

	/**
	 * @param hyp
	 * @return
	 */
	private static long[] getRightBottomInSourceImg( final Hypothesis< net.imglib2.algorithm.componenttree.Component< FloatType, ? >> hyp, final long middle ) {
		final ValuePair< Integer, Integer > limits = hyp.getLocation();
		final long right = middle + MoMA.GL_WIDTH_IN_PIXELS / 2 + MoMA.GL_WIDTH_IN_PIXELS % 2 - 1;
		final long bottom = limits.getB() + MoMA.GL_OFFSET_TOP;
		return new long[] { right, bottom };
	}

	/**
	 * @param sizeEstimationBoxInChannel0
	 * @param d
	 * @return
	 */
	public static int countPixelsAboveThreshold( final IntervalView< ShortType > segmentedFrame, final float threshold ) {
		int ret = 0;
		for ( final ShortType pixel : Views.iterable( segmentedFrame ) ) {
			if ( pixel.get() > threshold ) ret++;
		}
		return ret;
	}

	/**
	 * Returns the summed length of straight line sements connectiong the points
	 * in the given list (in the listed order).
	 *
	 * @param centerLine
	 * @param startIndex
	 * @param endIndex
	 */
	public static double evaluatePolygonLength( final List< Point > centerLine, Integer startIndex, Integer endIndex ) {
		if ( startIndex <= 0 ) startIndex = 0;
		if ( endIndex >= centerLine.size() ) endIndex = centerLine.size() - 1;
		if ( centerLine.size() < 2 ) return 0.;

		double length = 0.;
		for ( int i = startIndex; i < endIndex; i++ ) {
			final Point p1 = centerLine.get( i );
			final Point p2 = centerLine.get( i + 1 );
			length += getEuclidianDistance( p1, p2 );
		}
		return length;
	}

	/**
	 * @param p1
	 * @param p2
	 * @return
	 */
	private static double getEuclidianDistance( final Point p1, final Point p2 ) {
		if ( p1.numDimensions() != p2.numDimensions() )
			throw new RuntimeException( "Euclidian distance cannot be computed between points of unequal dimensionality!" );

		double sumOfSquares = 0.;
		for ( int d = 0; d < p1.numDimensions(); d++ ) {
			final double dimdist = Math.abs( p1.getDoublePosition( d ) - p2.getDoublePosition( d ) );
			sumOfSquares += dimdist * dimdist;
		}
		return Math.sqrt( sumOfSquares );
	}

}
