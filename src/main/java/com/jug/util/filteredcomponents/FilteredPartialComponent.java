package com.jug.util.filteredcomponents;

import java.util.ArrayList;

import net.imglib2.Localizable;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.PartialComponent;
import net.imglib2.algorithm.componenttree.pixellist.PixelList;
import net.imglib2.type.Type;

/**
 * Implementation of {@link Component} that stores a list of associated pixels
 * in a {@link PixelList}.
 *
 * @param <T>
 *            value type of the input image.
 *
 * @author Tobias Pietzsch
 */
final class FilteredPartialComponent< T extends Type< T > > implements PartialComponent< T >
{
	/**
	 * Threshold value of the connected component.
	 */
	private final T value;

	/**
	 * Pixels in the component.
	 */
	final PixelList pixelList;

	/**
	 * A list of {@link FilteredPartialComponent} merged into this one since it
	 * was last emitted. (For building up component tree.)
	 */
	final ArrayList< FilteredPartialComponent< T > > children;

	/**
	 * The PixelListComponent assigned to this PixelListComponentIntermediate
	 * when it was last emitted. (For building up component tree.)
	 */
	FilteredComponent< T > emittedComponent;

	/**
	 * Create new empty component.
	 *
	 * @param value
	 *            (initial) threshold value {@see #getValue()}.
	 * @param generator
	 *            the {@link PixelListComponentGenerator#linkedList} is used to
	 *            store the {@link #pixelList}.
	 */
	FilteredPartialComponent( final T value, final FilteredPartialComponentGenerator< T > generator )
	{
		pixelList = new PixelList( generator.linkedList.randomAccess(), generator.dimensions );
		this.value = value.copy();
		children = new ArrayList< FilteredPartialComponent< T > >();
		emittedComponent = null;
	}

	@Override
	public void addPosition( final Localizable position )
	{
		pixelList.addPosition( position );
	}

	@Override
	public T getValue()
	{
		return value;
	}

	@Override
	public void setValue( final T value )
	{
		this.value.set( value );
	}

	@Override
	public void merge( final PartialComponent< T > component )
	{
		final FilteredPartialComponent< T > c = ( FilteredPartialComponent< T > ) component;
		pixelList.merge( c.pixelList );
		children.add( c );
	}

	@Override
	public String toString()
	{
		String s = "{" + value.toString() + " : ";
		boolean first = true;
		for ( final Localizable l : pixelList )
		{
			if ( first )
			{
				first = false;
			}
			else
			{
				s += ", ";
			}
			s += l.toString();
		}
		return s + "}";
	}
}
