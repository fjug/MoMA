/**
 *
 */
package com.jug.lp;

import gurobi.GRBConstr;

/**
 * This class is used to wrap away whatever object that represents one of the
 * segmentation hypothesis. See {@link AbstractAssignment} for a place where
 * this is
 * used.
 * 
 * @author jug
 */
public class Hypothesis< T > {

	private final T wrappedHypothesis;
	private final double costs;

	/**
	 * Used to store a 'segment in solution constraint' after it was added to
	 * the ILP. If such a constraint does not exist for this hypothesis, this
	 * value is null.
	 */
	private GRBConstr segmentSpecificConstraint = null;

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

	/**
	 * @return the stored gurobi constraint that either forces this hypothesis
	 *         to be part of any solution to the ILP or forces this hypothesis
	 *         to be NOT included. Note: this function returns 'null' if such a
	 *         constraint was never created.
	 */
	public GRBConstr getSegmentSpecificConstraint() {
		return this.segmentSpecificConstraint;
	}

	/**
	 * Used to store a 'segment in solution constraint' or a 'segment not in
	 * solution constraint' after it was added to the ILP.
	 * 
	 * @param constr
	 *            the installed constraint.
	 */
	public void setSegmentSpecificConstraint( final GRBConstr constr ) {
		this.segmentSpecificConstraint = constr;
	}

}
