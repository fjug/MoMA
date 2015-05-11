/**
 *
 */
package com.indago.fg.variable;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.FloatType;

/**
 * @author jug
 */
public class ComponentVariable< C extends Component< FloatType, C > > extends BooleanVariable {

	private C segment;

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
}
