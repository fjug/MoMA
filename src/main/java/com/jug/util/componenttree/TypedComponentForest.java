package com.jug.util.componenttree;

import java.util.Set;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.algorithm.componenttree.mser.MserTree;
import net.imglib2.algorithm.componenttree.pixellist.PixelListComponentTree;
import net.imglib2.type.numeric.real.DoubleType;

public class TypedComponentForest< T, C extends Component< T, C > > implements ComponentForest< C >
{
	ComponentForest< C > forest;

	public TypedComponentForest( final ComponentForest< C > f )
	{
		this.forest = f;
	}

	static < T, C extends Component< T, C > > TypedComponentForest< T, C > create( final ComponentForest< C > f )
	{
		return new TypedComponentForest< T, C >( f );
	}

	/**
	 * Get the set of root nodes of this component forest.
	 *
	 * @return set of roots.
	 */
	@Override
	public Set< C > roots()
	{
		return forest.roots();
	}

	public static void main( final String[] args )
	{
		final RandomAccessibleInterval< DoubleType > input = null;
		TypedComponentForest< DoubleType, ? extends Component< DoubleType, ? > > forest;
		forest = TypedComponentForest.create( PixelListComponentTree.buildComponentTree( input, new DoubleType(), true ) );
		forest = TypedComponentForest.create( MserTree.buildMserTree( input, 0, 0, 0, 0, 0, true ) );
	}
}