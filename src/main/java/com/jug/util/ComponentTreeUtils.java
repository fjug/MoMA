/**
 *
 */
package com.jug.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.imglib2.Localizable;
import net.imglib2.Pair;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.algorithm.componenttree.ComponentTree;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ValuePair;

/**
 * @author jug
 *
 */
public class ComponentTreeUtils {

	/**
	 * @param tree
	 * @return
	 */
	public static < C extends Component< ?, C > > List< C > getListOfLeavesInOrder( final ComponentTree< C > tree ) {
		final List< C > leaves = new ArrayList< C >();

		for ( final C root : tree.roots() ) {
			recursivelyAddLeaves( root, leaves );
		}

		return leaves;
	}

	/**
	 * @param root
	 * @param leaves
	 */
	private static < C extends Component< ?, C > > void recursivelyAddLeaves( final C node, final List< C > leaves ) {
		if ( node.getChildren().size() == 0 ) {
			leaves.add( node );
		} else {
			for ( final C child : node.getChildren() ) {
				recursivelyAddLeaves( child, leaves );
			}
		}
	}

	/**
	 * @param candidate
	 * @param hyp
	 * @return
	 */
	public static boolean isAbove( final Component< DoubleType, ? > candidate, final Component< DoubleType, ? > reference ) {
		final Pair< Integer, Integer > candMinMax = getTreeNodeInterval( candidate );
		final Pair< Integer, Integer > refMinMax = getTreeNodeInterval( reference );
		return candMinMax.getB().intValue() < refMinMax.getA().intValue();
	}

	/**
	 * @param candidate
	 * @param hyp
	 * @return
	 */
	public static boolean isBelow( final Component< DoubleType, ? > candidate, final Component< DoubleType, ? > reference ) {
		final Pair< Integer, Integer > candMinMax = getTreeNodeInterval( candidate );
		final Pair< Integer, Integer > refMinMax = getTreeNodeInterval( reference );
		return candMinMax.getA().intValue() > refMinMax.getB().intValue();
	}

	/**
	 * Returns the smallest and largest value on the x-axis that is spanned by
	 * this component-tree-node.
	 * Note that this function really only makes sense if the comp.-tree was
	 * built on a one-dimensional image (as it is the case for my current
	 * MotherMachine stuff...)
	 *
	 * @param node
	 *            the node in question.
	 * @return a <code>Pair</code> or two <code>Integers</code> giving the
	 *         leftmost and rightmost point on the x-axis that is covered by
	 *         this component-tree-node respectively.
	 */
	public static Pair< Integer, Integer > getTreeNodeInterval( final Component< ?, ? > node ) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		final Iterator< Localizable > componentIterator = node.iterator();
		while ( componentIterator.hasNext() ) {
			final int pos = componentIterator.next().getIntPosition( 0 );
			min = Math.min( min, pos );
			max = Math.max( max, pos );
		}
		return new ValuePair< Integer, Integer >( new Integer( min ), new Integer( max ) );
	}

	// public static double[] getFunctionValues( final Component<
	// DoubleType, ? > node ) {
	// Pair< Integer, Integer > interval = getTreeNodeInterval( node );
	//
	// double[] ret = new double[ interval.b.intValue() - interval.a.intValue()
	// ];
	//
	// final Iterator< Localizable > componentIterator = node.iterator();
	// while ( componentIterator.hasNext() ) {
	// final int pos = componentIterator.next().getIntPosition( 0 ) -
	// interval.a.intValue();
	// ret[pos] = componentIterator.
	// }
	// return
	// }

	/**
	 * @param to
	 * @return
	 */
	public static List< Component< DoubleType, ? >> getRightNeighbors( final Component< DoubleType, ? > node ) {
		final ArrayList< Component< DoubleType, ? >> ret = new ArrayList< Component< DoubleType, ? >>();

		Component< DoubleType, ? > rightNeighbor = getRightNeighbor( node );
		if ( rightNeighbor != null ) {
			ret.add( rightNeighbor );
			while ( rightNeighbor.getChildren().size() > 0 ) {
				rightNeighbor = rightNeighbor.getChildren().get( 0 );
				ret.add( rightNeighbor );
			}
		}

		return ret;
	}

	/**
	 * @param node
	 * @return
	 */
	private static Component< DoubleType, ? > getRightNeighbor( final Component< DoubleType, ? > node ) {
		// TODO Note that we do not find the right neighbor in case the
		// component tree has several roots and the
		// right neighbor is somewhere down another root.
		final Component< DoubleType, ? > father = node.getParent();

		if ( father != null ) {
			final int idx = father.getChildren().indexOf( node );
			if ( idx + 1 < father.getChildren().size() ) {
				return father.getChildren().get( idx + 1 );
			} else {
				return getRightNeighbor( father );
			}
		}
		return null;
	}

	/**
	 * @param ct
	 * @return
	 */
	public static < C extends Component< ?, C > > int countNodes( final ComponentForest< C > ct ) {
		int nodeCount = ct.roots().size();;
		for ( final C root : ct.roots() ) {
			nodeCount += countNodes( root );
		}
		return nodeCount;
	}

	/**
	 * @param root
	 * @return
	 */
	public static < C extends Component< ?, C > > int countNodes( final C ctn ) {
		int nodeCount = ctn.getChildren().size();
		for ( final C child : ctn.getChildren() ) {
			nodeCount += countNodes( child );
		}
		return nodeCount;
	}

	/**
	 * @param ct
	 * @return
	 */
	public static < C extends Component< ?, C > > List< C > getListOfNodes( final ComponentForest< C > ct ) {
		final ArrayList< C > ret = new ArrayList< C >();
		for ( final C root : ct.roots() ) {
			ret.add( root );
			addListOfNodes( root, ret );
		}
		return ret;
	}

	/**
	 * @param root
	 * @param list
	 */
	private static < C extends Component< ?, C > > void addListOfNodes( final C ctn, final ArrayList< C > list ) {
		for ( final C child : ctn.getChildren() ) {
			list.add( child );
			addListOfNodes( child, list );
		}
	}

	/**
	 * @param ctnLevel
	 * @return
	 */
	public static < C extends Component< ?, C > > ArrayList< C > getAllChildren( final ArrayList< C > ctnLevel ) {
		final ArrayList< C > nextCtnLevel = new ArrayList< C >();
		for ( final C ctn : ctnLevel ) {
			for ( final C child : ctn.getChildren() ) {
				nextCtnLevel.add( child );
			}
		}
		return nextCtnLevel;
	}

	/**
	 * @param ctn
	 * @return
	 */
	public static int getLevelInTree( final Component< ?, ? > ctn ) {
		int level = 0;
		Component< ?, ? > runner = ctn;
		while ( runner.getParent() != null ) {
			level++;
			runner = runner.getParent();
		}
		return level;
	}
}
