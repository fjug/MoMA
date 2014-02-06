/**
 *
 */
package com.jug;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.type.numeric.real.DoubleType;

import com.jug.util.filteredcomponents.FilteredComponent;
import com.jug.util.filteredcomponents.FilteredComponentTree;

/**
 * @author jug
 *         Represents one growth line (well) in which Bacteria can grow, at one
 *         instance in time.
 *         This corresponds to one growth line micrograph. The class
 *         representing an entire time
 *         series (2d+t) representation of an growth line is
 *         <code>GrowthLine</code>.
 */
public class GrowthLineFrame extends AbstractGrowthLineFrame< FilteredComponent< DoubleType > > {

	/**
	 * @see com.jug.AbstractGrowthLineFrame#buildTree(net.imglib2.RandomAccessibleInterval)
	 */
	@Override
	protected ComponentForest< FilteredComponent< DoubleType >> buildTree( final RandomAccessibleInterval< DoubleType > raiFkt ) {
		return FilteredComponentTree.buildComponentTree( raiFkt, new DoubleType(), 3, Long.MAX_VALUE, true );
//		return MserComponentTree.buildMserTree( raiFkt, MotherMachine.MIN_GAP_CONTRAST / 2.0, MotherMachine.MIN_CELL_LENGTH, Long.MAX_VALUE, 0.5, 0.33, true );
	}

}
