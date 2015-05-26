/**
 *
 */
package com.jug.lp;

import gurobi.GRBConstr;
import gurobi.GRBException;

import java.util.LinkedList;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;

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
@SuppressWarnings( "restriction" )
public class Hypothesis< T extends Component< FloatType, ? > > {

	public class HypLoc {

		protected int t = -1;
		protected ValuePair< Integer, Integer > limits;

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

	/**
	 * Used to store track-branch pruning sources. This is a way to easily
	 * exclude branches from data export etc.
	 */
	private boolean isPruneRoot = false;
	private boolean isPruned = false;

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

	public ValuePair< Integer, Integer > getLocation() {
		return location.limits;
	}

	public HypLoc getHypLoc() {
		return location;
	}

	public int getTime() {
		return location.t;
	}

	/**
	 *
	 */
	public void setPruneRoot( final boolean value, final GrowthLineTrackingILP ilp ) {

		this.isPruneRoot = value;

		final LinkedList< Hypothesis< Component< FloatType, ? > > > queue =
				new LinkedList< Hypothesis< Component< FloatType, ? > > >();
		// TODO there will be no time, but this is of course not nice...
		queue.add( ( Hypothesis< Component< FloatType, ? > > ) this );
		while ( !queue.isEmpty() ) {
			final Hypothesis< Component< FloatType, ? > > node = queue.removeFirst();
			node.setPruned( value );

			AbstractAssignment< Hypothesis< Component< FloatType, ? >>> assmnt;
			try {
				assmnt = ilp.getOptimalRightAssignment( node );

				if ( assmnt != null ) {
					assmnt.setPruned( value );

					switch ( assmnt.getType() ) {
					case GrowthLineTrackingILP.ASSIGNMENT_DIVISION:
						if ( !( ( DivisionAssignment ) assmnt ).getUpperDesinationHypothesis().isPruneRoot() ) {
							queue.add( ( ( DivisionAssignment ) assmnt ).getUpperDesinationHypothesis() );
						}
						if ( !( ( DivisionAssignment ) assmnt ).getLowerDesinationHypothesis().isPruneRoot() ) {
							queue.add( ( ( DivisionAssignment ) assmnt ).getLowerDesinationHypothesis() );
						}
						break;
					case GrowthLineTrackingILP.ASSIGNMENT_MAPPING:
						if ( !( ( MappingAssignment ) assmnt ).getDestinationHypothesis().isPruneRoot() ) {
							queue.add( ( ( MappingAssignment ) assmnt ).getDestinationHypothesis() );
						}
						break;
					}
				}
			} catch ( final GRBException e ) {
//				e.printStackTrace();
			}
		}
	}

	/**
	 * @return
	 */
	public boolean isPruneRoot() {
		return isPruneRoot;
	}

	/**
	 * @param value
	 */
	public void setPruned( final boolean value ) {
		this.isPruned = value;
	}

	/**
	 * @return
	 */
	public boolean isPruned() {
		return this.isPruned;
	}
}
