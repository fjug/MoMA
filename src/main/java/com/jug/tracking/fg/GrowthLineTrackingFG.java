/**
 *
 */
package com.jug.tracking.fg;

import java.util.ArrayList;
import java.util.HashMap;

import com.indago.fg.factor.Factor;
import com.indago.fg.function.Function;
import com.jug.GrowthLine;
import com.jug.tracking.assignments.TrackingAssignment;
import com.jug.tracking.fg.variables.BooleanAssignmentVariable;

/**
 * @author jug
 */
public class GrowthLineTrackingFG {

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final GrowthLine gl;

	int factorId = 0;
	int functionId = 0;

	final ArrayList< Function< ?, ? > > functions = new ArrayList<Function< ?, ? >>();
	final ArrayList< Factor< ?, ?, ? > > factors = new ArrayList<Factor< ?, ?, ? >>();
	final ArrayList< BooleanAssignmentVariable< ? > > variables =
			new ArrayList< BooleanAssignmentVariable< ? > >();

	final HashMap< ? extends TrackingAssignment, BooleanAssignmentVariable< ? > > assignmentVariableDict =
			new HashMap< TrackingAssignment, BooleanAssignmentVariable< ? >>();

	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	public GrowthLineTrackingFG( final GrowthLine gl ) {
		this.gl = gl;
		buildFG();
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	public void buildFG() {

	}
}
