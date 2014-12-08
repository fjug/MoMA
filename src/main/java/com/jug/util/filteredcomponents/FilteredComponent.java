package com.jug.util.filteredcomponents;

import java.util.ArrayList;
import java.util.Iterator;

import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.pixellist.PixelList;
import net.imglib2.type.Type;

/**
 * A connected component of the image thresholded at {@link #maxValue()}. The
 * set
 * of pixels can be accessed by iterating ({@link #iterator()}) the component.
 * 
 * This is a node in a {@link FilteredComponentTree}. The child and parent
 * nodes can be accessed by {@link #getChildren()} and {@link #getParent()}.
 * 
 * @param <T>
 *            value type of the input image.
 * 
 * @author Tobias Pietzsch
 */
public final class FilteredComponent< T extends Type< T > > implements Component< T, FilteredComponent< T > > {

	/**
	 * child nodes in the {@link FilteredComponentTree}.
	 */
	final ArrayList< FilteredComponent< T > > children;

	/**
	 * parent node in the {@link FilteredComponentTree}.
	 */
	private FilteredComponent< T > parent;

	/**
	 * Minimum threshold value of the connected component.
	 */
	private final T minValue;

	/**
	 * Maximum threshold value of the connected component.
	 */
	private final T maxValue;

	/**
	 * minimum size (maximum size can be determined from {@link #pixelList}).
	 */
	private final long minSize;

	/**
	 * Pixels in the component.
	 */
	private PixelList pixelList;

	/**
	 * Pixels in the extended component.
	 * The extended component contains all regular pixels + takes on regions
	 * from the parent note that are not inside siblings of the current
	 * component.
	 */
	private ArrayList< Localizable > pixelListExtended;

	void update( final FilteredPartialComponent< T > intermediate ) {
		maxValue.set( intermediate.getValue() );
		pixelList = new PixelList( intermediate.pixelList );
		pixelListExtended = null;
		intermediate.emittedComponent = this;
		intermediate.children.clear();
	}

	FilteredComponent( final FilteredPartialComponent< T > intermediate ) {
		children = new ArrayList< FilteredComponent< T > >();
		parent = null;
		minValue = intermediate.getValue().copy();
		maxValue = intermediate.getValue().copy();
		pixelList = new PixelList( intermediate.pixelList );
		pixelListExtended = null;
		minSize = pixelList.size();
		if ( intermediate.emittedComponent != null ) {
			children.add( intermediate.emittedComponent );
			intermediate.emittedComponent.parent = this;
		}
		for ( final FilteredPartialComponent< T > c : intermediate.children )
			if ( c.emittedComponent != null ) {
				children.add( c.emittedComponent );
				c.emittedComponent.parent = this;
			}
		intermediate.emittedComponent = this;
		intermediate.children.clear();
	}

	/**
	 * Get the image threshold that created the maximum extremal region
	 * associated to this component.
	 * 
	 * @return the image threshold that created the extremal region.
	 */
	public T maxValue() {
		return maxValue;
	}

	/**
	 * Get the image threshold that created the minimum extremal region
	 * associated to this component.
	 * 
	 * @return the image threshold that created the extremal region.
	 */
	public T minValue() {
		return minValue;
	}

	/**
	 * Get the number of pixels in the maximum extremal region associated to
	 * this component.
	 * 
	 * @return number of pixels in the extremal region.
	 */
	public long maxSize() {
		return pixelList.size();
	}

	/**
	 * Get the number of pixels in the minimum extremal region associated to
	 * this component.
	 * 
	 * @return number of pixels in the extremal region.
	 */
	public long minSize() {
		return minSize;
	}

	/**
	 * Get an iterator over the pixel locations ({@link Localizable}) in this
	 * connected component.
	 * 
	 * @return iterator over locations.
	 */
	@Override
	public Iterator< Localizable > iterator() {
		return pixelList.iterator();
	}

	/**
	 * Get the children of this node in the {@link FilteredComponentTree}.
	 * 
	 * @return the children of this node in the {@link FilteredComponentTree}.
	 */
	@Override
	public ArrayList< FilteredComponent< T > > getChildren() {
		return children;
	}

	/**
	 * Get the parent of this node in the {@link FilteredComponentTree}.
	 * 
	 * @return the parent of this node in the {@link FilteredComponentTree}.
	 */
	@Override
	public FilteredComponent< T > getParent() {
		return parent;
	}

	@Override
	public long size() {
		return maxSize();
	}

	@Override
	public T value() {
		return maxValue();
	}

	// Trials for extended size etc.
	// =============================
	public long maxSizeExtended() {
		evaluatePixelListExtendedIfNeeded();
		return pixelListExtended.size();
	}

	public long sizeExtended() {
		return maxSizeExtended();
	}

	public Iterator< Localizable > iteratorExtended() {
		evaluatePixelListExtendedIfNeeded();
		return pixelListExtended.iterator();
	}

	private void evaluatePixelListExtendedIfNeeded() {
		if ( pixelListExtended == null ) {
			evaluatePixelListExtended();
		}
	}

	private void evaluatePixelListExtended() {
		this.pixelListExtended = new ArrayList< Localizable >();

		if ( parent == null ) {
			for ( final Localizable pixel : this.pixelList ) {
				this.pixelListExtended.add( new Point( pixel ) );
			}

		} else {

			final ArrayList< FilteredComponent< T >> siblings = new ArrayList< FilteredComponent< T > >();
			for ( final FilteredComponent< T > comp : parent.children ) {
				if ( !comp.equals( this ) ) {
					siblings.add( comp );
				}
			}

			final PixelList potentialPixelsToAdd = parent.pixelList;

			for ( final Localizable pixel : potentialPixelsToAdd ) {
				long minSquaredDist = Long.MAX_VALUE;
				for ( final FilteredComponent< T > sibling : siblings ) {
					minSquaredDist = Math.min( minSquaredDist, sibling.minSquaredDistToPixel( pixel ) );
				}
				if ( minSquaredDist > this.minSquaredDistToPixel( pixel ) ) {
					this.pixelListExtended.add( new Point( pixel ) );
				}
			}
		}
	}

	private final long minSquaredDistToPixel( final Localizable refPixel ) {
		long minSquaredDist = Long.MAX_VALUE;
		for ( final Localizable pixel : this.pixelList ) {
			long squaredDist = 0;
			for ( int d = 0; d < pixel.numDimensions(); d++ ) {
				long square = Math.abs( pixel.getIntPosition( d ) - refPixel.getIntPosition( d ) );
				square *= square;
				squaredDist += square;
			}

			if ( minSquaredDist > squaredDist ) minSquaredDist = squaredDist;
		}
		return minSquaredDist;
	}
}
