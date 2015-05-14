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

	private C sourceSegment;

	public AssignmentVariable( final C sourceSegment ) {
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
}
