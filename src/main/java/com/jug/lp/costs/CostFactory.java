/**
 *
 */
package com.jug.lp.costs;

import net.imglib2.Pair;
import net.imglib2.algorithm.componenttree.Component;

import com.jug.MotherMachine;
import com.jug.util.ComponentTreeUtils;
import com.jug.util.SimpleFunctionAnalysis;


/**
 * @author jug
 */
public class CostFactory {

	public static String latestCostEvaluation = "";

	public static double getMigrationCost( final double oldPosition, final double newPosition, final double normalizer ) {
		double deltaH = ( oldPosition - newPosition ) / normalizer;
		double power = 0.0;
		double costDeltaH = 0.0;
		if ( deltaH > 0 ) { // upward migration
			deltaH = Math.max( 0, deltaH - 0.05 ); // going upwards for up to 5% is for free...
			power = 5.0;
		} else { // downward migration
			power = 7.0;
		}
		deltaH = Math.abs( deltaH );
		costDeltaH = deltaH * Math.pow( 1 + deltaH, power );
		latestCostEvaluation = String.format( "c_h = %.4f * %.4f^%.1f = %.4f", deltaH, 1 + deltaH, power, costDeltaH );
		return costDeltaH;
	}

	public static double getGrowthCost( final double oldSize, final double newSize, final double normalizer ) {
		double deltaL = ( newSize - oldSize ) / normalizer;
		double power = 0.0;
		double costDeltaL = 0.0;
		if ( deltaL > 0 ) { // growth
			deltaL = Math.max( 0, deltaL - 0.05 ); // growing up 5% is free
			power = 4.0;
		} else { // shrinkage
			power = 6.0;
		}
		deltaL = Math.abs( deltaL );
		costDeltaL = deltaL * Math.pow( 1 + deltaL, power );
		latestCostEvaluation = String.format( "c_l = %.4f * %.4f^%.1f = %.4f", deltaL, 1 + deltaL, power, costDeltaL );
		return costDeltaL;
	}

	public static double getIntensityMismatchCost( final double oldIntensity, final double newIntensity ) {
		final double deltaV = Math.abs( oldIntensity - newIntensity ); // change in intensity value of the two CTNs.
		final double costDeltaV = 0.0 * deltaV;
		latestCostEvaluation = String.format( "c_v = 0.0 * %.4f = %.4f", deltaV, costDeltaV );
		return costDeltaV;
	}

	public static double getUnevenDivisionCost( final double sizeFirstChild, final double sizeSecondChild ) {
		final double deltaS = Math.abs( sizeFirstChild - sizeSecondChild ) / Math.min( sizeFirstChild, sizeSecondChild );
		double power = 2.0;
		double costDeltaL = 0.0;
		if ( deltaS > 1.15 ) {
			power = 7.0;
		}
		costDeltaL = Math.pow( deltaS, power );
		latestCostEvaluation = String.format( "c_d = %.4f^%.1f = %.4f", deltaS, power, costDeltaL );
		return costDeltaL;
	}

	/**
	 * @param ctNode
	 * @param gapSepFkt
	 * @return
	 */
	public static double getSegmentationCost( final Component< ?, ? > ctNode, final double[] gapSepFkt ) {
		final Pair< Integer, Integer > segInterval = ComponentTreeUtils.getTreeNodeInterval( ctNode );
		final int a = segInterval.getA().intValue();
		final int b = segInterval.getB().intValue();

		int aReduced = SimpleFunctionAnalysis.getRighthandLocalMax( gapSepFkt, a ).a.intValue();
		aReduced = SimpleFunctionAnalysis.getRighthandLocalMin( gapSepFkt, aReduced ).a.intValue();
		int bReduced = SimpleFunctionAnalysis.getLefthandLocalMax( gapSepFkt, b ).a.intValue();
		bReduced = SimpleFunctionAnalysis.getLefthandLocalMin( gapSepFkt, bReduced ).a.intValue();
		if ( aReduced > bReduced ) {
			aReduced = bReduced = SimpleFunctionAnalysis.getMin( gapSepFkt, a, b ).a.intValue();
		}

		final double l = gapSepFkt[ a ];
		final double r = gapSepFkt[ b ];

		final double maxReduced = SimpleFunctionAnalysis.getMax( gapSepFkt, aReduced, bReduced ).b.doubleValue();
		final double min = SimpleFunctionAnalysis.getMin( gapSepFkt, a, b ).b.doubleValue();

		final double maxRimHeight = Math.max( l, r ) - min;
		final double reducedMaxHeight = maxReduced - min;
		double cost = -( maxRimHeight - reducedMaxHeight ) + MotherMachine.MIN_GAP_CONTRAST;

		// Special case: min-value is above average gap-sep-fkt value (happens often at the very top)
		final double avgFktValue = SimpleFunctionAnalysis.getSum( gapSepFkt ) / ( gapSepFkt.length - 1 );
//		final double distAboveAvg = Math.max( 0.0, min - avgFktValue );
		final double medianValue = SimpleFunctionAnalysis.getMedian( gapSepFkt, a, b );
		final double distAboveAvg = Math.max( 0.0, medianValue - avgFktValue );
		cost += ( distAboveAvg + 0.05 ) * Math.pow( 1 + ( distAboveAvg + 0.05 ), 8.0 );

		// cell is too small
		if ( a > 0 && b - a < MotherMachine.MIN_CELL_LENGTH ) { // if a==0, only a part of the cell is seen!
			cost = 100;
		}
		return cost;
	}
}
