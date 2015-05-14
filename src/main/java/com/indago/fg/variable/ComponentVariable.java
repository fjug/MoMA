/**
 *
 */
package com.indago.fg.variable;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.FloatType;

/**
 * @author jug
 */
public class ComponentVariable< C extends Component< FloatType, C > >
		extends
		BooleanVariable {

	private C segment;
	private float annotatedCost;

	public ComponentVariable( final C segment ) {
		this.segment = segment;
	}

	/**
	 * @return the segment associated to this variable
	 */
	public C getSegment() {
		return segment;
	}

	/**
	 * @param segment
	 *            the segment to associate with this variable
	 */
	public void setSegment( final C segment ) {
		this.segment = segment;
	}

	/**
	 * @return the annotatedCost This is a convenience annotation and might be
	 *         inconsistent with the cost set in the unary factor dangling from
	 *         this variable. (Which is used during optimization!)
	 */
	public float getAnnotatedCost() {
		return annotatedCost;
	}

	/**
	 * @param annotatedCost
	 *            the annotatedCost to set - this is only to make cost retrieval
	 *            easier! In order to change the cost in the FG you must edit
	 *            the unary factor dangling from this variable.
	 */
	public void setAnnotatedCost( final float annotatedCost ) {
		this.annotatedCost = annotatedCost;
	}

}
