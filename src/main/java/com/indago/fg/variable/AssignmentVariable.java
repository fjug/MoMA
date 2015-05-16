/**
 *
 */
package com.indago.fg.variable;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.FloatType;

/**
 * @author jug
 */
public class AssignmentVariable< C extends Component< FloatType, C > >
		extends
		BooleanVariable {

	public static int NOT_SET = -1;
	public static int ASSIGNMENT_EXIT = 0;
	public static int ASSIGNMENT_MAPPING = 1;
	public static int ASSIGNMENT_DIVISION = 2;

	private int type = -1;

	private C sourceSegment;

	private float annotatedCost;

	public AssignmentVariable( final C sourceSegment, final int type ) {
		this.type = type;
		this.sourceSegment = sourceSegment;
	}

	/**
	 * @return the segment associated to this variable
	 */
	public C getSourceSegment() {
		return sourceSegment;
	}

	/**
	 * @param segment
	 *            the segment to associate with this variable
	 */
	public void setSourceSegment( final C segment ) {
		this.sourceSegment = segment;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

//	/**
//	 * @param type the type to set
//	 */
//	public void setType( int type ) {
//		this.type = type;
//	}

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
