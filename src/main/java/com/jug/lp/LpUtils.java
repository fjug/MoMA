/**
 *
 */
package com.jug.lp;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.FloatType;

import com.jug.util.ComponentTreeUtils;

/**
 * @author jug
 */
public class LpUtils {

	/**
	 * Builds and returns the set Hup (given as a List). Hup is defines as the
	 * set up all Hypothesis in hyps that strictly above the Hypothesis hyp (in
	 * image space).
	 * 
	 * @param hyp
	 *            the reference hypothesis.
	 * @param hyps
	 *            the set of all hypothesis of interest.
	 * @return a List of all Hypothesis from hyps that lie above the
	 *         segmentation hypothesis hyp.
	 */
	public static List< Hypothesis< Component< FloatType, ? >>> getHup( final Hypothesis< Component< FloatType, ? >> hyp, final List< Hypothesis< Component< FloatType, ? >>> hyps ) {
		final List< Hypothesis< Component< FloatType, ? >>> Hup = new ArrayList< Hypothesis< Component< FloatType, ? >>>();
		for ( final Hypothesis< Component< FloatType, ? >> candidate : hyps ) {
			if ( ComponentTreeUtils.isAbove( candidate.getWrappedHypothesis(), hyp.getWrappedHypothesis() ) ) {
				Hup.add( candidate );
			}
		}
		return Hup;
	}
}
