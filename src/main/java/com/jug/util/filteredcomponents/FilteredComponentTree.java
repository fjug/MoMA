package com.jug.util.filteredcomponents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.componenttree.BuildComponentTree;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.algorithm.componenttree.ComponentTree;
import net.imglib2.algorithm.componenttree.PartialComponent;
import net.imglib2.algorithm.componenttree.pixellist.PixelList;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;

/**
 * Component tree of an image stored as a tree of {@link FilteredComponent}s.
 * This class is used both to represent and build the tree. For building the
 * tree {@link Component.Handler} is implemented to gather
 * {@link FilteredPartialComponent} emitted by {@link ComponentTree}. Only
 * components in a specific size range are accepted. The tree contains only one
 * {@link FilteredComponent} per branch. This is the component with the highest
 * threshold, i.e., right before the branch joins another.
 *
 * @param <T>
 *            value type of the input image.
 *
 * @author Tobias Pietzsch
 */
public final class FilteredComponentTree< T extends Type< T > > implements ComponentForest< FilteredComponent< T > >, Iterable< FilteredComponent< T > >, PartialComponent.Handler< FilteredPartialComponent< T > >
{
	/**
	 * Build a component tree from an input image. Calls
	 * {@link #buildComponentTree(RandomAccessibleInterval, RealType, ImgFactory, boolean)}
	 * using an {@link ArrayImgFactory} or {@link CellImgFactory} depending on
	 * input image size.
	 *
	 * @param input
	 *            the input image.
	 * @param type
	 *            a variable of the input image type.
	 * @param minComponentSize
	 *            minimum allowed size for an accepted component.
	 * @param maxComponentSize
	 *            maximum allowed size for an accepted component.
	 * @param darkToBright
	 *            whether to apply thresholds from dark to bright (true) or
	 *            bright to dark (false)
	 * @return component tree of the image.
	 */
	public static < T extends RealType< T > > FilteredComponentTree< T > buildComponentTree(
			final RandomAccessibleInterval< T > input, final T type,
			final long minComponentSize, final long maxComponentSize, final boolean darkToBright )
	{
		final int numDimensions = input.numDimensions();
		long size = 1;
		for ( int d = 0; d < numDimensions; ++d )
			size *= input.dimension( d );
		if( size > Integer.MAX_VALUE ) {
			final int cellSize = ( int ) Math.pow( Integer.MAX_VALUE / new LongType().getEntitiesPerPixel(), 1.0 / numDimensions );
			return buildComponentTree( input, type, new CellImgFactory< LongType >( cellSize ), minComponentSize, maxComponentSize, darkToBright );
		} else
			return buildComponentTree( input, type, new ArrayImgFactory< LongType >(), minComponentSize, maxComponentSize, darkToBright );
	}

	/**
	 * Build a component tree from an input image.
	 *
	 * @param input
	 *            the input image.
	 * @param type
	 *            a variable of the input image type.
	 * @param imgFactory
	 *            used for creating the {@link PixelList} image {@see
	 *            FilteredComponentGenerator}.
	 * @param minComponentSize
	 *            minimum allowed size for an accepted component.
	 * @param maxComponentSize
	 *            maximum allowed size for an accepted component.
	 * @param darkToBright
	 *            whether to apply thresholds from dark to bright (true) or
	 *            bright to dark (false)
	 * @return component tree of the image.
	 */
	public static < T extends RealType< T > > FilteredComponentTree< T > buildComponentTree(
			final RandomAccessibleInterval< T > input, final T type, final ImgFactory< LongType > imgFactory,
			final long minComponentSize, final long maxComponentSize, final boolean darkToBright )
	{
		final T max = type.createVariable();
		max.setReal( darkToBright ? type.getMaxValue() : type.getMinValue() );
		final FilteredPartialComponentGenerator< T > generator = new FilteredPartialComponentGenerator< T >( max, input, imgFactory );
		final FilteredComponentTree< T > tree = new FilteredComponentTree< T >( minComponentSize, maxComponentSize );
		BuildComponentTree.buildComponentTree( input, generator, tree, darkToBright );
		return tree;
	}

	private final ArrayList< FilteredComponent< T > > nodes;

	private final HashSet< FilteredComponent< T > > roots;

	private final long minComponentSize;

	private final long maxComponentSize;

	private FilteredComponentTree( final long minComponentSize, final long maxComponentSize )
	{
		roots = new HashSet< FilteredComponent< T > >();
		nodes = new ArrayList< FilteredComponent< T > >();
		this.minComponentSize = minComponentSize;
		this.maxComponentSize = maxComponentSize;
	}

	@Override
	public void emit( final FilteredPartialComponent< T > intermediate )
	{
		final long size = intermediate.pixelList.size();
		if ( size >= minComponentSize && size <= maxComponentSize )
		{
			int numChildren = 0;
			if( intermediate.emittedComponent != null )
				++numChildren;
			for ( final FilteredPartialComponent< T > c : intermediate.children )
				if ( c.emittedComponent != null )
					++numChildren;
			if ( numChildren == 1 )
			{
				// update previously emitted node
				FilteredComponent< T > component = intermediate.emittedComponent;
				if( component == null )
					for ( final FilteredPartialComponent< T > c : intermediate.children )
						if ( c.emittedComponent != null )
							component = c.emittedComponent;
				component.update( intermediate );
			}
			else
			{
				// create new node
				final FilteredComponent< T > component = new FilteredComponent< T >( intermediate );
				for ( final FilteredComponent< T > c : component.children )
					roots.remove( c );
				roots.add( component );
				nodes.add( component );
			}
		}
		else
		{
//			for ( final FilteredComponentIntermediate< T > c : intermediate.children )
//				c.freeForReuse();
			intermediate.children.clear();
		}
	}

	/**
	 * Returns an iterator over all connected components in the tree.
	 *
	 * @return iterator over all connected components in the tree.
	 */
	@Override
	public Iterator< FilteredComponent< T > > iterator()
	{
		return nodes.iterator();
	}

	/**
	 * Get the set of roots of the tree (respectively forest...).
	 *
	 * @return set of roots.
	 */
	@Override
	public HashSet< FilteredComponent< T > > roots()
	{
		return roots;
	}
}
