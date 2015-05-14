/**
 *
 */
package com.jug.tracking.costs;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;

import com.jug.MotherMachine;
import com.jug.util.ComponentTreeUtils;
import com.jug.util.SimpleFunctionAnalysis;

/**
 * @author jug
 */
public class CostFactory {

	public static String latestCostEvaluation = "";

	private static float getMigrationCost(
			final float oldPosition,
			final float newPosition,
			final float normalizer ) {
		float deltaH = ( oldPosition - newPosition ) / normalizer;
		float power = 0.0f;
		float costDeltaH = 0.0f;
		if ( deltaH > 0 ) { // upward migration
			deltaH = Math.max( 0, deltaH - 0.05f ); // going upwards for up to 5% is for free...
			power = 3.0f;
		} else { // downward migration
			power = 12.0f;
		}
		deltaH = Math.abs( deltaH );
		costDeltaH = deltaH * ( float ) Math.pow( 1 + deltaH, power );
		latestCostEvaluation = String.format( "c_h = %.4f * %.4f^%.1f = %.4f", deltaH, 1 + deltaH, power, costDeltaH );
		return costDeltaH;
	}

	private static float getGrowthCost(
			final float oldSize,
			final float newSize,
			final float normalizer ) {
		float deltaL = ( newSize - oldSize ) / normalizer;
		float power = 0.0f;
		float costDeltaL = 0.0f;
		if ( deltaL > 0 ) { // growth
			deltaL = Math.max( 0, deltaL - 0.05f ); // growing up 5% is free
			power = 4.0f;
		} else { // shrinkage
			power = 12.0f;
		}
		deltaL = Math.abs( deltaL );
		costDeltaL = deltaL * ( float ) Math.pow( 1 + deltaL, power );
		latestCostEvaluation = String.format( "c_l = %.4f * %.4f^%.1f = %.4f", deltaL, 1 + deltaL, power, costDeltaL );
		return costDeltaL;
	}

	private static float getIntensityMismatchCost(
			final float oldIntensity,
			final float newIntensity ) {
		final float deltaV = Math.max( 0.0f, newIntensity - oldIntensity ); // nur heller werden wird bestraft!
		final float power = 1.0f;
		final float freeUntil = 0.1f;
		float costDeltaV = 0.0f;
		if ( deltaV > freeUntil ) { // significant jump
			costDeltaV = deltaV * ( float ) Math.pow( 1.0 + ( deltaV - freeUntil ), power );
		}
//		latestCostEvaluation = String.format( "c_v = %.4f * %.4f^%.1f = %.4f", deltaV, 1 + deltaV, power, costDeltaV );
		latestCostEvaluation = String.format( "c_v = 0.0" );
		return 0.0f * costDeltaV;
	}

	private static float getUnevenDivisionCost(
			final float sizeFirstChild,
			final float sizeSecondChild ) {
		final float deltaS = Math.abs( sizeFirstChild - sizeSecondChild ) / Math.min( sizeFirstChild, sizeSecondChild );
		float power = 2.0f;
		float costDeltaL = 0.0f;
		if ( deltaS > 1.15 ) {
			power = 7.0f;
		}
		costDeltaL = ( float ) Math.pow( deltaS, power );

		latestCostEvaluation = String.format( "c_d = %.4f^%.1f = %.4f", deltaS, power, costDeltaL );
		return costDeltaL;
	}

	/**
	 * @param ctNode
	 * @param gapSepFkt
	 * @return
	 */
	public static float getIntensitySegmentationCost( final Component< ?, ? > ctNode, final float[] gapSepFkt ) {
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

		final float l = gapSepFkt[ a ];
		final float r = gapSepFkt[ b ];

		final float maxReduced = SimpleFunctionAnalysis.getMax( gapSepFkt, aReduced, bReduced ).b.floatValue();
		final float min = SimpleFunctionAnalysis.getMin( gapSepFkt, a, b ).b.floatValue();

		final float maxRimHeight = Math.max( l, r ) - min;
		final float reducedMaxHeight = maxReduced - min;
		float cost = -( maxRimHeight - reducedMaxHeight ) + MotherMachine.MIN_GAP_CONTRAST;

		// Special case: min-value is above average gap-sep-fkt value (happens often at the very top)
		final float avgFktValue = SimpleFunctionAnalysis.getSum( gapSepFkt ) / ( gapSepFkt.length - 1 );
//		final float distAboveAvg = Math.max( 0.0, min - avgFktValue );
		final float medianSegmentValue = SimpleFunctionAnalysis.getMedian( gapSepFkt, a, b );
		final float distAboveAvg = Math.max( 0.0f, medianSegmentValue - avgFktValue );
		cost += ( distAboveAvg + 0.05 ) * Math.pow( 1 + ( distAboveAvg + 0.05 ), 8.0 );

		// cell is too small
		if ( a > 0 && b + 1 < gapSepFkt.length && b - a < MotherMachine.MIN_CELL_LENGTH ) { // if a==0 or b==gapSepFkt.len, only a part of the cell is seen!
			cost = 100;
		}
		return cost;
	}

	/**
	 * @param ctNode
	 * @param gapSepFkt
	 * @return
	 */
	public static float getParamaxflowSegmentationCost( final Component< ?, ? > ctNode, final float[] gapSepFkt ) {
		final Pair< Integer, Integer > segInterval = ComponentTreeUtils.getTreeNodeInterval( ctNode );
		final int a = segInterval.getA().intValue();
		final int b = segInterval.getB().intValue();

		final float plateauDerivativeThreshold = 0.0000f; //some epsilon
		int aReduced = SimpleFunctionAnalysis.getRighthandLocalMinOrPlateau( gapSepFkt, a, plateauDerivativeThreshold ).a.intValue();
		int bReduced = SimpleFunctionAnalysis.getLefthandLocalMinOrPlateau( gapSepFkt, b, plateauDerivativeThreshold ).a.intValue();
		if ( aReduced > bReduced ) {
			aReduced = bReduced = SimpleFunctionAnalysis.getMin( gapSepFkt, a, b ).a.intValue();
		}

		final float avgBottomVal = SimpleFunctionAnalysis.getAvg( gapSepFkt, a, b );
		float maxReduced = SimpleFunctionAnalysis.getMax( gapSepFkt, aReduced, bReduced ).b.floatValue();
		maxReduced = Math.max( maxReduced, avgBottomVal ); // tricky but I like it!

		final float segmentLengthInPercentGL = ( b - a ) / ( ( float ) gapSepFkt.length );

		// Special case: min-value is above average gap-sep-fkt value (happens often at the very top)
		final float avgFktValue = SimpleFunctionAnalysis.getAvg( gapSepFkt );
		final float distAboveAvg = Math.max( 0.0f, avgBottomVal - avgFktValue );
		final float penaltyHeight = distAboveAvg * ( float ) Math.pow( 1.0 + distAboveAvg, 5 );
		final float incentiveHeight = ( 1.0f - maxReduced );

		float cost = ( penaltyHeight * segmentLengthInPercentGL ) - ( incentiveHeight * segmentLengthInPercentGL );

		// cell is too small
		if ( a > 0 && b + 1 < gapSepFkt.length && b - a < MotherMachine.MIN_CELL_LENGTH ) { // if a==0 or b==gapSepFkt.len, only a part of the cell is seen!
			cost = 100;
		}
		return cost;
	}

	/**
	 * Computes the compatibility-mapping-costs between the two given
	 * hypothesis.
	 *
	 * @param from
	 *            the segmentation hypothesis from which the mapping originates.
	 * @param to
	 *            the segmentation hypothesis towards which the
	 *            mapping-assignment leads.
	 * @param glLength
	 *            the length of the GL (number of pixels along the green
	 *            center-line).
	 * @return the cost we want to set for the given combination of segmentation
	 *         hypothesis.
	 */
	public static float compatibilityCostOfMapping(
			final Component< FloatType, ? > from,
			final Component< FloatType, ? > to,
			final int glLength ) {

		final long sizeFrom = from.size();
		final long sizeTo = to.size();

		final float valueFrom = from.value().get();
		final float valueTo = to.value().get();

		final Pair< Integer, Integer > intervalFrom =
				ComponentTreeUtils.getTreeNodeInterval( from );
		final Pair< Integer, Integer > intervalTo =
				ComponentTreeUtils.getTreeNodeInterval( to );

		final float oldPosU = intervalFrom.getA().intValue();
		final float newPosU = intervalTo.getA().intValue();
		final float oldPosL = intervalFrom.getB().intValue();
		final float newPosL = intervalTo.getB().intValue();

		// Finally the costs are computed...
		final float costDeltaHU = CostFactory.getMigrationCost( oldPosU, newPosU, glLength );
		final float costDeltaHL = CostFactory.getMigrationCost( oldPosL, newPosL, glLength );
		final float costDeltaH = Math.max( costDeltaHL, costDeltaHU );
		final float costDeltaL = CostFactory.getGrowthCost( sizeFrom, sizeTo, glLength );
		final float costDeltaV = CostFactory.getIntensityMismatchCost( valueFrom, valueTo );

		float cost = costDeltaL + costDeltaV + costDeltaH;

		// Border case bullshit
		// if the target cell touches the upper or lower border (then don't count uneven and shrinking)
		// (It is not super obvious why this should be true for bottom ones... some data has shitty
		// contrast at bottom, hence we trick this condition in here not to loose the mother -- which would
		// mean to loose all future tracks!!!)
		if ( intervalTo.getA().intValue() == 0 || intervalTo.getB().intValue() + 1 >= glLength ) {
			cost = costDeltaH + costDeltaV;
		}

//		System.out.println( String.format( ">>> %f + %f + %f = %f", costDeltaL, costDeltaV, costDeltaH, cost ) );
		return cost;
	}

	/**
	 * Computes the compatibility-mapping-costs between the two given
	 * hypothesis.
	 *
	 * @param from
	 *            the segmentation hypothesis from which the mapping originates.
	 * @param to
	 *            the upper (left) segmentation hypothesis towards which the
	 *            mapping-assignment leads.
	 * @param lowerNeighbor
	 *            the lower (right) segmentation hypothesis towards which the
	 *            mapping-assignment leads.
	 * @return the cost we want to set for the given combination of segmentation
	 *         hypothesis.
	 */
	public static float compatibilityCostOfDivision(
			final Component< FloatType, ? > from,
			final Component< FloatType, ? > toUpper,
			final Component< FloatType, ? > toLower,
			final int glLength ) {
		final long sizeFrom = from.size();
		final long sizeToU = toUpper.size();
		final long sizeToL = toLower.size();
		final long sizeTo = sizeToU + sizeToL;

		final float valueFrom = from.value().get();
		final float valueTo = 0.5f * ( toUpper.value().get() + toLower.value().get() );

		final Pair< Integer, Integer > intervalFrom = ComponentTreeUtils.getTreeNodeInterval( from );
		final Pair< Integer, Integer > intervalToU =
				ComponentTreeUtils.getTreeNodeInterval( toUpper );
		final Pair< Integer, Integer > intervalToL =
				ComponentTreeUtils.getTreeNodeInterval( toLower );

		final float oldPosU = intervalFrom.getA().intValue();
		final float newPosU = intervalToU.getA().intValue();
		final float oldPosL = intervalFrom.getB().intValue();
		final float newPosL = intervalToL.getB().intValue();

		// Finally the costs are computed...
		final float costDeltaHU = CostFactory.getMigrationCost( oldPosU, newPosU, glLength );
		final float costDeltaHL = CostFactory.getMigrationCost( oldPosL, newPosL, glLength );
		final float costDeltaH = Math.max( costDeltaHL, costDeltaHU );
		final float costDeltaL = CostFactory.getGrowthCost( sizeFrom, sizeTo, glLength );
		final float costDeltaL_ifAtTop =
				CostFactory.getGrowthCost( sizeFrom, sizeToL * 2, glLength );
		final float costDeltaV = CostFactory.getIntensityMismatchCost( valueFrom, valueTo );
		final float costDeltaS = CostFactory.getUnevenDivisionCost( sizeToU, sizeToL );

		float cost = costDeltaL + costDeltaV + costDeltaH + costDeltaS;

		// Border case bullshit
		// if the upper cell touches the upper border (then don't count shrinking and be nicer to uneven)
		if ( intervalToU.getA().intValue() == 0 || intervalToL.getB().intValue() + 1 >= glLength ) {
			// In case the upper cell is still at least like 1/2 in
			if ( ( 1.0 * sizeToU ) / ( 1.0 * sizeToL ) > 0.5 ) {
				// don't count uneven div cost (but pay a bit to avoid exit+division instead of two mappings)
				cost = costDeltaL_ifAtTop + costDeltaH + costDeltaV + 0.1f;
			} else {
				// otherwise do just leave out shrinking cost alone - yeah!
				cost = costDeltaL_ifAtTop + costDeltaH + costDeltaV + costDeltaS + 0.03f;
			}
		}

//		System.out.println( String.format( ">>> %f + %f + %f + %f = %f", costDeltaL, costDeltaV, costDeltaH, costDeltaS, cost ) );
		return cost;
	}
}
