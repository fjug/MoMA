package com.jug.util.filteredcomponents;

import java.util.LinkedList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.PartialComponent;
import net.imglib2.algorithm.componenttree.pixellist.PixelList;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.LongType;

/**
 * Implementation of {@link Component.Generator} for creating
 * {@link PixelListComponentIntermediate} components. According to the
 * dimensions of the input image it creates a {@link LongType} {@link Img} to
 * store the {@link PixelList} used in the components.
 *
 * @param <T>
 *            value type of the input image.
 *
 * @author Tobias Pietzsch
 */
final class FilteredPartialComponentGenerator< T extends Type< T > > implements PartialComponent.Generator< T, FilteredPartialComponent< T > >
{
	private final T maxValue;

	/**
	 * Dimensions of the {@link LinkedList} image.
	 */
	final long[] dimensions;

	/**
	 * Represents a singly-linked list of pixel locations {@see PixelList}.
	 */
	final Img< LongType > linkedList;

	/**
	 * According to the dimensions of the input image, create a {@link LongType}
	 * {@link Img} to store the {@link PixelList} used in the components
	 * generated {@link #createComponent(Type)}.
	 *
	 * @param maxValue
	 *            a value (e.g., grey-level) greater than any occurring in the
	 *            input image.
	 * @param input
	 *            input image.
	 * @param imgFactory
	 *            used to create PixelList image.
	 */
	FilteredPartialComponentGenerator( final T maxValue, final RandomAccessibleInterval< T > input, final ImgFactory< LongType > imgFactory )
	{
		this.maxValue = maxValue;
		dimensions = new long[ input.numDimensions() ];
		input.dimensions( dimensions );
		linkedList = imgFactory.create( dimensions, new LongType() );
	}

	@Override
	public FilteredPartialComponent< T > createComponent( final T value )
	{
		return new FilteredPartialComponent< T >( value, this );
	}

	@Override
	public FilteredPartialComponent< T > createMaxComponent()
	{
		return new FilteredPartialComponent< T >( maxValue, this );
	}
}
