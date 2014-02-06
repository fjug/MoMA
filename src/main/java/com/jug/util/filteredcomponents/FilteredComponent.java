package com.jug.util.filteredcomponents;

import java.util.ArrayList;
import java.util.Iterator;

import net.imglib2.Localizable;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.pixellist.PixelList;
import net.imglib2.type.Type;

/**
 * A connected component of the image thresholded at {@link #maxValue()}. The set
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
public final class FilteredComponent< T extends Type< T > > implements Component< T, FilteredComponent< T > >
{
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

	void update( final FilteredPartialComponent< T > intermediate )
	{
		maxValue.set( intermediate.getValue() );
		pixelList = new PixelList( intermediate.pixelList );
		intermediate.emittedComponent = this;
		intermediate.children.clear();
	}

	FilteredComponent( final FilteredPartialComponent< T > intermediate )
	{
		children = new ArrayList< FilteredComponent< T > >();
		parent = null;
		minValue = intermediate.getValue().copy();
		maxValue = intermediate.getValue().copy();
		pixelList = new PixelList( intermediate.pixelList );
		minSize = pixelList.size();
		if( intermediate.emittedComponent != null )
		{
			children.add( intermediate.emittedComponent );
			intermediate.emittedComponent.parent = this;
		}
		for ( final FilteredPartialComponent< T > c : intermediate.children )
			if ( c.emittedComponent != null )
			{
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
	public T maxValue()
	{
		return maxValue;
	}

	/**
	 * Get the image threshold that created the minimum extremal region
	 * associated to this component.
	 *
	 * @return the image threshold that created the extremal region.
	 */
	public T minValue()
	{
		return minValue;
	}

	/**
	 * Get the number of pixels in the maximum extremal region associated to
	 * this component.
	 *
	 * @return number of pixels in the extremal region.
	 */
	public long maxSize()
	{
		return pixelList.size();
	}

	/**
	 * Get the number of pixels in the minimum extremal region associated to
	 * this component.
	 *
	 * @return number of pixels in the extremal region.
	 */
	public long minSize()
	{
		return minSize;
	}

	/**
	 * Get an iterator over the pixel locations ({@link Localizable}) in this
	 * connected component.
	 *
	 * @return iterator over locations.
	 */
	@Override
	public Iterator< Localizable > iterator()
	{
		return pixelList.iterator();
	}

	/**
	 * Get the children of this node in the {@link FilteredComponentTree}.
	 *
	 * @return the children of this node in the {@link FilteredComponentTree}.
	 */
	@Override
	public ArrayList< FilteredComponent< T > > getChildren()
	{
		return children;
	}

	/**
	 * Get the parent of this node in the {@link FilteredComponentTree}.
	 *
	 * @return the parent of this node in the {@link FilteredComponentTree}.
	 */
	@Override
	public FilteredComponent< T > getParent()
	{
		return parent;
	}

	@Override
	public long size()
	{
		return maxSize();
	}

	@Override
	public T value()
	{
		return maxValue();
	}
}
