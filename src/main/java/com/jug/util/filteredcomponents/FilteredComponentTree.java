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
import net.imglib2.algorithm.tree.Forest;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.util.Util;

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
public final class FilteredComponentTree< T extends Type< T > >
		implements
		ComponentForest< FilteredComponent< T > >,
		Forest< FilteredComponent< T > >,
		Iterable< FilteredComponent< T > >,
		PartialComponent.Handler< FilteredPartialComponent< T > > {

	private static enum FilterType {
		MAX_GROWTH_PER_STEP, MAX_GROWTH_PER_STEP_RATIO, MAX_GROWTH_PER_STEP_RATIO_WITH_MINIMAL_ABS_INC
	}

	public interface Filter {

		FilterType type();
	}

	public static class MaxGrowthPerStep implements Filter {

		private final long maxGrowthPerStep;

		@Override
		public FilterType type() {
			return FilterType.MAX_GROWTH_PER_STEP;
		}

		public MaxGrowthPerStep( final long maxGrowthPerStep ) {
			this.maxGrowthPerStep = maxGrowthPerStep;
		}

		@Override
		public String toString() {
			return "MaxGrowthPerStep = " + maxGrowthPerStep;
		}
	}

	public static class MaxGrowthPerStepRatio implements Filter {

		private final double maxGrowthPerStepRatio;

		@Override
		public FilterType type() {
			return FilterType.MAX_GROWTH_PER_STEP_RATIO;
		}

		public MaxGrowthPerStepRatio( final double maxGrowthPerStepRatio ) {
			this.maxGrowthPerStepRatio = maxGrowthPerStepRatio;
		}
	}

	public static class MaxGrowthPerStepRatioWithMinimalAbsoluteIncrease implements Filter {

		private final double maxGrowthPerStepRatio;
		private final long minGrowthPerStep;

		@Override
		public FilterType type() {
			return FilterType.MAX_GROWTH_PER_STEP_RATIO_WITH_MINIMAL_ABS_INC;
		}

		public MaxGrowthPerStepRatioWithMinimalAbsoluteIncrease(
				final double maxGrowthPerStepRatio,
				final long minGrowthPerStep ) {
			this.maxGrowthPerStepRatio = maxGrowthPerStepRatio;
			this.minGrowthPerStep = minGrowthPerStep;
		}
	}

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
	 * @param filter
	 * @param darkToBright
	 *            whether to apply thresholds from dark to bright (true) or
	 *            bright to dark (false)
	 * @return component tree of the image.
	 */
	public static < T extends RealType< T > > FilteredComponentTree< T > buildComponentTree(
			final RandomAccessibleInterval< T > input,
			final T type,
			final long minComponentSize,
			final long maxComponentSize,
			final Filter filter,
			final boolean darkToBright ) {
		final ImgFactory< LongType > imgFactory =
				Util.getArrayOrCellImgFactory( input, new LongType() );
		return buildComponentTree(
				input,
				type,
				imgFactory,
				minComponentSize,
				maxComponentSize,
				filter,
				darkToBright );
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
	 * @param filter
	 * @param darkToBright
	 *            whether to apply thresholds from dark to bright (true) or
	 *            bright to dark (false)
	 * @return component tree of the image.
	 */
	public static < T extends RealType< T > > FilteredComponentTree< T > buildComponentTree(
			final RandomAccessibleInterval< T > input,
			final T type,
			final ImgFactory< LongType > imgFactory,
			final long minComponentSize,
			final long maxComponentSize,
			final Filter filter,
			final boolean darkToBright ) {
		final T max = type.createVariable();
		max.setReal( darkToBright ? type.getMaxValue() : type.getMinValue() );
		final FilteredPartialComponentGenerator< T > generator = new FilteredPartialComponentGenerator< T >( max, input, imgFactory );
		final FilteredComponentTree< T > tree =
				new FilteredComponentTree< T >( minComponentSize, maxComponentSize, filter, generator.linkedList );
		BuildComponentTree.buildComponentTree( input, generator, tree, darkToBright );
		return tree;
	}

	private final ArrayList< FilteredComponent< T > > nodes;

	private final HashSet< FilteredComponent< T > > roots;

	private final long minComponentSize;

	private final long maxComponentSize;

	private final boolean filterByGrowthRatio;

	private final long maxGrowthPerStep;

	private final double maxGrowthPerStepRatio;

	private final Img< LongType > linkedList;

	private FilteredComponentTree(
			final long minComponentSize,
			final long maxComponentSize,
			final Filter filter,
			final Img< LongType > linkedList ) {
		roots = new HashSet< FilteredComponent< T > >();
		nodes = new ArrayList< FilteredComponent< T > >();
		this.minComponentSize = minComponentSize;
		this.maxComponentSize = maxComponentSize;
		if ( filter.type().equals( FilterType.MAX_GROWTH_PER_STEP ) )
		{
			this.filterByGrowthRatio = false;
			this.maxGrowthPerStep = ( ( MaxGrowthPerStep ) filter ).maxGrowthPerStep;
			this.maxGrowthPerStepRatio = 0;
		}
		else if ( filter.type().equals( FilterType.MAX_GROWTH_PER_STEP_RATIO ) )
		{
			this.filterByGrowthRatio = true;
			this.maxGrowthPerStep = 0;
			this.maxGrowthPerStepRatio = ( ( MaxGrowthPerStepRatio ) filter ).maxGrowthPerStepRatio;
		}
		else
		{
			this.filterByGrowthRatio = true;
			this.maxGrowthPerStep =
					( ( MaxGrowthPerStepRatioWithMinimalAbsoluteIncrease ) filter ).minGrowthPerStep;
			this.maxGrowthPerStepRatio =
					( ( MaxGrowthPerStepRatioWithMinimalAbsoluteIncrease ) filter ).maxGrowthPerStepRatio;
		}
		this.linkedList = linkedList;
	}

	@Override
	public void emit( final FilteredPartialComponent< T > intermediate ) {
		final long size = intermediate.pixelList.size();
		if ( size >= minComponentSize && size <= maxComponentSize ) {
			int numChildren = 0;
			if ( intermediate.emittedComponent != null ) ++numChildren;
			for ( final FilteredPartialComponent< T > c : intermediate.children )
				if ( c.emittedComponent != null ) ++numChildren;

			boolean createNewComponent = true;
			if ( numChildren == 1 ) {
				// get previously emitted node
				FilteredComponent< T > component = intermediate.emittedComponent;
				if ( component == null )
					for ( final FilteredPartialComponent< T > c : intermediate.children )
						if ( c.emittedComponent != null )
							component = c.emittedComponent;

				final boolean postpone;
				if ( filterByGrowthRatio )
					if ( maxGrowthPerStep == 0 ) {
						postpone =
								( ( double ) intermediate.pixelList.size() - component.minSize() ) / component.minSize() < maxGrowthPerStepRatio;
					} else {
						postpone =
								(
								( ( double ) intermediate.pixelList.size() - component.minSize() ) / component.minSize() < maxGrowthPerStepRatio ||
								( intermediate.pixelList.size() - component.minSize() < maxGrowthPerStep )
								);
					}
				else
					postpone =
							intermediate.pixelList.size() - component.minSize() < maxGrowthPerStep;
				if ( postpone ) {
					// update previously emitted node unless that node is a leaf
					if ( !component.children.isEmpty() ) {
						component.update( intermediate );
						createNewComponent = false;
					}
				}
			}

			if ( createNewComponent ) {
				// create new node
				final FilteredComponent< T > component = new FilteredComponent< T >( intermediate );
				for ( final FilteredComponent< T > c : component.children )
					roots.remove( c );
				roots.add( component );
				nodes.add( component );
			}
		} else {
			intermediate.children.clear();
		}
	}

	/**
	 * Returns an iterator over all connected components in the tree.
	 *
	 * @return iterator over all connected components in the tree.
	 */
	@Override
	public Iterator< FilteredComponent< T > > iterator() {
		return nodes.iterator();
	}

	/**
	 * Get the set of roots of the tree (respectively forest...).
	 *
	 * @return set of roots.
	 */
	@Override
	public HashSet< FilteredComponent< T > > roots() {
		return roots;
	}

	public Img< LongType > getLinkedList()
	{
		return linkedList;
	}
}
