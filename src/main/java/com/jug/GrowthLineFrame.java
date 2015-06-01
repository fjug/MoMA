/**
 *
 */
package com.jug;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.type.numeric.real.FloatType;

import com.jug.util.filteredcomponents.FilteredComponent;
import com.jug.util.filteredcomponents.FilteredComponentTree;
import com.jug.util.filteredcomponents.FilteredComponentTree.Filter;
import com.jug.util.filteredcomponents.FilteredComponentTree.MaxGrowthPerStepRatioWithMinimalAbsoluteIncrease;

/**
 * @author jug
 *         Represents one growth line (well) in which Bacteria can grow, at one
 *         instance in time.
 *         This corresponds to one growth line micrograph. The class
 *         representing an entire time
 *         series (2d+t) representation of an growth line is
 *         <code>GrowthLine</code>.
 */
public class GrowthLineFrame extends AbstractGrowthLineFrame< FilteredComponent< FloatType > > {

//	final Filter maxGrowthPerStep = new MaxGrowthPerStep( 4 );
//	final Filter maxGrowthPerStepRatio = new MaxGrowthPerStepRatio( 0.075 );
	final Filter maxGrowthPerStepRatioWithMinimalAbsoluteIncrease =
			new MaxGrowthPerStepRatioWithMinimalAbsoluteIncrease( 0.05, 6 );

	/**
	 * @see com.jug.AbstractGrowthLineFrame#buildIntensityTree(net.imglib2.RandomAccessibleInterval)
	 */
	@Override
	protected ComponentForest< FilteredComponent< FloatType >> buildIntensityTree( final RandomAccessibleInterval< FloatType > raiFkt ) {
		return FilteredComponentTree.buildComponentTree(
				raiFkt,
				new FloatType(),
				MotherMachine.MIN_CELL_LENGTH,
				Long.MAX_VALUE,
				maxGrowthPerStepRatioWithMinimalAbsoluteIncrease,
				true );
//		return MserTree.buildMserTree( raiFkt, MotherMachine.MIN_GAP_CONTRAST / 2.0, MotherMachine.MIN_CELL_LENGTH, Long.MAX_VALUE, 0.5, 0.33, true );
	}

	/**
	 * @see com.jug.AbstractGrowthLineFrame#buildParaMaxFlowSumTree(net.imglib2.RandomAccessibleInterval)
	 */
	@Override
	protected ComponentForest< FilteredComponent< FloatType >> buildParaMaxFlowSumTree( final RandomAccessibleInterval< FloatType > raiFkt ) {
		return FilteredComponentTree.buildComponentTree(
				raiFkt,
				new FloatType(),
				3,
				Long.MAX_VALUE,
				maxGrowthPerStepRatioWithMinimalAbsoluteIncrease,
				true );
	}

}
