/**
 *
 */
package com.jug.lp;


/**
 * This class is used to wrap away whatever object that represents one of the
 * segmentation hypothesis. See {@link AbstractAssignment} for a place where this is
 * used.
 *
 * @author jug
 */
public class Hypothesis< T > {

	private final T wrappedHypothesis;
	private final double costs;

	public Hypothesis( final T elementToWrap, final double costs ) {
		// setSegmentHypothesis( elementToWrap );
		this.wrappedHypothesis = elementToWrap;
		this.costs = costs;
	}

	/**
	 * @return the wrapped segmentHypothesis
	 */
	public T getWrappedHypothesis() {
		return wrappedHypothesis;
	}

	// /**
	// * @param elementToWrap
	// * the segmentHypothesis to wrap inside this
	// * {@link Hypothesis}
	// */
	// public void setSegmentHypothesis( final T elementToWrap ) {
	// this.wrappedHypothesis = elementToWrap;
	// }

	/**
	 * @return the costs
	 */
	public double getCosts() {
		return costs;
	}

}
