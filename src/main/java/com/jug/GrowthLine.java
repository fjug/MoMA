/**
 *
 */
package com.jug;

import java.util.ArrayList;
import java.util.List;

import com.indago.fg.FactorGraph;
import com.indago.fg.factor.Factor;
import com.indago.fg.function.Function;
import com.indago.fg.variable.ComponentVariable;
import com.jug.gui.progress.DialogProgress;

/**
 * @author jug
 */
public class GrowthLine {

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final List< GrowthLineFrame > frames;
	private FactorGraph fg;

	// Hypothesis< Component< FloatType, ? > >,
	// AbstractAssignment< Hypothesis< Component< FloatType, ? > > > > ilp;

	// -------------------------------------------------------------------------------------
	// setters and getters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the frames
	 */
	public List< GrowthLineFrame > getFrames() {
		return frames;
	}

	/**
	 * @return the FactorGraph built for this GL (or null if no FG was built
	 *         yet).
	 */
	public FactorGraph getFg() {
		return fg;
	}

	// -------------------------------------------------------------------------------------
	// constructors
	// -------------------------------------------------------------------------------------
	public GrowthLine() {
		this.frames = new ArrayList< GrowthLineFrame >();
	}

	public GrowthLine( final List< GrowthLineFrame > frames ) {
		this.frames = frames;
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * @return the number of frames (time-steps) in this <code>GrowthLine</code>
	 */
	public int size() {
		return frames.size();
	}

	/**
	 * @param frame
	 *            the GrowthLineFrame to be appended as last frame
	 * @return true, if add was successful.
	 */
	public boolean add( final GrowthLineFrame frame ) {
		frame.setParent( this );
		return frames.add( frame );
	}

	/**
	 * @param frame
	 *            the GrowthLineFrame to be prepended as first frame
	 * @return true, if add was successful.
	 */
	public void prepand( final GrowthLineFrame frame ) {
		frame.setParent( this );
		frames.add( 0, frame );
	}

	/**
	 * @param f
	 * @return
	 */
	public GrowthLineFrame get( final int i ) {
		return this.getFrames().get( i );
	}

	/**
	 * Generates the tracking FactorGraph for this GL.
	 *
	 * @param dialogProgress
	 * @return
	 */
	public FactorGraph generateFG( final DialogProgress dialogProgress ) {
		final ArrayList< Function< ?, ? > > functions = new ArrayList<>();
		final ArrayList< Factor< ?, ?, ? > > factors = new ArrayList<>();
		final ArrayList< ComponentVariable< ? > > variables = new ArrayList<>();

		final int factorId = 0;
		final int functionId = 0;

		// Collect all segment hypotheses and create associated variables
		// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

		// Build all functions needed for the FG
		// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

		// Connect variables by factors (using previously prepared functions)
		// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

		fg = ( new FactorGraph( variables, factors, functions ) );
		return fg;
	}
}
