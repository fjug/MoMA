/**
 *
 */
package com.jug.lp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * The main purpose of this class is to manage and update the assignment
 * neighborhoods $A_{>>b_i^t}$ and $A_{b_i^t>>}$.
 *
 * @author jug
 */
public class HypothesisNeighborhoods< H extends Hypothesis< ? >, A extends AbstractAssignment< H > > {

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	/**
	 * This structure corresponds to $A_{b_i->}$ for some time-point.
	 */
	private final HashMap< H, Set< A > > rightNeighborhoods;

	/**
	 * This structure corresponds to $A_{->b_i}$ for some time-point.
	 */
	private final HashMap< H, Set< A > > leftNeighborhoods;

	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	public HypothesisNeighborhoods() {
		rightNeighborhoods = new HashMap< H, Set< A > >();
		leftNeighborhoods = new HashMap< H, Set< A > >();
	}

	// -------------------------------------------------------------------------------------
	// getters and setters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the rightNeighborhoods
	 */
	public HashMap< H, Set< A > > getRightNeighborhoods() {
		return rightNeighborhoods;
	}

	// /**
	// * @param rightNeighborhoods
	// * the rightNeighborhoods to set
	// */
	// public void setRightNeighborhoods( final HashMap< H, Set< A > >
	// rightNeighborhoods ) {
	// this.rightNeighborhoods = rightNeighborhoods;
	// }

	/**
	 * @return the leftNeighborhoods
	 */
	public HashMap< H, Set< A > > getLeftNeighborhoods() {
		return leftNeighborhoods;
	}

	// /**
	// * @param leftNeighborhoods
	// * the leftNeighborhoods to set
	// */
	// public void setLeftNeighborhoods( final HashMap< H, Set< A > >
	// leftNeighborhoods ) {
	// this.leftNeighborhoods = leftNeighborhoods;
	// }

	/**
	 * Gets the leftNeighborhood of a hypothesis <code>h</code>.
	 *
	 * @param h
	 *            a hypothesis of type <code>H</code>.
	 * @return a set of assignments of type <code>A</code>, or <code>null</code>
	 *         if such a neighborhood does not exist here.
	 */
	public Set< A > getLeftNeighborhood( final H h ) {
		return leftNeighborhoods.get( h );
	}

	/**
	 * Gets the rightNeighborhood of a hypothesis <code>h</code>.
	 *
	 * @param h
	 *            a hypothesis of type <code>H</code>.
	 * @return a set of assignments of type <code>A</code>, or <code>null</code>
	 *         if such a neighborhood does not exist here.
	 */
	public Set< A > getRightNeighborhood( final H h ) {
		return rightNeighborhoods.get( h );
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * Returns whether or not a given segmentation hypothesis has a
	 * left-neighborhood stored here.
	 *
	 * @param h
	 *            a hypothesis of type <code>H</code>.
	 * @return true, if such a neighborhood exists (might be empty though), or
	 *         false if it does not.
	 */
	public boolean hasLeftNeighborhoods( final H h ) {
		return getLeftNeighborhood( h ) != null;
	}

	/**
	 * Returns whether or not a given segmentation hypothesis has a
	 * right-neighborhood stored here.
	 *
	 * @param h
	 *            a hypothesis of type <code>H</code>.
	 * @return true, if such a neighborhood exists (might be empty though), or
	 *         false if it does not.
	 */
	public boolean hasRightNeighborhoods( final H h ) {
		return getRightNeighborhood( h ) != null;
	}

	/**
	 * Returns whether or not a given segmentation hypothesis has either a right
	 * or a left-neighborhood.
	 *
	 * @param h
	 *            a hypothesis of type <code>H</code>.
	 * @return true, if such a neighborhood exists (might be empty though), or
	 *         false if it does not.
	 */
	public boolean hasNeighborhoods( final H h ) {
		return hasLeftNeighborhoods( h ) && hasRightNeighborhoods( h );
	}

	/**
	 * Adds an assignment go the left-neighborhood of a segmentation hypothesis.
	 *
	 * @param h
	 *            a hypothesis of type <code>H</code>.
	 * @param a
	 *            an assignment of type <code>A</code>.
	 * @return true, if the assignment could be stored.
	 */
	public boolean addToLeftNeighborhood ( final H h, final A a ) {
		if ( ! hasLeftNeighborhoods( h ) ) {
			leftNeighborhoods.put( h, new HashSet< A >() );
		}
		return getLeftNeighborhood( h ).add( a );
	}

	/**
	 * Adds an assignment go the right-neighborhood of a segmentation
	 * hypothesis.
	 *
	 * @param h
	 *            a hypothesis of type <code>H</code>.
	 * @param a
	 *            an assignment of type <code>A</code>.
	 * @return true, if the assignment could be stored.
	 */
	public boolean addToRightNeighborhood( final H h, final A a ) {
		if ( !hasRightNeighborhoods( h ) ) {
			rightNeighborhoods.put( h, new HashSet< A >() );
		}
		return getRightNeighborhood( h ).add( a );
	}

}
