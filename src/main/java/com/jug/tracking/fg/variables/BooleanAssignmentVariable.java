/**
 *
 */
package com.jug.tracking.fg.variables;

import com.indago.fg.variable.BooleanVariable;
import com.jug.tracking.assignments.TrackingAssignment;

/**
 * @author jug
 */
public class BooleanAssignmentVariable< A extends TrackingAssignment > extends BooleanVariable {

	private final A asmnt;

	/**
	 * @param value
	 */
	public BooleanAssignmentVariable( final A a ) {
		this.asmnt = a;
	}

	public A getAssignment() {
		return this.asmnt;
	}
}
