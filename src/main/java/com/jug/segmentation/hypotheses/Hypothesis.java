/**
 *
 */
package com.jug.segmentation.hypotheses;


/**
 * This class is used to wrap away whatever object that represents one of the
 * segmentation hypothesis. See {@link AbstractAssignment} for a place where
 * this is
 * used.
 *
 * @author jug
 */
public class Hypothesis< T > {

	private final T wrappedSegment;
	private final float costs;

	public Hypothesis( final T segment, final float costs ) {
		this.wrappedSegment = segment;
		this.costs = costs;
	}

	/**
	 * @return the wrapped segmentHypothesis
	 */
	public T getSegment() {
		return wrappedSegment;
	}

	/**
	 * @return the costs
	 */
	public float getCosts() {
		return costs;
	}
}
