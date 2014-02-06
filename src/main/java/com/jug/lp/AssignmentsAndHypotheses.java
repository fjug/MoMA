/**
 *
 */
package com.jug.lp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author jug
 */
public class AssignmentsAndHypotheses< A extends AbstractAssignment< H >, H extends Hypothesis< ? > > {

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	/**
	 * All assignments. Outer list corresponds with time-points t, inner one is
	 * just a container for the assignments (could very well be a set instead of
	 * a list).
	 */
	private final List< List< A > > a_t;

	/**
	 * All segmentation hypotheses. Outer list corresponds with time-points t,
	 * inner one is just a container for the assignments (could very well be a
	 * set instead of a list).
	 */
	private final List< List< H > > h_t;

	/**
	 * A Map from any <code>Object</code> to a segmentation hypothesis.
	 * Hypotheses are used to encapsulate any kind of entity (segmentation
	 * hypothesis) out there in the world. This might for example be a
	 * <code>Component</code>.
	 */
	private final Map< Object, H > hmap;

	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	public AssignmentsAndHypotheses() {
		a_t = new ArrayList< List< A > >();
		h_t = new ArrayList< List< H > >();
		hmap = new HashMap< Object, H >();
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * Adds (appends) a new time-step.
	 * This shrinks down to appending an inner <code>List</code> to
	 * <code>a_t</code> and <code>h_t</code>.
	 */
	public void addTimeStep () {
		a_t.add( new ArrayList< A >() );
		h_t.add( new ArrayList< H >() );
	}

	/**
	 * Adds an assignment to <code>a_t</code>. If <code>a_t</code> does not
	 * contain <code>t</code> time-steps this function will add the missing
	 * amount.
	 *
	 * @param t
	 *            a number denoting the time at which the given assignment
	 *            should be added.
	 * @param a
	 *            the assignment to be added.
	 * @return true, if the assignment could be added to <code>a_t</code>.
	 */
	public boolean addAssignment( final int t, final A a ) {
		while ( t >= a_t.size() ) {
			addTimeStep();
		}
		return a_t.get( t ).add( a );
	}

	/**
	 * Returns all time-points in a <code>List</code>, containing all stored
	 * assignments in a <code>List</code>.
	 *
	 * @return <code>a_t</code>
	 */
	public List< List< A > > getAllAssignments() {
		return a_t;
	}

	/**
	 * Returns a <code>List</code> containing all assignments stored at
	 * time-point t.
	 * By definition those are all the assignments between t and t+1.
	 *
	 * @param t
	 *            a number denoting the time at which the given assignment
	 *            should be returned.
	 * @return <code>a_t.get(t);</code>
	 */
	public List< A > getAssignmentsAt( final int t ) {
		assert ( t >= 0 );
		assert ( t < a_t.size() );
		return a_t.get( t );
	}


	/**
	 * Adds a hypothesis to <code>h_t</code>. If <code>h_t</code> does not
	 * contain <code>t</code> time-steps this function will add the missing
	 * amount.
	 *
	 * @param t
	 *            a number denoting the time at which the given assignment
	 *            should be added.
	 * @param h
	 *            the segmentation hypothesis to be added.
	 * @return true, if the hypothesis could be added to <code>h_t</code>.
	 */
	public boolean addHypothesis( final int t, final H h ) {
		while ( t >= h_t.size() ) {
			addTimeStep();
		}
		if ( h_t.get( t ).add( h ) ) {
			hmap.put( h.getWrappedHypothesis(), h );
			return true;
		}
		return false;
	}

	/**
	 * Returns all time-points in a <code>List</code>, containing all stored
	 * segmentation hypothesis in an inner <code>List</code>.
	 *
	 * @return <code>h_t</code>
	 */
	public List< List< H > > getAllHypotheses() {
		return h_t;
	}

	/**
	 * Returns a <code>List</code> containing all hypothesis stored at
	 * time-point t.
	 *
	 * @param t
	 *            a number denoting the time at which the given assignment
	 *            should be returned.
	 * @return <code>h_t.get(t);</code>
	 */
	public List< H > getHypothesesAt( final int t ) {
		assert ( t < h_t.size() );
		if ( t >= 0 ) {
			return h_t.get( t );
		} else {
			return null;
		}

	}

	/**
	 * Finds an <code>Hypothesis</code> that wraps the given <code>Object</code>
	 * .
	 *
	 * @param something
	 *            any <code>Object</code> you expect to be wrapped inside a
	 *            hypothesis.
	 * @return the <code>Hypothesis</code> wrapping the given
	 *         <code>Object</code>, or <code>null</code> in case this object was
	 *         not wrapped by any of the stored hypotheses.
	 */
	public Hypothesis< ? > findHypothesisContaining( final Object something ) {
		return hmap.get( something );
	}

	/**
	 * @return the number of entries in the outer lists of <code>h_t</code> and
	 *         <code>a_t</code>.
	 */
	public int getNumberOfTimeSteps() {
		return h_t.size();
	}
}
