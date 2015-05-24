/**
 *
 */
package com.jug.lp;

import gurobi.GRBConstr;
import net.imglib2.Pair;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.FloatType;

import com.jug.util.ComponentTreeUtils;
import com.jug.util.filteredcomponents.FilteredComponent;

/**
 * This class is used to wrap away whatever object that represents one of the
 * segmentation hypothesis. See {@link AbstractAssignment} for a place where
 * this is
 * used.
 *
 * @author jug
 */
public class Hypothesis< T extends Component< FloatType, ? > > {

	public class HypLoc {

		protected int t = -1;
		protected Pair< Integer, Integer > limits;

		public HypLoc( final int t, final T segment ) {
			this.t = t;
			if ( segment instanceof FilteredComponent ) {
				this.limits =
						ComponentTreeUtils.getExtendedTreeNodeInterval( ( FilteredComponent< ? > ) segment );
			} else {
				this.limits = ComponentTreeUtils.getTreeNodeInterval( segment );
			}
		}
	}

	private final T wrappedHypothesis;
	private final float costs;
	private final HypLoc location;

	/**
	 * Used to store a 'segment in solution constraint' after it was added to
	 * the ILP. If such a constraint does not exist for this hypothesis, this
	 * value is null.
	 */
	private GRBConstr segmentSpecificConstraint = null;

	public Hypothesis( final int t, final T elementToWrap, final float costs ) {
		// setSegmentHypothesis( elementToWrap );
		this.wrappedHypothesis = elementToWrap;
		this.costs = costs;
		location = new HypLoc( t, elementToWrap );
	}

	public int getId() {
		return location.limits.getA() * 1000 + location.limits.getB();
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
	public float getCosts() {
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

	public Pair< Integer, Integer > getLocation() {
		return location.limits;
	}

	public HypLoc getHypLoc() {
		return location;
	}

	public int getTime() {
		return location.t;
	}
}