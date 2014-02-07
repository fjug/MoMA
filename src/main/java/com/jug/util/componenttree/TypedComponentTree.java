package com.jug.util.componenttree;

import java.util.Set;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.ComponentTree;
import net.imglib2.algorithm.componenttree.pixellist.PixelListComponentTree;
import net.imglib2.type.numeric.real.DoubleType;

public class TypedComponentTree< T, C extends Component< T, C > > implements ComponentTree< C >
{
	ComponentTree< C > tree;

	public TypedComponentTree( final ComponentTree< C > f )
	{
		this.tree = f;
	}

	static < T, C extends Component< T, C > > TypedComponentTree< T, C > create( final ComponentTree< C > f )
	{
		return new TypedComponentTree< T, C >( f );
	}

	/**
	 * Get the set of root nodes of this component forest.
	 *
	 * @return set of roots.
	 */
	@Override
	public Set< C > roots()
	{
		return tree.roots();
	}

	@Override
	public C root()
	{
		return tree.root();
	}

	public static void main( final String[] args )
	{
		final RandomAccessibleInterval< DoubleType > input = null;
		TypedComponentTree< DoubleType, ? extends Component< DoubleType, ? > > tree;
		tree = TypedComponentTree.create( PixelListComponentTree.buildComponentTree( input, new DoubleType(), true ) );
//		tree = TypedComponentForest.create( MserTree.buildMserTree( input, 0, 0, 0, 0, 0, true ) );
	}
}