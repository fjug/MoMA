/**
 *
 */
package com.jug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import com.jug.util.ArgbDrawingUtils;
import com.jug.util.SimpleFunctionAnalysis;
import com.jug.util.Util;
import com.jug.util.filteredcomponents.FilteredComponent;

/**
 * @author jug
 *         Represents one growth line (well) in which Bacteria can grow, at one
 *         instance in time.
 *         This corresponds to one growth line micrograph. The class
 *         representing an entire time
 *         series (2d+t) representation of an growth line is
 *         <code>GrowthLine</code>.
 */
public abstract class AbstractGrowthLineFrame< C extends Component< DoubleType, C > > {

	private class CompareComponentTreeNodes< T extends Type< T > > implements Comparator< Component< T, ? > > {

		/**
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public int compare( final Component< T, ? > o1, final Component< T, ? > o2 ) {

			// FilteredComponent
			if ( false || o1 instanceof FilteredComponent< ? > && o2 instanceof FilteredComponent< ? > ) { // igitt...
																											// instanceof!
				final FilteredComponent< DoubleType > fc1 = ( FilteredComponent< DoubleType > ) o1;
				final FilteredComponent< DoubleType > fc2 = ( FilteredComponent< DoubleType > ) o2;
				return ( int ) ( ( fc1.maxValue().get() - fc1.minValue().get() ) - fc2.maxValue().get() - fc2.minValue().get() ) * -1;
			}

			// MSER
			// if ( false || o1 instanceof MserComponentTreeNode< ? > && o2
			// instanceof MserComponentTreeNode< ? >) { // igitt... instanceof!
			// final MserComponentTreeNode<DoubleType> mser1 =
			// (MserComponentTreeNode<DoubleType>) o1;
			// final MserComponentTreeNode<DoubleType> mser2 =
			// (MserComponentTreeNode<DoubleType>) o2;
			// return (int) ( (mser1.maxValue().get()-mser1.minValue().get()) -
			// mser2.maxValue().get()-mser2.minValue().get() ) * -1;
			// }

			// System.out.println("Warning: defaultComparison of ComponentTreeNodes applied!");
			return ( int ) ( o1.size() - o2.size() ) * -1;
		}
	}

	// -------------------------------------------------------------------------------------
	// private fields
	// -------------------------------------------------------------------------------------
	/**
	 * Points at all the detected GrowthLine centers associated with this
	 * GrowthLine.
	 */
	private List< Point > imgLocations;
	private double[] sepValues; // lazy evaluation -- gets computed when
								// getGapSeparationValues is called...
	private GrowthLine parent;
	private ComponentForest< C > componentTree;

	// -------------------------------------------------------------------------------------
	// setters and getters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the location
	 */
	public List< Point > getImgLocations() {
		return imgLocations;
	}

	/**
	 * @return the location
	 */
	public List< Point > getMirroredImgLocations() {
		return flipAtCenter( imgLocations );
	}

	/**
	 * @param location
	 *            the location to set
	 */
	public void setImgLocations( final List< Point > locations ) {
		this.imgLocations = locations;
	}

	/**
	 * @return the growth line time series this one growth line is part of.
	 */
	public GrowthLine getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            - the growth line time series this one growth line is part of.
	 */
	public void setParent( final GrowthLine parent ) {
		this.parent = parent;
	}

	/**
	 * @return the componentTree
	 */
	public ComponentForest< C > getComponentTree() {
		return componentTree;
	}

	/**
	 * @return the x-offset of the GrowthLineFrame given the original micrograph
	 */
	public long getOffsetX() {
//		return getAvgXpos();
		return getPoint( 0 ).getLongPosition( 0 );
	}

	/**
	 * @return the y-offset of the GrowthLineFrame given the original micrograph
	 */
	public long getOffsetY() {
		return 0;
	}

	/**
	 * @return the z-offset of the GrowthLineFrame given the original micrograph
	 *         (stack)
	 */
	public long getOffsetZ() {
		return parent.getFrames().indexOf( this );
	}

	// -------------------------------------------------------------------------------------
	// constructors
	// -------------------------------------------------------------------------------------
	public AbstractGrowthLineFrame() {
		imgLocations = new ArrayList< Point >();
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * @return the number of points (the length) of this GrowthLine
	 */
	public int size() {
		return imgLocations.size();
	}

	/**
	 * Reads out all image intensities along the GrowthLine center.
	 *
	 * @param img
	 *            - an Img.
	 * @return a double array containing the image intensities in
	 *         <code>img</code> at the points given in wellPoints.
	 */
	private double[] getInvertedIntensities( final Img< DoubleType > img ) {
		final double[] ret = new double[ imgLocations.size() ];
		final RandomAccess< DoubleType > ra = img.randomAccess();
		int i = 0;
		for ( final Point p : imgLocations ) {
			ra.setPosition( p );
			ret[ i++ ] = 1.0 - ra.get().get();
		}
		return ret;
	}

	/**
	 * Adds a detected center point to a GrowthsLineFrame.
	 *
	 * @param point
	 */
	public void addPoint( final Point point ) {
		imgLocations.add( point );
	}

	/**
	 * Gets a detected center point of a GrowthsLine.
	 *
	 * @param idx
	 *            - index of the Point to be returned.
	 */
	public Point getPoint( final int idx ) {
		return ( imgLocations.get( idx ) );
	}

	/**
	 * Gets the first detected center point of a GrowthsLine.
	 */
	public Point getFirstPoint() {
		return ( imgLocations.get( 0 ) );
	}

	/**
	 * Gets the last detected center point of a GrowthsLine.
	 */
	public Point getLastPoint() {
		return ( imgLocations.get( imgLocations.size() - 1 ) );
	}

	/**
	 * Using the imglib2 component tree to find the most stable components
	 * (bacteria).
	 *
	 * @param img
	 */
	public void generateSegmentationHypotheses( final Img< DoubleType > img ) {

		final double[] fkt = getGapSeparationValues( img );

		if ( fkt.length > 0 ) {
			final RandomAccessibleInterval< DoubleType > raiFkt = new ArrayImgFactory< DoubleType >().create( new int[] { fkt.length }, new DoubleType() );
			final RandomAccess< DoubleType > ra = raiFkt.randomAccess();
			for ( int i = 0; i < fkt.length; i++ ) {
				ra.setPosition( i, 0 );
				ra.get().set( fkt[ i ] );
			}

			componentTree = buildTree( raiFkt );
//			componentTree = FilteredComponentTree.buildComponentTree( raiFkt, new DoubleType(), 3, Long.MAX_VALUE, true );
//			componentTree = MserComponentTree.buildMserTree( raiFkt, MotherMachine.MIN_GAP_CONTRAST / 2.0, MotherMachine.MIN_CELL_LENGTH, Long.MAX_VALUE, 0.5, 0.33, true );
		}
	}

	protected abstract ComponentForest< C > buildTree( final RandomAccessibleInterval< DoubleType > raiFkt );

	/**
	 * So far this function is pretty stupid: I traverse the entire component
	 * tree, put each node in a priority queue, take then the numComponents
	 * first elements out of it, put them in a ArrayList and give them back to
	 * the caller. Efficiency: O(turbo-puke) !!
	 *
	 * @param numComponents
	 * @return null, if there are less components in the tree then the callee
	 *         requested
	 */
	public List< Component< DoubleType, ? > > getComponentHypothesis( final int numComponents ) {
		if ( componentTree == null ) return null;

		final PriorityQueue< Component< DoubleType, ? > > queue = new PriorityQueue< Component< DoubleType, ? > >( 1, new CompareComponentTreeNodes< DoubleType >() );
		final List< Component< DoubleType, ? > > listNext = new LinkedList< Component< DoubleType, ? > >();
		queue.addAll( componentTree.roots() );
		listNext.addAll( componentTree.roots() );

		// go down the tree until you find the right amount of components
		// return from within loop, on failure break and return null
		while ( !listNext.isEmpty() ) {
			final Component< DoubleType, ? > node = listNext.remove( 0 );
			listNext.addAll( node.getChildren() );
			queue.addAll( node.getChildren() );
		}

		if ( queue.size() < numComponents ) { return null; }

		final ArrayList< Component< DoubleType, ? > > ret = new ArrayList< Component< DoubleType, ? > >( numComponents );
		for ( int i = 0; i < numComponents; i++ ) {
			ret.add( queue.poll() );
		}
		return ret;
	}

	/**
	 * @param img
	 * @param wellPoints
	 * @return
	 */
	public double[] getCenterLineValues( final Img< DoubleType > img ) {
		final RandomAccess< DoubleType > raImg = img.randomAccess();

		final double[] dIntensity = new double[ imgLocations.size() ];
		for ( int i = 0; i < imgLocations.size(); i++ ) {
			raImg.setPosition( imgLocations.get( i ) );
			dIntensity[ i ] = raImg.get().get();
		}
		return dIntensity;
	}

	/**
	 * @param img
	 * @param wellPoints
	 * @return
	 */
	public double[] getMirroredCenterLineValues( final Img< DoubleType > img ) {
		final RandomAccess< DoubleType > raImg = img.randomAccess();
		final List< Point > mirroredImgLocations = getMirroredImgLocations();
		final double[] dIntensity = new double[ mirroredImgLocations.size() ];
		for ( int i = 0; i < mirroredImgLocations.size(); i++ ) {
			raImg.setPosition( mirroredImgLocations.get( i ) );
			dIntensity[ i ] = raImg.get().get();
		}
		return dIntensity;
	}

	/**
	 * Trying to look there a bit smarter... ;)
	 *
	 * @param img
	 * @param wellPoints
	 * @return
	 */
	public double[] getGapSeparationValues( final Img< DoubleType > img ) {
		return getGapSeparationValues( img, false );
	}

	public double[] getGapSeparationValues( final Img< DoubleType > img, final boolean forceRecomputation ) {
		if ( sepValues == null ) {
			if ( img == null ) return null;
//			sepValues = getMaxTiltedLineAveragesInRectangleAlongAvgCenter( img );
			sepValues = getInvertedIntensities( img );
		}
		return sepValues;
	}

	/**
	 * Trying to look there a bit smarter... ;)
	 *
	 * @param img
	 * @param wellPoints
	 * @return
	 */
	@SuppressWarnings( "unused" )
	private double[] getMaxTiltedLineAveragesInRectangleAlongAvgCenter( final Img< DoubleType > img ) {
		// special case: growth line does not exist in this frame
		if ( imgLocations.size() == 0 ) return new double[ 0 ];

		final int centerX = getAvgXpos();
		final int centerZ = imgLocations.get( 0 ).getIntPosition( 2 );
		final int maxOffsetX = 9;
		final int maxOffsetY = 9;

		final RealRandomAccessible< DoubleType > rrImg = Views.interpolate( Views.hyperSlice( img, 2, centerZ ), new NLinearInterpolatorFactory< DoubleType >() );
		final RealRandomAccess< DoubleType > rraImg = rrImg.realRandomAccess();

		final double[] dIntensity = new double[ imgLocations.size() ]; //  + 1
		for ( int i = 0; i < imgLocations.size(); i++ ) {
			final int centerY = imgLocations.get( i ).getIntPosition( 1 );

			int nextAverageIdx = 0;
			final double[] diagonalAverages = new double[ maxOffsetY * 2 + 1 ];
			for ( int currentOffsetY = -maxOffsetY; currentOffsetY <= maxOffsetY; currentOffsetY++ ) {
				double summedIntensities = 0;
				int summands = 0;
				for ( int currentOffsetX = -maxOffsetX; currentOffsetX <= maxOffsetX; currentOffsetX++ ) {
					final double x = centerX + currentOffsetX;
					final double y = centerY + ( ( double ) currentOffsetY / maxOffsetX ) * currentOffsetX;
					rraImg.setPosition( new double[] { x, y } );
					summedIntensities += rraImg.get().get();
					summands++;
				}
				diagonalAverages[ nextAverageIdx ] = summedIntensities / summands;
				nextAverageIdx++;
			}
			final double maxDiagonalAvg = SimpleFunctionAnalysis.getMax( diagonalAverages ).b.doubleValue();

			// dIntensity[i] = maxDiagonalAvg - totalAverageIntensity;
			// dIntensity[i] = maxDiagonalAvg - minIntensity;
			dIntensity[ i ] = maxDiagonalAvg;
		}

//		dIntensity = SimpleFunctionAnalysis.normalizeDoubleArray( dIntensity, 0.0, 1.0 );

		return dIntensity;
	}

	/**
	 * Draws the GrowthLine center line into the given annotation
	 * <code>Img</code>.
	 *
	 * @param img
	 *            the Img to draw into.
	 */
	public void drawCenterLine( final Img< ARGBType > imgAnnotated ) {
		drawCenterLine( imgAnnotated, null );
	}

	/**
	 * Draws the GrowthLine center line into the given annotation
	 * <code>Img</code>.
	 *
	 * @param img
	 *            the Img to draw into.
	 * @param view
	 *            the active view on that Img (in order to know the pixel
	 *            offsets)
	 */
	public void drawCenterLine( final Img< ARGBType > img, final IntervalView< DoubleType > view ) {
		final RandomAccess< ARGBType > raAnnotationImg = img.randomAccess();

		long offsetX = 0;
		long offsetY = 0;
		if ( view != null ) {
			offsetX = view.min( 0 );
			offsetY = view.min( 1 );
		}

		for ( final Point p : getMirroredImgLocations() ) { // imgLocations
			final long[] pos = Util.pointLocation( p );
			pos[ 0 ] += offsetX;
			pos[ 1 ] += offsetY;
			raAnnotationImg.setPosition( pos );
			raAnnotationImg.get().set( new ARGBType( ARGBType.rgba( 0, 255, 0, 255 ) ) );
		}
	}

	/**
	 * Draws the optimal segmentation (determined by the solved ILP) into the
	 * given <code>Img</code>.
	 *
	 * @param img
	 *            the Img to draw into.
	 * @param optimalSegmentation
	 *            a <code>List</code> of the component-tree-nodes that represent
	 *            the optimal segmentation (the one returned by the solution to
	 *            the ILP).
	 */
	public void drawOptimalSegmentation( final Img< ARGBType > img, final List< Component< DoubleType, ? >> optimalSegmentation ) {
		drawOptimalSegmentation( img, null, optimalSegmentation );
	}

	/**
	 * Draws the optimal segmentation (determined by the solved ILP) into the
	 * given <code>Img</code>.
	 *
	 * @param img
	 *            the Img to draw into.
	 * @param view
	 *            the active view on that Img (in order to know the pixel
	 *            offsets)
	 * @param optimalSegmentation
	 *            a <code>List</code> of the component-tree-nodes that represent
	 *            the optimal segmentation (the one returned by the solution to
	 *            the ILP).
	 */
	public void drawOptimalSegmentation( final Img< ARGBType > img, final IntervalView< DoubleType > view, final List< Component< DoubleType, ? >> optimalSegmentation ) {
		final RandomAccess< ARGBType > raAnnotationImg = img.randomAccess();

		long offsetX = 0;
		long offsetY = 0;
		if ( view != null ) {
			offsetX = view.min( 0 );
			offsetY = view.min( 1 );
		}

		for ( final Component< DoubleType, ? > ctn : optimalSegmentation ) {
			ArgbDrawingUtils.taintComponentTreeNode( ctn, raAnnotationImg, offsetX + getAvgXpos(), offsetY + MotherMachine.GL_OFFSET_TOP );
		}
	}

	/**
	 * @return the average X coordinate of the center line of this
	 *         <code>GrowthLine</code>
	 */
	public int getAvgXpos() {
		int avg = 0;
		for ( final Point p : imgLocations ) {
			avg += p.getIntPosition( 0 );
		}
		if ( imgLocations.size() == 0 ) { return -1; }
		return avg / imgLocations.size();
	}

	/**
	 * @param locations
	 * @return
	 */
	private List< Point > flipAtCenter( final List< Point > locations ) {
		// System.out.println("FLIP FLIP FLIP FLIP FLIP FLIP FLIP FLIP");
		final ArrayList< Point > ret = new ArrayList< Point >( locations.size() );

		final int centerInX = getAvgXpos();
		for ( final Point p : locations ) {
			final int newX = ( -1 * ( p.getIntPosition( 0 ) - centerInX ) ) + centerInX; // flip
																							// at
																							// center
			ret.add( new Point( newX, p.getIntPosition( 1 ), p.getIntPosition( 2 ) ) );
		}

		return ret;
	}

	/**
	 * @return the time-step this GLF corresponds to in the GL it is part of.
	 */
	public int getTime() {
		return this.getParent().getFrames().indexOf( this );
	}

}
